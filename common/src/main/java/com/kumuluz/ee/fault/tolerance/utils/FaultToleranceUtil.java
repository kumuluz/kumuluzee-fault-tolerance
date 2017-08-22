/*
 *  Copyright (c) 2014-2017 Kumuluz and/or its affiliates
 *  and other contributors as indicated by the @author tags and
 *  the contributor list.
 *
 *  Licensed under the MIT License (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://opensource.org/licenses/MIT
 *
 *  The software is provided "AS IS", WITHOUT WARRANTY OF ANY KIND, express or
 *  implied, including but not limited to the warranties of merchantability,
 *  fitness for a particular purpose and noninfringement. in no event shall the
 *  authors or copyright holders be liable for any claim, damages or other
 *  liability, whether in an action of contract, tort or otherwise, arising from,
 *  out of or in connection with the software or the use or other dealings in the
 *  software. See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.kumuluz.ee.fault.tolerance.utils;

import com.kumuluz.ee.fault.tolerance.annotations.*;
import com.kumuluz.ee.fault.tolerance.interfaces.FallbackHandler;
import com.kumuluz.ee.fault.tolerance.interfaces.FaultToleranceExecutor;
import com.kumuluz.ee.fault.tolerance.models.FaultToleranceConfigurationType;
import com.kumuluz.ee.fault.tolerance.models.ConfigurationProperty;
import com.kumuluz.ee.fault.tolerance.models.ExecutionMetadata;
import com.kumuluz.ee.configuration.ConfigurationListener;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import org.jboss.weld.context.RequestContext;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;
import java.util.logging.Logger;

/**
 * Util for executing method in circuit breaker and setting up configuration
 *
 * @author Luka Šarc
 */
@ApplicationScoped
public class FaultToleranceUtil {

    private static final Logger log = Logger.getLogger(FaultToleranceUtil.class.getName());

    public static final String SERVICE_NAME = "fault-tolerance";
    private static final int CONFIG_WATCH_QUEUE_UPDATE_LIMIT = 20;

    private Boolean watchEnabled;
    private List<String> watchProperties;

    private Map<String, ExecutionMetadata> metadatasMap;
    private Queue<ConfigurationProperty> updatePropertiesQueue;
    private Map<String, ConfigurationListener> configListenersMap;

    @Inject
    private FaultToleranceExecutor circuitBreakerExecutor;

    @PostConstruct
    public void init() {

        metadatasMap = new HashMap<>();
        updatePropertiesQueue = new LinkedList<>();
        configListenersMap = new HashMap<>();

        ConfigurationUtil configUtil = ConfigurationUtil.getInstance();

        Optional<Boolean> watchEnabledOptional = configUtil.getBoolean("kumuluzee." + SERVICE_NAME + ".watch-enabled");
        if (watchEnabledOptional.isPresent())
            watchEnabled = watchEnabledOptional.get();
        else
            watchEnabled = null;

        if (watchEnabled == null || watchEnabled.booleanValue()) {
            Optional<String> watchPropertiesOptional = configUtil.get("kumuluzee." + SERVICE_NAME + ".watch-properties");

            if (watchPropertiesOptional.isPresent()) {
                watchProperties = Arrays.asList(watchPropertiesOptional.get().split(","));
            }
        }
    }

    @PreDestroy
    public void destroy() {

        ConfigurationUtil configUtil = ConfigurationUtil.getInstance();

        configListenersMap.values().forEach(configUtil::unsubscribe);
    }

    public Object execute(InvocationContext invocationContext, RequestContext requestContext) throws Exception {

        ExecutionMetadata config = toExecutionMetadata(invocationContext);

        updateConfigurations();

        return circuitBreakerExecutor.execute(invocationContext, requestContext, config);
    }

    public void watch(ConfigurationProperty property) {

        if (watchEnabled == null || !watchEnabled.booleanValue() ||
                configListenersMap.containsKey(property.configurationPath()))
            return;

        if (watchProperties != null) {
            boolean propertyMatch = watchProperties.stream()
                    .anyMatch(p -> p.equals(property.getProperty()));

            if (!propertyMatch)
                return;
        }

        ConfigurationListener listener = (String updatedKey, String updatedValue) -> {

            if (property.configurationPath().equals(updatedKey)) {
                log.finest("Configuration property updated for '" + updatedKey + "' with value '" +
                        updatedValue + "'.");

                ConfigurationProperty updatedProperty = ConfigurationProperty.create(updatedKey);

                if (updatedProperty != null) {
                    boolean valueParsed = false;

                    if (FaultToleranceHelper.isInt(updatedValue)) {
                        updatedProperty.setValue(FaultToleranceHelper.parseInt(updatedValue));
                        valueParsed = true;
                    } else if (FaultToleranceHelper.isBoolean(updatedValue)) {
                        updatedProperty.setValue(FaultToleranceHelper.parseBoolean(updatedValue));
                        valueParsed = true;
                    } else if (FaultToleranceHelper.isTime(updatedValue)) {
                        updatedProperty.setValue(FaultToleranceHelper.parseDuration(updatedValue));
                        valueParsed = true;
                    }

                    if (valueParsed) {
                        updatePropertiesQueue.add(updatedProperty);
                    } else {
                        log.warning("Parsing of configuration property value '" +
                                updatedValue + "' for key '" + updatedKey + "' failed.");
                    }
                } else {
                    log.warning("Parsing of configuration property key '" +
                            updatedKey + "' failed.");
                }
            }
        };

        configListenersMap.put(property.configurationPath(), listener);

        ConfigurationUtil.getInstance().subscribe(property.configurationPath(), listener);
    }

