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
package com.kumuluz.ee.circuit.breaker.utils;

import com.kumuluz.ee.circuit.breaker.annotations.CircuitBreaker;
import com.kumuluz.ee.circuit.breaker.annotations.CircuitBreakerGroup;
import com.kumuluz.ee.circuit.breaker.models.CircuitBreakerConfigurationType;
import com.kumuluz.ee.circuit.breaker.models.ConfigurationProperty;
import com.kumuluz.ee.circuit.breaker.models.ExecutionMetadata;
import com.kumuluz.ee.configuration.ConfigurationListener;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import org.jboss.weld.context.RequestContext;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Util for executing method in circuit breaker and setting up configuration
 *
 * @author Luka Šarc
 */
@ApplicationScoped
public class CircuitBreakerUtil {

    private static final Logger log = Logger.getLogger(CircuitBreakerUtil.class.getName());

    public static final String SERVICE_NAME = "circuit-breaker";
    private static final int QUEUE_PROCESS_LIMIT = 20;

    private static CircuitBreakerUtil instance;

    private Boolean watchEnabled;
    private List<String> watchProperties;

    private Map<String, ExecutionMetadata> metadatasMap;
    private Queue<ConfigurationProperty> updatePropertiesQueue;
    private Map<String, ConfigurationListener> configListenersMap;

    @Inject
    private CircuitBreakerExecutor circuitBreakerExecutor;

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

                    if (CircuitBreakerHelper.isInt(updatedValue)) {
                        updatedProperty.setValue(CircuitBreakerHelper.parseInt(updatedValue));
                        valueParsed = true;
                    } else if (CircuitBreakerHelper.isBoolean(updatedValue)) {
                        updatedProperty.setValue(CircuitBreakerHelper.parseBoolean(updatedValue));
                        valueParsed = true;
                    } else if (CircuitBreakerHelper.isTime(updatedValue)) {
                        updatedProperty.setValue(CircuitBreakerHelper.parseTime(updatedValue));
                        updatedProperty.setTimeUnit(CircuitBreakerHelper.parseTimeUnit(updatedValue));
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
        while(updatePropertiesQueue.peek() != null && cnt < QUEUE_PROCESS_LIMIT) {
            ConfigurationProperty prop = updatePropertiesQueue.poll();
            circuitBreakerExecutor.setPropertyValue(prop);
            cnt++;
        }
    }

