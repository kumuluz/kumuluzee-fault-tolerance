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
package com.kumuluz.ee.fault.tolerance.smallrye.config;

import com.kumuluz.ee.configuration.ConfigurationSource;
import com.kumuluz.ee.configuration.utils.ConfigurationDispatcher;
import com.kumuluz.ee.fault.tolerance.smallrye.SmallRyeFtExtension;

import java.util.List;
import java.util.Optional;

/**
 * Sets the key that disables the FT metrics collection if KumuluzEE Metrics extension is not present.
 *
 * @author Urban Malc
 * @since 2.0.0
 */
public class MetricsKeyConfigOverride implements ConfigurationSource {

    @Override
    public void init(ConfigurationDispatcher configurationDispatcher) {
    }

    @Override
    public Optional<String> get(String key) {

        if (!SmallRyeFtExtension.isMetricsEnabled() && FaultToleranceConfigMapper.MP_METRICS_ENABLED_KEY.equals(key)) {
            return Optional.of("false");
        }

        return Optional.empty();
    }

    @Override
    public Optional<Boolean> getBoolean(String key) {

        Optional<String> value = get(key);

        return value.map(Boolean::valueOf);
    }

    /**
     * High priority in order to override other config sources.
     *
     * @return configuration source ordinal
     */
    @Override
    public Integer getOrdinal() {
        return Integer.MAX_VALUE - 10;
    }

    @Override
    public Optional<Integer> getInteger(String s) {
        return Optional.empty();
    }

    @Override
    public Optional<Long> getLong(String s) {
        return Optional.empty();
    }

    @Override
    public Optional<Double> getDouble(String s) {
        return Optional.empty();
    }

    @Override
    public Optional<Float> getFloat(String s) {
        return Optional.empty();
    }

    @Override
    public Optional<Integer> getListSize(String s) {
        return Optional.empty();
    }

    @Override
    public Optional<List<String>> getMapKeys(String s) {
        return Optional.empty();
    }

    @Override
    public void watch(String s) {

    }

    @Override
    public void set(String s, String s1) {

    }

    @Override
    public void set(String s, Boolean aBoolean) {

    }

    @Override
    public void set(String s, Integer integer) {

    }

    @Override
    public void set(String s, Double aDouble) {

    }

    @Override
    public void set(String s, Float aFloat) {

    }
}
