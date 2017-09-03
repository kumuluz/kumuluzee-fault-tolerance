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
package com.kumuluz.ee.fault.tolerance.configurations;

import com.kumuluz.ee.fault.tolerance.annotations.Bulkhead;
import com.kumuluz.ee.fault.tolerance.annotations.CircuitBreaker;
import com.kumuluz.ee.fault.tolerance.annotations.Timeout;
import com.kumuluz.ee.fault.tolerance.enums.FaultToleranceType;
import com.kumuluz.ee.fault.tolerance.enums.HystrixConfigurationType;
import com.kumuluz.ee.fault.tolerance.models.ConfigurationProperty;
import com.kumuluz.ee.fault.tolerance.models.ExecutionMetadata;

import java.time.Duration;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Hystrix command configuration util
 *
 * @author Luka Šarc
 */
public class CommandHystrixConfigurationUtil extends AbstractHystrixConfigurationUtil {

    private static final Logger log = Logger.getLogger(CommandHystrixConfigurationUtil.class.getName());

    public CommandHystrixConfigurationUtil(HystrixFaultToleranceConfigurationManager configManager) {
        super(configManager);
    }

    public void initialize(ExecutionMetadata metadata) {

        String commandKey = metadata.getCommandKey();
        String groupKey = metadata.getGroupKey();
        FaultToleranceType type;

        type = FaultToleranceType.ASYNCHRONOUS;

        if (!metadata.isAsynchronous()) {
            intializeProperty(commandKey, groupKey, type, "value", false);
        }

        CircuitBreaker cb = metadata.getCircuitBreaker();
        type = FaultToleranceType.CIRCUIT_BREAKER;

        if (cb != null) {
            log.info("Initializing circuit breaker pattern for command '" + commandKey + "'.");

            intializeProperty(commandKey, groupKey, type, "request-volume-threshold", cb.requestVolumeThreshold());
            intializeProperty(commandKey, groupKey, type, "failure-ratio", cb.failureRatio());

            Duration delay = Duration.of(cb.delay(), cb.delayUnit());
            intializeProperty(commandKey, groupKey, type, "delay", delay);

            intializeProperty(commandKey, groupKey, type, "metrics.rolling-window.size", null);
            intializeProperty(commandKey, groupKey, type, "metrics.rolling-window.buckets", null);
            intializeProperty(commandKey, groupKey, type, "metrics.rolling-percentile.enabled", null);
            intializeProperty(commandKey, groupKey, type, "metrics.rolling-percentile.size", null);
            intializeProperty(commandKey, groupKey, type, "metrics.rolling-percentile.buckets", null);
            intializeProperty(commandKey, groupKey, type, "metrics.rolling-percentile.bucket-size", null);
            intializeProperty(commandKey, groupKey, type, "metrics.health-interval", null);

            if (metadata.isAsynchronous()) {
                intializeProperty(commandKey, groupKey, type, "interrupt.on-timeout", null);
                intializeProperty(commandKey, groupKey, type, "interrupt.on-cancel", null);
            }
        } else {
            intializeProperty(commandKey, groupKey, type, "enabled", false);
        }

        type = FaultToleranceType.FALLBACK;
        boolean isFallback = metadata.getFallbackHandlerClass() != null || metadata.getFallbackMethod() != null;

        if (!isFallback) {
            intializeProperty(commandKey, groupKey, type, "enabled", false);
        } else if (cb != null) {
            log.info("Initializing fallback pattern for command '" + commandKey + "'");

            intializeProperty(commandKey, groupKey, type, "max-requests", null);
        }

        Bulkhead bulkhead = metadata.getBulkhead();
        type = FaultToleranceType.BULKHEAD;

        if (bulkhead != null && !metadata.isAsynchronous()) {
            log.info("Initializing semaphored bulkhead pattern for command '" + commandKey + "'.");

            intializeProperty(commandKey, groupKey, type, "bulkhead.value", bulkhead.value());
        }

        Timeout timeout = metadata.getTimeout();
        type = FaultToleranceType.TIMEOUT;

        if (timeout != null) {
            log.info("Initializing timeout pattern for command '" + commandKey + "'.");

            Duration value = Duration.of(timeout.value(), timeout.unit());
            intializeProperty(commandKey, groupKey, type, "value", value);

            intializeProperty(commandKey, groupKey, type, "enabled", true);
        } else {
            intializeProperty(commandKey, groupKey, type, "enabled", false);
        }
    }