    private ExecutionMetadata toExecutionMetadata(InvocationContext ic) {

        Method targetMethod = ic.getMethod();

        CircuitBreaker cb = targetMethod.getAnnotation(CircuitBreaker.class);

        Object targetObject = ic.getTarget();
        Class targetClass = targetObject.getClass();

        if (targetClassIsProxied(targetClass))
            targetClass = targetClass.getSuperclass();

        String commandKey = getKey(targetMethod);
        String groupKey = getGroupKey(commandKey, targetMethod, targetClass);
        String key = groupKey + "-" + commandKey;

        if (metadatasMap.containsKey(key)) {
            return metadatasMap.get(key);
        }

        log.finest("Initializing execution metadata for key '" + key + "'.");

        Method fallbackMethod = getFallbackMethod(cb.fallbackMethod(), targetClass, targetMethod);
        Object[] methodParameters = ic.getParameters();
        Class[] skipFallbackClasses = getSkipFallbackClasses(targetMethod);

        ConfigurationUtil configurationUtil = ConfigurationUtil.getInstance();

        ExecutionMetadata metadata = new ExecutionMetadata(targetClass, targetMethod, fallbackMethod,
                commandKey, groupKey, skipFallbackClasses);

        int timeout = cb.timeout();
        TimeUnit timeoutUnit = cb.timeoutUnit();
        ConfigurationProperty timeoutProperty = new ConfigurationProperty(CircuitBreakerConfigurationType.COMMAND,
                commandKey, "timeout");
        if (timeout == -1) {
            String timeoutStr = configurationUtil.get(timeoutProperty.configurationPath()).orElse("1000ms");
            timeout = CircuitBreakerHelper.parseTime(timeoutStr);
            timeoutUnit = CircuitBreakerHelper.parseTimeUnit(timeoutStr);
            watch(timeoutProperty);
        }
        timeoutProperty.setTimeUnit(timeoutUnit);
        timeoutProperty.setValue(timeout);
        circuitBreakerExecutor.setPropertyValue(timeoutProperty);

        int requestThreshold = cb.requestThreshold();
        ConfigurationProperty requestThresholdProperty = new ConfigurationProperty(CircuitBreakerConfigurationType.COMMAND,
                commandKey, "request-threshold");
        if (requestThreshold == -1) {
            requestThreshold = configurationUtil.getInteger(requestThresholdProperty.configurationPath()).orElse(20);
            watch(requestThresholdProperty);
        }
        requestThresholdProperty.setValue(requestThreshold);
        circuitBreakerExecutor.setPropertyValue(requestThresholdProperty);

        int failureThreshold = cb.failureThreshold();
        ConfigurationProperty failureThresholdProperty = new ConfigurationProperty(CircuitBreakerConfigurationType.COMMAND,
                commandKey, "failure-threshold");
        if (failureThreshold == -1) {
            failureThreshold = configurationUtil.getInteger(failureThresholdProperty.configurationPath()).orElse(50);
            watch(failureThresholdProperty);
        }
        failureThresholdProperty.setValue(failureThreshold);
        circuitBreakerExecutor.setPropertyValue(failureThresholdProperty);

        int openCircuitWait = cb.openCircuitWait();
        TimeUnit openCircuitWaitUnit = cb.openCircuitWaitUnit();
        ConfigurationProperty openCircuitWaitProperty = new ConfigurationProperty(CircuitBreakerConfigurationType.COMMAND,
                commandKey, "open-circuit-wait");
        if (openCircuitWait == -1) {
            String openCircuitWaitStr = configurationUtil.get(openCircuitWaitProperty.configurationPath()).orElse("5000ms");
            openCircuitWait = CircuitBreakerHelper.parseTime(openCircuitWaitStr);
            openCircuitWaitUnit = CircuitBreakerHelper.parseTimeUnit(openCircuitWaitStr);
            watch(openCircuitWaitProperty);
        }
        openCircuitWaitProperty.setTimeUnit(openCircuitWaitUnit);
        openCircuitWaitProperty.setValue(openCircuitWait);
        circuitBreakerExecutor.setPropertyValue(openCircuitWaitProperty);

        boolean forceClosed = cb.forceClosed();
        ConfigurationProperty forceClosedProperty = new ConfigurationProperty(CircuitBreakerConfigurationType.COMMAND,
                commandKey, "force-closed");
        if (!forceClosed) {
            forceClosed = configurationUtil.getBoolean(forceClosedProperty.configurationPath()).orElse(false);
            watch(forceClosedProperty);
        }
        forceClosedProperty.setValue(forceClosed);
        circuitBreakerExecutor.setPropertyValue(forceClosedProperty);

        boolean forceOpen = cb.forceOpen();
        ConfigurationProperty forceOpenProperty = new ConfigurationProperty(CircuitBreakerConfigurationType.COMMAND,
                commandKey, "force-open");
        if (!forceOpen) {
            forceOpen = configurationUtil.getBoolean(forceOpenProperty.configurationPath()).orElse(false);
            watch(forceOpenProperty);
        }
        forceOpenProperty.setValue(forceOpen);
        circuitBreakerExecutor.setPropertyValue(forceOpenProperty);

        metadatasMap.put(key, metadata);

        return metadata;
    }

    private String getGroupKey(String commandKey, Method targetMethod, Class<?> targetClass) {

        if (targetMethod.isAnnotationPresent(CircuitBreakerGroup.class)) {
            CircuitBreakerGroup cbg = targetMethod.getAnnotation(CircuitBreakerGroup.class);

            if (!cbg.value().equals(""))
                return cbg.value();
        }

        CircuitBreaker cb = targetMethod.getAnnotation(CircuitBreaker.class);

        if (cb.group() != null && !cb.group().equals(""))
            return cb.group();

        if (targetClass.isAnnotationPresent(CircuitBreakerGroup.class)) {
            CircuitBreakerGroup cbg = (CircuitBreakerGroup) targetClass.getAnnotation(CircuitBreakerGroup.class);

            if (!cbg.value().equals(""))
                return cbg.value();
        }

        Optional<String> groupConfig = ConfigurationUtil.getInstance()
                .get(CircuitBreakerHelper.getBaseConfigPath(CircuitBreakerConfigurationType.COMMAND, commandKey, null) +
                        ".group");

        if (groupConfig.isPresent())
            return groupConfig.get();


        return targetClass.getSimpleName();
    }

    private String getKey(Method targetMethod) {

        CircuitBreaker cb = targetMethod.getAnnotation(CircuitBreaker.class);

        if (cb.key() != null && !cb.key().equals(""))
            return cb.key();
        else
            return targetMethod.getName();
    }

    private Method getFallbackMethod(String methodName, Class targetClass, Method targetMethod) {

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

    private Class[] getSkipFallbackClasses(Method targetMethod) {

        List<Class<?>> skipFallbackExceptions = new ArrayList();

        for (Class<?> c : targetMethod.getExceptionTypes())
            skipFallbackExceptions.add(c);

        CircuitBreaker cb = targetMethod.getAnnotation(CircuitBreaker.class);

        for (Class<?> c : cb.skipFallbackOn()) {
            if (!skipFallbackExceptions.contains(c))
                skipFallbackExceptions.add(c);
        }

        return skipFallbackExceptions.toArray(new Class[skipFallbackExceptions.size()]);
    }

    private boolean targetClassIsProxied(Class targetClass) {
        return targetClass.getCanonicalName().contains("$Proxy");
    }

}
