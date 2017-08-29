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

import com.kumuluz.ee.fault.tolerance.enums.FaultToleranceType;
import com.kumuluz.ee.fault.tolerance.enums.HystrixConfigurationType;
import com.kumuluz.ee.fault.tolerance.exceptions.FaultToleranceConfigException;
import com.kumuluz.ee.fault.tolerance.models.ConfigurationProperty;
import com.kumuluz.ee.fault.tolerance.models.ExecutionMetadata;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * Hystrix thread pool configuration util
 *
 * @author Luka Šarc
 */
public class ThreadPoolConfigurationUtil extends AbstractHystrixConfigurationUtil {

    private static final Logger log = Logger.getLogger(ThreadPoolConfigurationUtil.class.getName());

    public ThreadPoolConfigurationUtil(HystrixFaultToleranceConfigurationManager configManager) {
        super(configManager);
    }

    public void initialize(ExecutionMetadata metadata) {

        String key = metadata.getGroupKey();

        log.info("Initializing thread bulkhead pattern for group '" + key + "'.");

        intializeProperty(key, FaultToleranceType.BULKHEAD, "value", metadata.getBulkhead().value());
        intializeProperty(key, FaultToleranceType.BULKHEAD, "waiting-task-queue", metadata.getBulkhead().waitingTaskQueue());
        intializeProperty(key, FaultToleranceType.BULKHEAD, "metrics.rolling-window.size", null);
        intializeProperty(key, FaultToleranceType.BULKHEAD, "metrics.rolling-window.buckets", null);
        intializeProperty(key, FaultToleranceType.BULKHEAD, "keep-alive", null);
    }

    public void updateProperty(ConfigurationProperty property, Object value) {
        setHystrixProperty(property, HystrixConfigurationType.THREAD_POOL, property.getGroupKey(),
                value, true);
    }

    protected void initializeWatchedProperty(ConfigurationProperty property, ConfigurationProperty appliedProperty, Object defaultValue) {

        boolean isChangeable = isHystrixPropertyChangeable(property);

        switch (property.typeConfigurationPath()) {
            case "bulkhead.value":
                if (!(defaultValue instanceof Integer) || !(appliedProperty.getValue() instanceof Integer)) {
                    throw new FaultToleranceConfigException("One of provided values for watched bulkhead " +
                            "is not of type Integer: " + defaultValue + ", " + appliedProperty.getValue());
                }

                Integer bulkheadStaticValue = (Integer) defaultValue;
                Integer bulkheadDynamicalue = (Integer) appliedProperty.getValue();

                if (bulkheadStaticValue > bulkheadDynamicalue)
                    bulkheadDynamicalue = bulkheadStaticValue;

                ConfigurationProperty allowDivergedSizesProperty = new ConfigurationProperty(property.getGroupKey(),
                        FaultToleranceType.BULKHEAD, "value-change-enabled");

                setHystrixProperty(allowDivergedSizesProperty, HystrixConfigurationType.THREAD_POOL, property.getGroupKey(),
                        true);
                setHystrixProperty(property, HystrixConfigurationType.THREAD_POOL, property.getGroupKey(),
                        bulkheadStaticValue);
                setHystrixProperty(property, HystrixConfigurationType.THREAD_POOL, property.getGroupKey(),
                        bulkheadDynamicalue, true);

                break;
            case "bulkhead.waiting-task-queue":
                if (!(defaultValue instanceof Integer) || !(appliedProperty.getValue() instanceof Integer)) {
                    throw new FaultToleranceConfigException("One of provided values for watched bulkhead " +
                            "waiting task queue is not of type Integer: " + defaultValue + ", " +
                            appliedProperty.getValue());
                }

                Integer queueStaticValue = (Integer) defaultValue;
                Integer queueDynamicValue = (Integer) appliedProperty.getValue();

                if (queueStaticValue < queueDynamicValue)
                    queueDynamicValue = queueStaticValue;

                setHystrixProperty(property, HystrixConfigurationType.THREAD_POOL, property.getGroupKey(),
                        queueStaticValue);
                setHystrixProperty(property, HystrixConfigurationType.THREAD_POOL, property.getGroupKey(),
                        queueDynamicValue, true);

                break;
            default:
                setHystrixProperty(property, HystrixConfigurationType.THREAD_POOL, property.getGroupKey(),
                        appliedProperty.getValue(), isChangeable);
                break;
        }

        if (isChangeable) {
            configManager.intializeWatch(HystrixConfigurationType.THREAD_POOL, appliedProperty,
                    property);
        }
    }

    protected String toHystrixPropertyPath(ConfigurationProperty property, boolean changeable) {

        switch (property.typeConfigurationPath()) {
            case "bulkhead.value":
                return changeable ? "maximumSize" : "coreSize";
            case "bulkhead.waiting-task-queue":
                return changeable ? "queueSizeRejectionThreshold" : "maxQueueSize";
            case "bulkhead.value-change-enabled":
                return changeable ? null : "allowMaximumSizeToDivergeFromCoreSize";
            case "bulkhead.metrics.rolling-window.size":
                return changeable ? null : "metrics.rollingStats.timeInMilliseconds";
            case "bulkhead.metrics.rolling-window.buckets":
                return changeable ? null : "metrics.rollingStats.numBuckets";
            case "bulkhead.keep-alive":
                return "keepAliveTimeMinutes";
            default:
                return null;
        }
    }

    private void intializeProperty(String groupKey, FaultToleranceType type, String propertyPath, Object defaultValue) {

        boolean watchEnabled = false;
        boolean configValueFound = false;
        ConfigurationProperty property = new ConfigurationProperty(groupKey, type, propertyPath);
        Optional<ConfigurationProperty> appliedProperty = configManager.findKumuluzConfig(groupKey, type, propertyPath);

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
            setHystrixProperty(property, HystrixConfigurationType.THREAD_POOL, groupKey,
                    configValueFound ? appliedProperty.get().getValue() : defaultValue);
        }
    }
}
