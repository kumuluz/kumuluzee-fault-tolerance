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

import com.kumuluz.ee.configuration.ConfigurationListener;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import com.kumuluz.ee.fault.tolerance.annotations.*;
import com.kumuluz.ee.fault.tolerance.enums.FaultToleranceType;
import com.kumuluz.ee.fault.tolerance.interfaces.FallbackHandler;
import com.kumuluz.ee.fault.tolerance.interfaces.FaultToleranceExecutor;
import com.kumuluz.ee.fault.tolerance.interfaces.FaultToleranceUtil;
import com.kumuluz.ee.fault.tolerance.models.ConfigurationProperty;
import com.kumuluz.ee.fault.tolerance.models.ExecutionMetadata;
import org.jboss.weld.context.RequestContext;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Logger;

/**
 * Util for setting up basic configuration and passing execution of intercepted method within
 * fault tolerance executor
 *
 * @author Luka Šarc
 * @since 1.0.0
 */
@ApplicationScoped
public class FaultToleranceUtilImpl implements FaultToleranceUtil {

    private static final Logger log = Logger.getLogger(FaultToleranceUtilImpl.class.getName());

    public static final String SERVICE_NAME = "fault-tolerance";
    private static final int CONFIG_WATCH_QUEUE_UPDATE_LIMIT = 50;

    private Boolean watchEnabled;
    private List<String> watchProperties;

    private Map<String, ExecutionMetadata> metadatasMap;
    private Map<String, ConfigurationListener> configListenersMap;
    private Queue<ConfigurationProperty> updatePropertiesQueue;

    @Inject
    private FaultToleranceExecutor executor;

