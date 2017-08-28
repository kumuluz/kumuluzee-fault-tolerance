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

import com.kumuluz.ee.fault.tolerance.enums.HystrixConfigurationType;
import com.kumuluz.ee.fault.tolerance.models.ConfigurationProperty;
import com.kumuluz.ee.fault.tolerance.models.ExecutionMetadata;
import com.netflix.hystrix.HystrixCommandProperties;

import java.time.Duration;

/**
 * Abstract Hystrix configuration util
 *
 * @author Luka Šarc
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