    public void removeWatch(ConfigurationProperty property) {

        String configPath = property.configurationPath();

        if (configListenersMap.containsKey(configPath)) {
            ConfigurationUtil.getInstance().unsubscribe(configListenersMap.get(configPath));
            configListenersMap.remove(configPath);
        }
    }

    public void updateConfigurations() {

        int cnt = 0;
        while(updatePropertiesQueue.peek() != null && cnt < CONFIG_WATCH_QUEUE_UPDATE_LIMIT) {
            ConfigurationProperty prop = updatePropertiesQueue.poll();
            circuitBreakerExecutor.setPropertyValue(prop);
            cnt++;
        }
    }

    private ExecutionMetadata toExecutionMetadata(InvocationContext ic) {

        Method targetMethod = ic.getMethod();
        Object targetObject = ic.getTarget();
        Class<?> targetClass = targetObject.getClass();

        if (targetClassIsProxied(targetClass))
            targetClass = targetClass.getSuperclass();

        String commandKey = getKey(targetMethod, targetClass);
        String groupKey = getGroupKey(targetMethod, targetClass);
        String key = groupKey + "." + commandKey;

        if (metadatasMap.containsKey(key))
            return metadatasMap.get(key);

        log.finest("Initializing execution metadata for key '" + key + "'.");

        Asynchronous asynchronous = null;
        Bulkhead bulkhead = null;
        Timeout timeout = null;
        Fallback fallback = null;
        CircuitBreaker circuitBreaker = null;

        if (targetMethod.isAnnotationPresent(Asynchronous.class))
            asynchronous = targetMethod.getAnnotation(Asynchronous.class);
        else if (targetClass.isAnnotationPresent(Asynchronous.class))
            asynchronous = targetClass.getAnnotation(Asynchronous.class);

        if (targetMethod.isAnnotationPresent(Bulkhead.class))
            bulkhead = targetMethod.getAnnotation(Bulkhead.class);
        else if (targetClass.isAnnotationPresent(Bulkhead.class))
            bulkhead = targetClass.getAnnotation(Bulkhead.class);

        if (targetMethod.isAnnotationPresent(Timeout.class))
            timeout = targetMethod.getAnnotation(Timeout.class);
        else if (targetClass.isAnnotationPresent(Timeout.class))
            timeout = targetClass.getAnnotation(Timeout.class);

        if (targetMethod.isAnnotationPresent(Fallback.class))
            fallback = targetMethod.getAnnotation(Fallback.class);
        else if (targetClass.isAnnotationPresent(Fallback.class))
            fallback = targetClass.getAnnotation(Fallback.class);

        if (targetMethod.isAnnotationPresent(CircuitBreaker.class))
            circuitBreaker = targetMethod.getAnnotation(CircuitBreaker.class);
        else if (targetClass.isAnnotationPresent(CircuitBreaker.class))
            circuitBreaker = targetClass.getAnnotation(CircuitBreaker.class);

        Class<? extends FallbackHandler> fallbackHandlerClass = getFallbackHandlerClass(fallback, targetMethod);
        Method fallbackMethod = getFallbackMethod(fallback, targetClass, targetMethod);
        Class<? extends Throwable>[] failOn = circuitBreaker.failOn();

        ConfigurationUtil configurationUtil = ConfigurationUtil.getInstance();

        ExecutionMetadata metadata = new ExecutionMetadata(targetClass, targetMethod, fallbackHandlerClass,
                fallbackMethod, commandKey, groupKey, failOn);

        // initialize parameters from annottaions
        Duration timeoutDuration = Duration.of(timeout.value(), timeout.unit());
        ConfigurationProperty timeoutProperty = new ConfigurationProperty(FaultToleranceConfigurationType.COMMAND,
                commandKey, "timeout");
        Optional<String> timeoutConfig = configurationUtil.get(timeoutProperty.configurationPath());

        if (timeoutConfig.isPresent()) {
            timeoutDuration = FaultToleranceHelper.parseDuration(timeoutConfig.get());
            watch(timeoutProperty);
        }

        timeoutProperty.setValue(timeoutDuration);
        circuitBreakerExecutor.setPropertyValue(timeoutProperty);

        int rvThreshold = circuitBreaker.requestVolumeThreshold();
        ConfigurationProperty rvThresholdProperty = new ConfigurationProperty(FaultToleranceConfigurationType.COMMAND,
                commandKey, "request-volume-threshold");
        Optional<Integer> rvThresholdConfig = configurationUtil.getInteger(rvThresholdProperty.configurationPath());

        if (rvThresholdConfig.isPresent()) {
            rvThreshold = rvThresholdConfig.get();
            watch(rvThresholdProperty);
        }

        rvThresholdProperty.setValue(rvThreshold);
        circuitBreakerExecutor.setPropertyValue(rvThresholdProperty);

        double failureRatio = circuitBreaker.failureRatio();
        ConfigurationProperty failureRatioProperty = new ConfigurationProperty(FaultToleranceConfigurationType.COMMAND,
                commandKey, "failure-ratio");
        Optional<Double> failureRatioConfig = configurationUtil.getDouble(failureRatioProperty.configurationPath());

        if (failureRatioConfig.isPresent()) {
            failureRatio = failureRatioConfig.get();
            watch(failureRatioProperty);
        }

        failureRatioProperty.setValue(failureRatio);
        circuitBreakerExecutor.setPropertyValue(failureRatioProperty);

        Duration delay = Duration.of(circuitBreaker.delay(), circuitBreaker.delayUnit());
        ConfigurationProperty delayProperty = new ConfigurationProperty(FaultToleranceConfigurationType.COMMAND,
                commandKey, "delay");
        Optional<String> delayConfig = configurationUtil.get(delayProperty.configurationPath());

        if (delayConfig.isPresent()) {
            delay = FaultToleranceHelper.parseDuration(delayConfig.get());
            watch(delayProperty);
        }

        delayProperty.setValue(delay);
        circuitBreakerExecutor.setPropertyValue(delayProperty);


        metadatasMap.put(key, metadata);


        return metadata;
    }