    @PostConstruct
    public void init() {

        metadatasMap = new HashMap<>();
        updatePropertiesQueue = new LinkedList<>();
        configListenersMap = new HashMap<>();

        ConfigurationUtil configUtil = ConfigurationUtil.getInstance();

        Optional<Boolean> watchEnabledOptional = configUtil.getBoolean(SERVICE_NAME + ".config.watch-enabled");
        if (watchEnabledOptional.isPresent())
            watchEnabled = watchEnabledOptional.get();
        else
            watchEnabled = null;

        if (watchEnabled == null || watchEnabled.booleanValue()) {
            Optional<String> watchPropertiesOptional = configUtil.get(SERVICE_NAME + ".config.watch-properties");

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

    /**
     * Executes method intercepted by interceptor with executor
     * @param invocationContext Invocation context provided by interceptor
     * @param requestContext    Request context provided by interceptor
     * @return                  Result of method execution
     * @throws Exception
     */
    @Override
    public Object execute(InvocationContext invocationContext, RequestContext requestContext) throws Exception {

        ExecutionMetadata config = toExecutionMetadata(invocationContext);

        updateConfigurations();

        return executor.execute(invocationContext, requestContext, config);
    }

    /**
     * Checks if watch is enabled for property
     * @param property  ConfigurationProperty object to check watch for
     * @return          True if watch is enabled, false otherwise
     */
    @Override
    public boolean isWatchEnabled(ConfigurationProperty property) {

        String configPath = property.configurationPath();
        return watchEnabled != null && watchEnabled.booleanValue() &&
                watchProperties.stream().anyMatch(p -> configPath.endsWith(p));
    }

    /**
     * Initiates watch for property if watch is enabled and not already set
     * @param property  ConfigurationProperty object to initiate watch for
     */
    @Override
    public void watch(ConfigurationProperty property) {

        if (!isWatchEnabled(property) || configListenersMap.containsKey(property.configurationPath()))
            return;

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
                    } else if (FaultToleranceHelper.isDouble(updatedValue)) {
                        updatedProperty.setValue(FaultToleranceHelper.parseDouble(updatedValue));
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

    /**
     * Removes watch for property
     * @param property  ConfigurationProperty object to remove watch for
     */
    @Override
    public void removeWatch(ConfigurationProperty property) {

        String configPath = property.configurationPath();

        if (configListenersMap.containsKey(configPath)) {
            ConfigurationUtil.getInstance().unsubscribe(configListenersMap.get(configPath));
            configListenersMap.remove(configPath);
        }
    }

    /**
     * Updates received updated configurations for watched configuration properties
     */
    @Override
    public void updateConfigurations() {

        int cnt = 0;
        while(updatePropertiesQueue.peek() != null && cnt < CONFIG_WATCH_QUEUE_UPDATE_LIMIT) {
            ConfigurationProperty prop = updatePropertiesQueue.poll();
            executor.setPropertyValue(prop);
            cnt++;
        }
    }

    @Override
    public Optional<ConfigurationProperty> findConfig(FaultToleranceType type, String propertyPath) {
        return findConfig(null, null, type, propertyPath);
    }

    @Override
    public Optional<ConfigurationProperty> findConfig(String groupKey, FaultToleranceType type, String propertyPath) {
        return findConfig(null, groupKey, type, propertyPath);
    }

    @Override
    public Optional<ConfigurationProperty> findConfig(String commandKey, String groupKey, FaultToleranceType type, String propertyPath) {

        log.finest("Searhing configuration for '" + commandKey + "', '" + groupKey + "', '" + type.getKey() +
                "', '" + propertyPath + "'.");
        ConfigurationUtil configUtil = ConfigurationUtil.getInstance();

        ConfigurationProperty resultProperty = null;
        Optional<String> value = null;

        if (commandKey != null && groupKey != null) {
            resultProperty = new ConfigurationProperty(commandKey, groupKey, type, propertyPath);
            value = configUtil.get(resultProperty.configurationPath());

            if (value.isPresent()) {
                log.finest("Found configuration at path '" + resultProperty.configurationPath() + "'.");

                return Optional.of(resultProperty);
            }
        }

        if (commandKey == null && groupKey != null || resultProperty != null) {
            resultProperty = new ConfigurationProperty(groupKey, type, propertyPath);
            value = configUtil.get(resultProperty.configurationPath());

            if (value.isPresent()) {
                log.finest("Found configuration at path '" + resultProperty.configurationPath() + "'.");

                return Optional.of(resultProperty);
            }
        }

        if (commandKey == null && groupKey == null || resultProperty != null) {
            resultProperty = new ConfigurationProperty(type, propertyPath);
            value = configUtil.get(resultProperty.configurationPath());

            if (value.isPresent()) {
                log.finest("Found configuration at path '" + resultProperty.configurationPath() + "'.");

                return Optional.of(resultProperty);
            }
        }

        log.finest("No configuration was found.");

        return Optional.empty();
    }

    /**
     * Creates ExecutionMetadata object with execution info from invocation context or retreives it
     * from map if exists already
     * @param ic    InvocationContext associated with the execution
     * @return      ExecutionMetadata object with execution info
     */
    private ExecutionMetadata toExecutionMetadata(InvocationContext ic) {

        Method targetMethod = ic.getMethod();
        Object targetObject = ic.getTarget();
        Class<?> targetClass = targetObject.getClass();

        if (targetClassIsProxied(targetClass))
            targetClass = targetClass.getSuperclass();

        String commandKey = getCommandKey(targetMethod);
        String groupKey = getGroupKey(targetClass, targetMethod);
        String key = groupKey + "." + commandKey;

        if (metadatasMap.containsKey(key))
            return metadatasMap.get(key);

        log.finest("Initializing execution metadata for key '" + key + "'.");

        Asynchronous asynchronous = null;
        Bulkhead bulkhead = null;
        Timeout timeout = null;
        Fallback fallback = null;
        Retry retry = null;
        CircuitBreaker circuitBreaker = null;

        // check for asynchronous annotation
        if (targetMethod.isAnnotationPresent(Asynchronous.class))
            asynchronous = targetMethod.getAnnotation(Asynchronous.class);
        else if (targetClass.isAnnotationPresent(Asynchronous.class))
            asynchronous = targetClass.getAnnotation(Asynchronous.class);

        // check for bulkhead annotation
        if (targetMethod.isAnnotationPresent(Bulkhead.class))
            bulkhead = targetMethod.getAnnotation(Bulkhead.class);
        else if (targetClass.isAnnotationPresent(Bulkhead.class))
            bulkhead = targetClass.getAnnotation(Bulkhead.class);

        // check for timeout annotation
        if (targetMethod.isAnnotationPresent(Timeout.class))
            timeout = targetMethod.getAnnotation(Timeout.class);
        else if (targetClass.isAnnotationPresent(Timeout.class))
            timeout = targetClass.getAnnotation(Timeout.class);

        // checko for fallback annotation
        if (targetMethod.isAnnotationPresent(Fallback.class))
            fallback = targetMethod.getAnnotation(Fallback.class);
        else if (targetClass.isAnnotationPresent(Fallback.class))
            fallback = targetClass.getAnnotation(Fallback.class);

        // check for retry annotation
        if (targetMethod.isAnnotationPresent(Retry.class))
            retry = targetMethod.getAnnotation(Retry.class);
        else if (targetClass.isAnnotationPresent(Retry.class))
            retry = targetClass.getAnnotation(Retry.class);

        // check for circuit breaker annotation
        if (targetMethod.isAnnotationPresent(CircuitBreaker.class))
            circuitBreaker = targetMethod.getAnnotation(CircuitBreaker.class);
        else if (targetClass.isAnnotationPresent(CircuitBreaker.class))
            circuitBreaker = targetClass.getAnnotation(CircuitBreaker.class);

        boolean isAsync = asynchronous != null;

        if (isAsync) {
            // TODO: Check if returns Future<T>
        }

        Class<? extends FallbackHandler> fallbackHandlerClass = getFallbackHandlerClass(fallback, targetMethod);
        Method fallbackMethod = getFallbackMethod(fallback, targetClass, targetMethod);

        ExecutionMetadata metadata = new ExecutionMetadata(targetClass, targetMethod, commandKey, groupKey);

        metadata.setAsynchronous(isAsync);
        metadata.setFallbackHandlerClass(fallbackHandlerClass);
        metadata.setFallbackMethod(fallbackMethod);

        metadata.setBulkhead(bulkhead);
        metadata.setTimeout(timeout);
        metadata.setRetry(retry);
        metadata.setCircuitBreaker(circuitBreaker);

        metadatasMap.put(key, metadata);

        return metadata;
    }

    /**
     * Constructs command key. By default target method is used. If @CommandKey annotation is present,
     * it's value is used instead.
     * @param targetMethod  Execution target method
     * @return              Command name
     */
    private String getCommandKey(Method targetMethod) {

        CommandKey annotKey = null;

        if (targetMethod.isAnnotationPresent(CommandKey.class))
            annotKey = targetMethod.getAnnotation(CommandKey.class);

        if (annotKey != null && !annotKey.value().equals(""))
            return annotKey.value();
        else
            return targetMethod.getName();
    }

    /**
     * Constructs group key. By default target class simple name is used. If @Bulkhead annotation is used on
     * target method, then target method name is set as group key. In case of @GroupKey annotation, it's value
     * is used as group key name instead of target class or method name.
     * @param targetClass   Execution target class
     * @param targetMethod  Execution target method
     * @return              Group name
     */
    private String getGroupKey(Class<?> targetClass, Method targetMethod) {

        boolean method = targetMethod.isAnnotationPresent(Bulkhead.class);
        String key = targetClass.getSimpleName() + (method ? "-" + targetMethod.getName() : "");
        GroupKey annotKey = null;

        if (method && targetMethod.isAnnotationPresent(GroupKey.class))
            annotKey = targetMethod.getAnnotation(GroupKey.class);
        else if (!method && targetClass.isAnnotationPresent(GroupKey.class))
            annotKey = targetClass.getAnnotation(GroupKey.class);

        if (annotKey != null && !annotKey.value().equals(""))
            key = annotKey.value();

        return key;
    }

    /**
     * Extracts fallback handler class if defined, checks if return type of handle method in FallbackHandler
     * implementation matches target method return type
     * @param fallback      Fallback annotation associated with target method
     * @param targetMethod  Execution target method
     * @return              FallbackHandler implementation's class
     */
    private Class<? extends FallbackHandler> getFallbackHandlerClass(Fallback fallback, Method targetMethod) {

        if (fallback == null || Fallback.DEFAULT.class.equals(fallback.value()))
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

    /**
     * Finds fallback method if defined using reflection, checks if fallback method matches return type and
     * parameter types with target method
     * @param fallback      Fallback annotation associated with target method
     * @param targetClass   Execution target class
     * @param targetMethod  Execution target method
     * @return              Fallback reflection Method object
     */
    private Method getFallbackMethod(Fallback fallback, Class targetClass, Method targetMethod) {

        if (fallback == null || !Fallback.DEFAULT.class.equals(fallback.value()) ||
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

    /**
     * Check if target class is proxied due to CDI use
     * @param targetClass
     * @return true if target class is proxied, false otherwise
     */
    private boolean targetClassIsProxied(Class targetClass) {
        return targetClass.getCanonicalName().contains("$Proxy");
    }

}
