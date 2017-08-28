package com.kumuluz.ee.fault.tolerance.configurations;

import com.kumuluz.ee.fault.tolerance.enums.HystrixConfigurationType;
import com.kumuluz.ee.fault.tolerance.models.ConfigurationProperty;
import com.kumuluz.ee.fault.tolerance.models.ExecutionMetadata;
import com.netflix.hystrix.HystrixCommandProperties;

import java.time.Duration;

/**
 * Created by luka on 26/08/2017.
 */
public abstract class AbstractHystrixConfigurationUtil {

    protected HystrixFaultToleranceConfigurationManager configManager;

    public AbstractHystrixConfigurationUtil(HystrixFaultToleranceConfigurationManager configManager) {
        this.configManager = configManager;
    }

    public abstract void initialize(ExecutionMetadata metadata);

    public abstract void updateProperty(ConfigurationProperty property, Object value);

    protected abstract String toHystrixPropertyPath(ConfigurationProperty property, boolean changeable);

    protected abstract void initializeWatchedProperty(ConfigurationProperty property, ConfigurationProperty appliedProperty, Object defaultValue);

    public void setHystrixProperty(ConfigurationProperty property, HystrixConfigurationType type, String key, Object value) {
        setHystrixProperty(property, type, key, value, false);
    }

    public void setHystrixProperty(ConfigurationProperty property, HystrixConfigurationType type, String key, Object value, boolean changeable) {

        String hystrixProperyPath = toHystrixPropertyPath(property, changeable);
        Object hystrixValue = convertToHystrixValue(property, value);

        if (hystrixProperyPath != null)
            configManager.setHystrixConfig(type, key, hystrixProperyPath, hystrixValue);
    }

    public boolean isHystrixPropertyChangeable(ConfigurationProperty property) {
        return toHystrixPropertyPath(property, true) != null;
    }

    public Object convertToHystrixValue(ConfigurationProperty property, Object value) {

        switch (property.typeConfigurationPath()) {
            case "circuit-breaker.failure-ratio":
                if (value instanceof Double)
                    return (int) (((Double) value) * 100);
                break;
            case "bulkhead.metrics.rolling-window.size":
            case "circuit-breaker.delay":
            case "circuit-breaker.metrics.rolling-window.size":
            case "circuit-breaker.metrics.health-interval":
            case "timeout.value":
                if (value instanceof Duration)
                    return ((Duration) value).toMillis();
                break;
            case "bulkhead.keep-alive":
                if (value instanceof Duration)
                    return ((Duration) value).toMinutes();
                break;
            case "asynchronous.value":
                if (value instanceof Boolean)
                    return ((Boolean) value) ?
                            HystrixCommandProperties.ExecutionIsolationStrategy.THREAD :
                            HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE;
                break;
            default:
                break;
        }

        return value;
    }
}
