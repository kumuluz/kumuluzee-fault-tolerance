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
package com.kumuluz.ee.fault.tolerance;

import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import com.kumuluz.ee.fault.tolerance.commands.HystrixGenericCommand;
import com.kumuluz.ee.fault.tolerance.interfaces.FaultToleranceExecutor;
import com.kumuluz.ee.fault.tolerance.models.ConfigurationProperty;
import com.kumuluz.ee.fault.tolerance.models.ExecutionMetadata;
import com.kumuluz.ee.fault.tolerance.models.FaultToleranceConfigurationType;
import com.kumuluz.ee.fault.tolerance.utils.FaultToleranceHelper;
import com.kumuluz.ee.fault.tolerance.utils.FaultToleranceUtil;
import com.netflix.config.ConfigurationManager;
import com.netflix.hystrix.*;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import org.jboss.weld.context.RequestContext;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.interceptor.InvocationContext;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Hystrix implementation of circuit breaker executor
 *
 * @author Luka Šarc
 */
@RequestScoped
public class HystrixFaultToleranceExecutorImpl implements FaultToleranceExecutor {

    private static final String NAME = "hystrix";

    private static final Logger log = Logger.getLogger(HystrixFaultToleranceExecutorImpl.class.getName());

    private static HashMap<String, HystrixCommand.Setter> hystrixCommandSetters;
    private static HashMap<String, HystrixCommandKey> hystrixCommandKeys;
    private static HashMap<String, HystrixThreadPoolKey> hystrixThreadPoolKeys;

    private static HashMap<String, String> generalPropertiesMap;
    private static HashMap<String, String> commonPropertiesMap;
    private static HashMap<String, String> commandPropertiesMap;
    private static HashMap<String, String> threadPoolPropertiesMap;

    @Inject
    private FaultToleranceUtil faultToleranceUtil;

