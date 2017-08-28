package com.kumuluz.ee.fault.tolerance.configurations;

import com.kumuluz.ee.fault.tolerance.enums.FaultToleranceType;
import com.kumuluz.ee.fault.tolerance.enums.HystrixConfigurationType;
import com.kumuluz.ee.fault.tolerance.exceptions.FaultToleranceConfigException;
import com.kumuluz.ee.fault.tolerance.models.ConfigurationProperty;
import com.kumuluz.ee.fault.tolerance.models.ExecutionMetadata;

import java.util.Optional;

/**
 * Created by luka on 26/08/2017.
 */
public class ThreadPoolConfigurationUtil extends AbstractHystrixConfigurationUtil {

    public ThreadPoolConfigurationUtil(HystrixFaultToleranceConfigurationManager configManager) {
        super(configManager);
    }

    public void initialize(ExecutionMetadata metadata) {

        String key = metadata.getGroupKey();
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
                if (!(defaultValue instanceof Integer) && !(appliedProperty.getValue() instanceof Integer))
                    throw new FaultToleranceConfigException();

                Integer staticValue = (Integer) defaultValue;
                Integer dynamicValue = (Integer) appliedProperty.getValue();

                if (staticValue > dynamicValue)
                    dynamicValue = staticValue;

                setHystrixProperty(property, HystrixConfigurationType.THREAD_POOL, property.getGroupKey(), staticValue);
                setHystrixProperty(property, HystrixConfigurationType.THREAD_POOL, property.getGroupKey(),
                        dynamicValue, true);

                break;
            case "bulkhead.waiting-task-queue":
                setHystrixProperty(property, HystrixConfigurationType.THREAD_POOL, property.getGroupKey(), -1);
                setHystrixProperty(property, HystrixConfigurationType.THREAD_POOL, property.getGroupKey(),
                        appliedProperty.getValue(), true);

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
