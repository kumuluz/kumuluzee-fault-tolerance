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
package com.kumuluz.ee.fault.tolerance.configurations.retry;

import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import com.kumuluz.ee.fault.tolerance.enums.FaultToleranceType;
import com.kumuluz.ee.fault.tolerance.interfaces.FaultToleranceUtil;
import com.kumuluz.ee.fault.tolerance.models.ConfigurationProperty;
import com.kumuluz.ee.fault.tolerance.models.ExecutionMetadata;
import com.kumuluz.ee.fault.tolerance.utils.FaultToleranceHelper;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Duration;
import java.util.*;
import java.util.logging.Logger;

/**
 * Configuration manager for managing retry configurations that are not
 * part of Hystrix framework
 *
 * @author Luka Šarc
 * @since 1.0.0
 */
@ApplicationScoped
public class RetryConfigurationManager {

    private static final Logger log = Logger.getLogger(RetryConfigurationManager.class.getName());

    private ConfigurationUtil config;

    private Map<String, RetryConfig> retryConfigs;
    private Map<String, List<String>> retryWatches;

    @Inject
    private FaultToleranceUtil faultToleranceUtil;

    @PostConstruct
    private void init() {
        config = ConfigurationUtil.getInstance();

        retryConfigs = new HashMap<>();
        retryWatches = new HashMap<>();
    }

    public void initializeRetry(ExecutionMetadata metadata) {

        if (metadata.getRetry() == null)
            return;

        RetryConfig retryConfig = new RetryConfig(metadata.getRetry().retryOn(),
                metadata.getRetry().abortOn());

        Optional<ConfigurationProperty> maxRetriesProperty = faultToleranceUtil.findConfig(metadata.getCommandKey(),
                metadata.getGroupKey(), FaultToleranceType.RETRY, "max-retries");

        if (maxRetriesProperty.isPresent()) {
            retryConfig.setMaxRetries(config.getInteger(maxRetriesProperty.get()
                    .configurationPath()).get());

            if (faultToleranceUtil.isWatchEnabled(maxRetriesProperty.get())) {
                initializeWatch(maxRetriesProperty.get(), metadata.getCommandKey());
            }
        } else {
            retryConfig.setMaxRetries(metadata.getRetry().maxRetries());
        }

        Optional<ConfigurationProperty> delayProperty = faultToleranceUtil.findConfig(metadata.getCommandKey(),
                metadata.getGroupKey(), FaultToleranceType.RETRY, "delay");

        if (delayProperty.isPresent()) {
            Duration duration = FaultToleranceHelper.parseDuration(config.get(
                    delayProperty.get().configurationPath()).get());

            retryConfig.setDelayInMillis(duration.toMillis());

            if (faultToleranceUtil.isWatchEnabled(delayProperty.get())) {
                initializeWatch(delayProperty.get(), metadata.getCommandKey());
            }
        } else {
            retryConfig.setDelayInMillis(Duration.of(metadata.getRetry().delay(),
                    metadata.getRetry().delayUnit()).toMillis());
        }

        Optional<ConfigurationProperty> jitterProperty = faultToleranceUtil.findConfig(metadata.getCommandKey(),
                metadata.getGroupKey(), FaultToleranceType.RETRY, "jitter");

        if (jitterProperty.isPresent()) {
            Duration duration = FaultToleranceHelper.parseDuration(config.get(
                    jitterProperty.get().configurationPath()).get());

            retryConfig.setJitterInMillis(duration.toMillis());

            if (faultToleranceUtil.isWatchEnabled(jitterProperty.get())) {
                initializeWatch(jitterProperty.get(), metadata.getCommandKey());
            }
        } else {
            retryConfig.setJitterInMillis(Duration.of(metadata.getRetry().jitter(),
                    metadata.getRetry().jitterDelayUnit()).toMillis());
        }

        retryConfigs.put(metadata.getIdentifier(), retryConfig);
    }

    public void setRetryConfig(ConfigurationProperty property, Object value) {

        if (property.getType() != FaultToleranceType.RETRY ||
                !retryConfigs.containsKey(property.getIdentifier()))
            return;

        RetryConfig retryConfig = retryConfigs.get(property.getIdentifier());

        switch (property.getPropertyPath()) {
            case "max-retries":
                if (value instanceof Integer)
                    retryConfig.setMaxRetries((int) value);

                break;
            case "delay":
                if (value instanceof Duration)
                    retryConfig.setDelayInMillis(((Duration) value).toMillis());

                break;
            case "jitter":
                if (value instanceof Duration)
                    retryConfig.setJitterInMillis(((Duration) value).toMillis());

                break;
            default:
                break;
        }
    }

    public RetryConfig getRetryConfig(String key) {
        return retryConfigs.get(key);
    }

    public void initializeWatch(ConfigurationProperty property, String newWatchCommandKey) {

        String configPath = property.configurationPath();

        if (retryWatches.containsKey(configPath)) {
            List<String> commandKeys = retryWatches.get(configPath);

            if (commandKeys.stream().noneMatch(ck -> ck.equals(newWatchCommandKey))) {
                log.finest("Adding command key '" + newWatchCommandKey + "' to key '" + configPath + "' in map.");

                commandKeys.add(newWatchCommandKey);
            }
        } else {
            log.info("Initializing config watch for key path '" + configPath + "'.");

            List<String> commandKeys = new ArrayList<>();
            commandKeys.add(newWatchCommandKey);

            log.finest("Adding key path '" + newWatchCommandKey + "' to key '" + configPath + "' in map.");

            retryWatches.put(configPath, commandKeys);

            faultToleranceUtil.watch(property);
        }
    }

    public void updateProperty(ConfigurationProperty property) {

        String configPath = property.configurationPath();

        log.info("Received update for key path '" + configPath + "'.");

        if (retryWatches.containsKey(configPath)) {
            retryWatches.get(configPath).forEach(ck -> {
                log.info("Updating configuration key '" + ck + "' with value '" + property.getValue() + "'.");

                ConfigurationProperty commandProperty = new ConfigurationProperty(ck, null, property.getType(),
                        property.getPropertyPath());

                setRetryConfig(commandProperty, property.getValue());
            });
        }
    }
}