    static {
        hystrixCommandSetters = new HashMap<>();
        hystrixCommandKeys = new HashMap<>();
        hystrixThreadPoolKeys = new HashMap<>();

        generalPropertiesMap = new HashMap<>();
        commonPropertiesMap = new HashMap<>();
        commandPropertiesMap = new HashMap<>();
        threadPoolPropertiesMap = new HashMap<>();

        // annotation confgigurable or general properties
        generalPropertiesMap.put("timeout", "execution.isolation.thread.timeoutInMilliseconds");
        generalPropertiesMap.put("request-volume-threshold", "circuitBreaker.requestVolumeThreshold");
        generalPropertiesMap.put("failure-ratio", "circuitBreaker.errorThresholdPercentage");
        generalPropertiesMap.put("delay", "circuitBreaker.sleepWindowInMilliseconds");
        generalPropertiesMap.put("force-open", "circuitBreaker.forceOpen");
        generalPropertiesMap.put("force-closed", "circuitBreaker.forceClosed");

        // hystrix specific common properties
        commonPropertiesMap.put("metrics-rolling-statistical-window", "metrics.rollingStats.timeInMilliseconds");
        commonPropertiesMap.put("metrics-rolling-statistical-window-buckets", "metrics.rollingStats.numBuckets");

        // hystrix specific command properties
        commandPropertiesMap.put("enabled", "circuitBreaker.enabled");
        commandPropertiesMap.put("execution-strategy", "execution.isolation.strategy");
        commandPropertiesMap.put("thread-execution-timeout-interrupt", "execution.isolation.thread.interruptOnTimeout");
        commandPropertiesMap.put("thread-execution-cancel-interrupt", "execution.isolation.thread.interruptOnCancel");
        commandPropertiesMap.put("semaphore-execution-max-concurrent-requests", "execution.isolation.semaphore.maxConcurrentRequests");
        commandPropertiesMap.put("semaphore-fallback-max-concurrent-requests", "fallback.isolation.semaphore.maxConcurrentRequests");
        commandPropertiesMap.put("timeout-enabled", "execution.timeout.enabled");
        commandPropertiesMap.put("fallback-enabled", "fallback.enabled");
        commandPropertiesMap.put("metrics-rolling-percentile-enabled", "metrics.rollingPercentile.enabled");
        commandPropertiesMap.put("metrics-rolling-percentile-window", "metrics.rollingPercentile.timeInMilliseconds");
        commandPropertiesMap.put("metrics-rolling-percentile-buckets", "metrics.rollingPercentile.numBuckets");
        commandPropertiesMap.put("metrics-rolling-percentile-bucket-size", "metrics.rollingPercentile.bucketSize");
        commandPropertiesMap.put("metrics-health-interval", "metrics.healthSnapshot.intervalInMilliseconds");
        commandPropertiesMap.put("request-log", "requestLog.enabled");

        // hystrix specific thread pool properties
        threadPoolPropertiesMap.put("threads-core", "coreSize");
        threadPoolPropertiesMap.put("threads-max", "maximumSize");
        threadPoolPropertiesMap.put("threads-keep-alive", "keepAliveTimeMinutes");
        threadPoolPropertiesMap.put("threads-diverge-from-core", "allowMaximumSizeToDivergeFromCoreSize");
        threadPoolPropertiesMap.put("queue-max-size", "maxQueueSize");
        threadPoolPropertiesMap.put("queue-size-rejection-threshold", "queueSizeRejectionThreshold");
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Object execute(InvocationContext invocationContext, RequestContext requestContext, ExecutionMetadata metadata) throws Exception {

        HystrixCommand.Setter hystrixCommand = getHystrixCommandSetter(metadata);

        HystrixGenericCommand cmd = new HystrixGenericCommand(hystrixCommand, invocationContext, requestContext, metadata);

        try {
            return cmd.execute();
        } catch (HystrixBadRequestException e) {
            throw (Exception) e.getCause();
        } catch (HystrixRuntimeException e) {
            log.warning("Hystrix runtime exception was thrown because of " + e.getCause().getClass().getName());
            throw (Exception) e.getCause();
        }
    }

    @Override
    public void setPropertyValue(ConfigurationProperty property) {

        log.finest("Received kumuluzee configuration property '" + property.configurationPath() +
                "' with value '" + property.getValue() + "'.");

        String hystrixProperty = mapConfigProperty(property.getExecutorName(), property.getType(), property.getProperty());

        if (property.getExecutorName() != null && !property.getExecutorName().equals(getName()) ||
                hystrixProperty == null)
            return;

        String hystrixPropertyKey = toHystrixPropertyKey(property.getType(), property.getTypeKey(), hystrixProperty);
        Object hystrixPropertyValue = toHystrixPropertyValue(hystrixProperty, property.getValue());

        log.finest("Setting hystrix configuration property '" + hystrixPropertyKey +
                "' with value '" + hystrixPropertyValue + "'.");

        setHystrixProperty(hystrixPropertyKey, hystrixPropertyValue);
    }

    @Override
    public ConfigurationProperty getPropertyValue(ConfigurationProperty property) {

        String hystrixProperty = mapConfigProperty(property.getExecutorName(), property.getType(), property.getProperty());
        String hystrixPropertyKey = toHystrixPropertyKey(property.getType(), property.getTypeKey(), hystrixProperty);

        Object value = getHystrixProperty(hystrixPropertyKey);
        ChronoUnit timeUnit = getHystrixPropertyTimeUnit(hystrixProperty);

        if (timeUnit != null) {
            Long valueLong = FaultToleranceHelper.toLongValue(value);
            property.setValue(Duration.of(valueLong, timeUnit));
        } else {
            property.setValue(getHystrixProperty(hystrixPropertyKey));
        }

        return property;
    }

    private HystrixCommand.Setter getHystrixCommandSetter(ExecutionMetadata metadata) {

        String key = metadata.getGroupKey() + "-" + metadata.getCommandKey();

        if (hystrixCommandSetters.containsKey(key))
            return hystrixCommandSetters.get(key);

        log.finest("Initializing Hystrix command setter for key '" + key + "'.");

        HystrixCommandKey commandKey = getHystrixCommandKey(metadata);
        HystrixCommandGroupKey groupKey = getHystrixCommandGroupKey(metadata);
        HystrixThreadPoolKey threadPoolKey = getHystrixThreadPoolKey(metadata);

        HystrixCommand.Setter setter = HystrixCommand.Setter
                .withGroupKey(groupKey)
                .andThreadPoolKey(threadPoolKey)
                .andCommandKey(commandKey);

        hystrixCommandSetters.put(key, setter);

        return setter;
    }

    private HystrixCommandKey getHystrixCommandKey(ExecutionMetadata metadata) {

        String key = metadata.getCommandKey();

        if (hystrixCommandKeys.containsKey(key))
            return hystrixCommandKeys.get(key);

        log.finest("Initializing Hystrix command key object for key '" + key + "'.");

        HystrixCommandKey commandKey = HystrixCommandKey.Factory.asKey(key);
        initializeCommandProperties(key);

        // check if fallback is not defined and disable its execution
        if (metadata.getFallbackHandlerClass() == null && metadata.getFallbackMethod() == null) {
            ConfigurationProperty configProperty = new ConfigurationProperty(getName(), FaultToleranceConfigurationType.COMMAND,
                    key, "fallback-enabled");
            String hystrixConfigProperty = toHystrixPropertyKey(configProperty.getType(), configProperty.getTypeKey(),
                    commandPropertiesMap.get(configProperty.getProperty()));

            setHystrixProperty(hystrixConfigProperty, false);
            faultToleranceUtil.removeWatch(configProperty);
        }

        hystrixCommandKeys.put(key, commandKey);

        return commandKey;
    }

    private HystrixCommandGroupKey getHystrixCommandGroupKey(ExecutionMetadata metadata) {

        HystrixCommandGroupKey commandGroupKey = HystrixCommandGroupKey.Factory
                .asKey(metadata.getGroupKey());

        return commandGroupKey;
    }

    private HystrixThreadPoolKey getHystrixThreadPoolKey(ExecutionMetadata metadata) {

        String key = metadata.getGroupKey();
        String configKey = FaultToleranceHelper.getBaseConfigPath(FaultToleranceConfigurationType.COMMAND,
                metadata.getCommandKey(), getName());

        Optional<String> optionalKey = ConfigurationUtil.getInstance().get(configKey + ".thread-pool");
        if (optionalKey.isPresent())
            key = optionalKey.get();

        if (hystrixThreadPoolKeys.containsKey(key))
            return hystrixThreadPoolKeys.get(key);

        log.finest("Initializing Hystrix thread pool key object for key '" + key + "'.");

        HystrixThreadPoolKey threadPoolKey = HystrixThreadPoolKey.Factory.asKey(key);
        initializeThreadPoolProperties(key);

        hystrixThreadPoolKeys.put(key, threadPoolKey);

        return threadPoolKey;
    }

    private void setHystrixProperty(String key, Object value) {
        ConfigurationManager.getConfigInstance().setProperty(key, value);
    }

    private Object getHystrixProperty(String key) {
        return ConfigurationManager.getConfigInstance().getProperty(key);
    }

    private void initializeCommandProperties(String commandKey) {

        log.finest("Initializing Hystrix command properties for key '" + commandKey + "'.");

        commonPropertiesMap.entrySet().forEach(entry -> initializeProperty(FaultToleranceConfigurationType.COMMAND,
                commandKey, entry.getKey(), entry.getValue()));

        commandPropertiesMap.entrySet().forEach(entry -> initializeProperty(FaultToleranceConfigurationType.COMMAND,
                commandKey, entry.getKey(), entry.getValue()));
    }

    private void initializeThreadPoolProperties(String threadPoolKey) {

        log.finest("Initializing Hystrix thread pool properties for key '" + threadPoolKey + "'.");

        commonPropertiesMap.entrySet().forEach(entry -> initializeProperty(FaultToleranceConfigurationType.THREAD_POOL,
                threadPoolKey, entry.getKey(), entry.getValue()));

        threadPoolPropertiesMap.entrySet().forEach(entry -> initializeProperty(FaultToleranceConfigurationType.THREAD_POOL,
                threadPoolKey, entry.getKey(), entry.getValue()));
    }

    private void initializeProperty(FaultToleranceConfigurationType type, String typeName, String configKey, String hystrixPropertyKey) {

        ConfigurationProperty property = new ConfigurationProperty(getName(), type, typeName, configKey);
        Optional<String> optionalValue = ConfigurationUtil.getInstance()
                .get(property.configurationPath());

        if (optionalValue.isPresent()) {
            String key = toHystrixPropertyKey(type, typeName, hystrixPropertyKey);
            Object value = null;

            if (FaultToleranceHelper.isInt(optionalValue.get())) {
                value = toHystrixPropertyValue(hystrixPropertyKey,
                        FaultToleranceHelper.parseInt(optionalValue.get()));
            } else if (FaultToleranceHelper.isBoolean(optionalValue.get())) {
                value = toHystrixPropertyValue(hystrixPropertyKey,
                        FaultToleranceHelper.parseBoolean(optionalValue.get()));
            } else if (FaultToleranceHelper.isTime(optionalValue.get())) {
                value = toHystrixPropertyValue(hystrixPropertyKey,
                        FaultToleranceHelper.parseDuration(optionalValue.get()));
            } else {
                value = toHystrixPropertyValue(hystrixPropertyKey,
                        optionalValue.get());
            }

            if (value != null && key != null)
                setHystrixProperty(key, value);
        }

        if (isPropertyChangeAvailable(hystrixPropertyKey))
            faultToleranceUtil.watch(property);
    }

    private String mapConfigProperty(String executor, FaultToleranceConfigurationType type, String property) {

        if (executor == null) {
            return generalPropertiesMap.get(property);
        } else if (executor.equals(getName())) {
            if (commonPropertiesMap.containsKey(property))
                return commonPropertiesMap.get(property);
            else if (type == FaultToleranceConfigurationType.COMMAND)
                return commandPropertiesMap.get(property);
            else if (type == FaultToleranceConfigurationType.THREAD_POOL)
                return threadPoolPropertiesMap.get(property);
        }

        return null;
    }

    private String toHystrixPropertyKey(FaultToleranceConfigurationType type, String typeKey, String property) {

        String base;

        switch (type) {
            case COMMAND:
                base = NAME + ".command";
                break;
            case THREAD_POOL:
                base = NAME + ".threadpool";
                break;
            case GROUP:
            default:
                return null;
        }

        return base + "." + typeKey + "." + property;
    }

    private Object toHystrixPropertyValue(String key, Object value) {

        ChronoUnit propChronoUnit = getHystrixPropertyTimeUnit(key);

        if (key.equals("execution.isolation.strategy")) {
            return value.equals("semaphore") ?
                    HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE:
                    HystrixCommandProperties.ExecutionIsolationStrategy.THREAD;
        } else if (propChronoUnit != null && value instanceof Duration) {
            switch (propChronoUnit) {
                case MINUTES:
                    return ((Duration) value).toMinutes();
                case MILLIS:
                default:
                    return ((Duration) value).toMillis();
            }
        } else {
            return value;
        }
    }

    private ChronoUnit getHystrixPropertyTimeUnit(String key) {

        switch (key) {
            case "execution.isolation.thread.timeoutInMilliseconds":
            case "circuitBreaker.sleepWindowInMilliseconds":
            case "metrics.rollingStats.timeInMilliseconds":
            case "metrics.rollingPercentile.timeInMilliseconds":
            case "metrics.healthSnapshot.intervalInMilliseconds":
                return ChronoUnit.MILLIS;
            case "keepAliveTimeMinutes":
                return ChronoUnit.MINUTES;
            default:
                return null;
        }
    }

    private boolean isPropertyChangeAvailable(String key) {

        switch (key) {
            case "metrics.rollingStats.timeInMilliseconds":
            case "metrics.rollingStats.numBuckets":
            case "metrics.rollingPercentile.timeInMilliseconds":
            case "metrics.rollingPercentile.numBuckets":
            case "metrics.rollingPercentile.bucketSize":
            case "maxQueueSize":
                return false;
            default:
                return true;
        }
    }
}