    private String getGroupKey(Method targetMethod, Class<?> targetClass) {

        CircuitBreakerGroupKey cbgk = null;

        if (targetClass.isAnnotationPresent(CircuitBreakerGroupKey.class))
            cbgk = targetClass.getAnnotation(CircuitBreakerGroupKey.class);

        if (targetMethod.isAnnotationPresent(CircuitBreakerGroupKey.class))
            cbgk = targetMethod.getAnnotation(CircuitBreakerGroupKey.class);

        if (cbgk != null && !cbgk.value().equals(""))
            return cbgk.value();
        else
            return targetClass.getName();
    }

    private String getKey(Method targetMethod, Class<?> targetClass) {

        CircuitBreakerKey cbk = null;

        if (targetClass.isAnnotationPresent(CircuitBreakerKey.class))
            cbk = targetClass.getAnnotation(CircuitBreakerKey.class);

        if (targetMethod.isAnnotationPresent(CircuitBreakerKey.class))
            cbk = targetMethod.getAnnotation(CircuitBreakerKey.class);

        if (cbk != null && !cbk.value().equals(""))
            return cbk.value();
        else
            return targetMethod.getName();
    }

    private Class<? extends FallbackHandler> getFallbackHandlerClass(Fallback fallback, Method targetMethod) {

        if (fallback == null || Fallback.DEFAULT.class.isInstance(fallback.value()))
            return null;

        Class<? extends FallbackHandler<?>> fallbackClass = fallback.value();

        Method[] handleInterfaceMethods = FallbackHandler.class.getMethods();

        for (Method method : handleInterfaceMethods) {
            if (method.getName().equals("handle")) {
                try {
                    Method fallbackMethod = fallbackClass.getMethod(method.getName(), method.getParameterTypes());

                    if (targetMethod.getReturnType().equals(fallbackMethod.getReturnType()))
                        return fallback.value();
                } catch (NoSuchMethodException e) {
                }

                break;
            }
        }

        return null;
    }

    private Method getFallbackMethod(Fallback fallback, Class targetClass, Method targetMethod) {

        if (fallback == null || !Fallback.DEFAULT.class.isInstance(fallback.value()) ||
                fallback.fallbackMethod().equals(""))
            return null;

        String methodName = fallback.fallbackMethod();

        if (methodName != null && methodName.length() > 0) {
            for (Method m : targetClass.getMethods()) {
                if (m.getName().equals(methodName) &&
                        m.getReturnType().equals(targetMethod.getReturnType()) &&
                        m.getParameterCount() == targetMethod.getParameterCount()) {
                    boolean parameterMatch = true;

                    for (int i = 0; i < m.getParameterTypes().length; i++) {
                        if (!m.getParameterTypes()[i].equals(targetMethod.getParameterTypes()[i])) {
                            parameterMatch = false;
                            break;
                        }
                    }

                    if (parameterMatch)
                        return m;
                }
            }
        }

        return null;
    }

    private boolean targetClassIsProxied(Class targetClass) {
        return targetClass.getCanonicalName().contains("$Proxy");
    }

}
