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
package com.kumuluz.ee.fault.tolerance.configurations.hystrix;

import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import com.kumuluz.ee.fault.tolerance.enums.FaultToleranceType;
import com.kumuluz.ee.fault.tolerance.enums.HystrixConfigurationType;
import com.kumuluz.ee.fault.tolerance.interfaces.FaultToleranceUtil;
import com.kumuluz.ee.fault.tolerance.models.ConfigurationProperty;
import com.kumuluz.ee.fault.tolerance.utils.FaultToleranceHelper;
import org.apache.commons.configuration.AbstractConfiguration;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Duration;
import java.util.*;
import java.util.logging.Logger;

/**
 * Configuration manager for setting Hystrix settings from KumuluzEE Fault Tolerance and
 * KumuluzEE Config frameworks
 *
 * @author Luka Šarc
 * @since 1.0.0
 */
@ApplicationScoped
public class HystrixFaultToleranceConfigurationManager {

    private static final Logger log = Logger.getLogger(HystrixFaultToleranceConfigurationManager.class.getName());

    private ConfigurationUtil kumuluzConfig;
    private AbstractConfiguration hystrixConfig;

    private Map<String, List<ConfigurationProperty>> commandWatchToUpdateMap;
    private Map<String, List<ConfigurationProperty>> threadPoolWatchToUpdateMap;

    @Inject
    private FaultToleranceUtil faultToleranceUtil;

    @PostConstruct
    private void init() {

        kumuluzConfig = ConfigurationUtil.getInstance();
        hystrixConfig = com.netflix.config.ConfigurationManager.getConfigInstance();

        commandWatchToUpdateMap = new HashMap<>();
        threadPoolWatchToUpdateMap = new HashMap<>();
    }

    public void setHystrixConfig(HystrixConfigurationType type, String key, String propertyPath, Object value) {

        String keyPath = toHystrixConfigKeyPath(type, key, propertyPath);
        setHystrixConfig(keyPath, value);
    }

    public void setHystrixConfig(String key, Object value) {
        hystrixConfig.setProperty(key, value);
    }

    public Object getHystrixConfig(String key, Object value) {
        return hystrixConfig.getProperty(key);
    }

    public String toHystrixConfigKeyPath(HystrixConfigurationType type, String key, String propertyPath) {
        return String.format("hystrix.%s.%s.%s", type.getConfigKey(), key, propertyPath);
    }

    public Optional<Object> getKumuluzConfig(ConfigurationProperty property) {

        switch (property.typeConfigurationPath()) {
            case "bulkhead.metrics.rolling-window.size":
            case "bulkhead.keep-alive":
            case "circuit-breaker.delay":
            case "circuit-breaker.metrics.rolling-window.size":
            case "circuit-breaker.metrics.health-interval":
            case "timeout.value":
                Optional<Duration> durationVal = getKumuluzConfigDuration(property.configurationPath());
                return durationVal.map(Duration::toMillis);
            case "circuit-breaker.failure-ratio":
                Optional<Double> doubleVal = getKumuluzConfigDouble(property.configurationPath());
                return doubleVal.isPresent() ? Optional.of(doubleVal.get()) : Optional.empty();
            case "bulkhead.value-change-enabled":
            case "asynchronous.value":
            case "circuit-breaker.enabled":
            case "circuit-breaker.metrics.rolling-percentile.enabled":
            case "circuit-breaker.interrupt.on-timeout":
            case "circuit-breaker.interrupt.on-cancel":
            case "circuit-breaker.log.enabled":
            case "timeout.enabled":
            case "fallback.enabled":
                Optional<Boolean> boolVal = getKumuluzConfigBoolean(property.configurationPath());
                return boolVal.isPresent() ? Optional.of(boolVal.get()) : Optional.empty();
            default:
                Optional<Integer> intVal = getKumuluzConfigInteger(property.configurationPath());
                return intVal.isPresent() ? Optional.of(intVal.get()) : Optional.empty();
        }
    }

    public Optional<String> getKumuluzConfigString(String keyPath) {
        return kumuluzConfig.get(keyPath);
    }

    public Optional<Integer> getKumuluzConfigInteger(String keyPath) {
        return kumuluzConfig.getInteger(keyPath);
    }

    public Optional<Double> getKumuluzConfigDouble(String keyPath) {
        return kumuluzConfig.getDouble(keyPath);
    }

    public Optional<Boolean> getKumuluzConfigBoolean(String keyPath) {
        return kumuluzConfig.getBoolean(keyPath);
    }

    public Optional<Duration> getKumuluzConfigDuration(String keyPath) {

        Optional<String> value = getKumuluzConfigString(keyPath);

        if (value.isPresent()) {
            Duration duration = FaultToleranceHelper.parseDuration(value.get());
            return Optional.of(duration);
        } else {
            return Optional.empty();
        }
    }

    public void intializeWatch(HystrixConfigurationType type, ConfigurationProperty watchProperty, ConfigurationProperty destProperty) {

        String configPath = watchProperty.configurationPath();
        String newPropertyKeyPath = destProperty.configurationPath();
        Map<String, List<ConfigurationProperty>> map = type == HystrixConfigurationType.COMMAND ?
                commandWatchToUpdateMap :
                threadPoolWatchToUpdateMap;

        if (map.containsKey(configPath)) {
            List<ConfigurationProperty> properties = map.get(configPath);

            if (properties.stream().noneMatch(p -> p.configurationPath().equals(newPropertyKeyPath))) {
                log.finest("Adding key path '" + newPropertyKeyPath + "' to key '" + configPath + "' in map.");

                properties.add(destProperty);
            }
        } else {
            log.info("Initializing config watch for key path '" + configPath + "'.");

            List<ConfigurationProperty> properties = new ArrayList<>();
            properties.add(destProperty);

            log.finest("Adding key path '" + newPropertyKeyPath + "' to key '" + configPath + "' in map.");

            map.put(configPath, properties);

            faultToleranceUtil.watch(watchProperty);
        }
    }

    public void updateProperty(ConfigurationProperty property) {

        String configPath = property.configurationPath();

        log.info("Received update for key path '" + configPath + "'.");

        List<ConfigurationProperty> toUpdate;
        AbstractHystrixConfigurationUtil hystrixConfigurationUtil;

        if (commandWatchToUpdateMap.containsKey(configPath)) {
            toUpdate = commandWatchToUpdateMap.get(configPath);
            hystrixConfigurationUtil = new CommandHystrixConfigurationUtil(this);
        } else if (threadPoolWatchToUpdateMap.containsKey(configPath)) {
            toUpdate = threadPoolWatchToUpdateMap.get(configPath);
            hystrixConfigurationUtil = new ThreadPoolHystrixConfigurationUtil(this);
        } else {
            return;
        }

        toUpdate.forEach(p -> {
            log.info("Updating configuration key '" + p.configurationPath() + "' with value '" + property.getValue() + "'.");

            hystrixConfigurationUtil.updateProperty(p, property.getValue());
        });
    }

    public boolean isWatchEnabled(ConfigurationProperty property) {
        return faultToleranceUtil.isWatchEnabled(property);
    }

    public Optional<ConfigurationProperty> findKumuluzConfig(String groupKey, FaultToleranceType type, String propertyPath) {
        return faultToleranceUtil.findConfig(groupKey, type, propertyPath);
    }

    public Optional<ConfigurationProperty> findKumuluzConfig(String commandKey, String groupKey, FaultToleranceType type, String propertyPath) {
        return faultToleranceUtil.findConfig(commandKey, groupKey, type, propertyPath);
    }
}