    public void updateProperty(ConfigurationProperty property, Object value) {
        setHystrixProperty(property, HystrixConfigurationType.COMMAND, property.getCommandKey(),
                value, true);
    }

    protected void initializeWatchedProperty(ConfigurationProperty property, ConfigurationProperty appliedProperty, Object defaultValue) {

        boolean isChangeable = isHystrixPropertyChangeable(property);

        setHystrixProperty(property, HystrixConfigurationType.COMMAND, property.getCommandKey(),
                appliedProperty.getValue(), isChangeable);

        if (isChangeable) {
            configManager.intializeWatch(HystrixConfigurationType.COMMAND, appliedProperty,
                    property);
        }
    }

    protected String toHystrixPropertyPath(ConfigurationProperty property, boolean changeable) {

        switch (property.typeConfigurationPath()) {
            case "asynchronous.value":
                return changeable ? null : "execution.isolation.strategy";
            case "bulkhead.value":
                return "execution.isolation.semaphore.maxConcurrentRequests";
            case "circuit-breaker.enabled":
                return changeable ? null : "circuitBreaker.enabled";
            case "circuit-breaker.request-volume-threshold":
                return "circuitBreaker.requestVolumeThreshold";
            case "circuit-breaker.failure-ratio":
                return "circuitBreaker.errorThresholdPercentage";
            case "circuit-breaker.delay":
                return "circuitBreaker.sleepWindowInMilliseconds";
            case "circuit-breaker.metrics.rolling-window.size":
                return changeable ? null : "metrics.rollingStats.timeInMilliseconds";
            case "circuit-breaker.metrics.rolling-window.buckets":
                return changeable ? null : "metrics.rollingStats.numBuckets";
            case "circuit-breaker.metrics.rolling-percentile.enabled":
                return changeable ? null : "metrics.rollingPercentile.enabled";
            case "circuit-breaker.metrics.rolling-percentile.size":
                return changeable ? null : "metrics.rollingPercentile.timeInMilliseconds";
            case "circuit-breaker.metrics.rolling-percentile.buckets":
                return changeable ? null : "metrics.rollingPercentile.numBuckets";
            case "circuit-breaker.metrics.rolling-percentile.bucket-size":
                return changeable ? null : "metrics.rollingPercentile.bucketSize";
            case "circuit-breaker.metrics.health-interval":
                return "metrics.healthSnapshot.intervalInMilliseconds";
            case "circuit-breaker.interrupt.on-timeout":
                return "execution.isolation.thread.interruptOnTimeout";
            case "circuit-breaker.interrupt.on-cancel":
                return "execution.isolation.thread.interruptOnCancel";
            case "circuit-breaker.log.enabled":
                return "requestLog.enabled";
            case "timeout.enabled":
                return changeable ? null : "execution.timeout.enabled";
            case "timeout.value":
                return "execution.isolation.thread.timeoutInMilliseconds";
            case "fallback.enabled":
                return changeable ? null : "fallback.enabled";
            case "fallback.max-requests":
                return "fallback.isolation.semaphore.maxConcurrentRequests";
            default:
                return null;
        }
    }

    private void intializeProperty(String commandKey, String groupKey, FaultToleranceType type, String propertyPath, Object defaultValue) {

        boolean watchEnabled = false;
        boolean configValueFound = false;
        ConfigurationProperty property = new ConfigurationProperty(commandKey, groupKey, type, propertyPath);
        Optional<ConfigurationProperty> appliedProperty = configManager.findKumuluzConfig(commandKey, groupKey, type, propertyPath);

        if (appliedProperty.isPresent()) {
            Optional<Object> configValue = configManager.getKumuluzConfig(appliedProperty.get());

            if (configValue.isPresent()) {
                appliedProperty.get().setValue(configValue.get());

                configValueFound = true;
                watchEnabled = configManager.isWatchEnabled(appliedProperty.get());
            }
        }

        if (watchEnabled) {
            initializeWatchedProperty(property, appliedProperty.get(), defaultValue);
        } else if (defaultValue != null || configValueFound) {
            setHystrixProperty(property, HystrixConfigurationType.COMMAND, commandKey,
                    configValueFound ? appliedProperty.get().getValue() : defaultValue);
        }
    }
}
