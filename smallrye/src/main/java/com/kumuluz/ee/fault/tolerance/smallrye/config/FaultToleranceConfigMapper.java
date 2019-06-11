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

import com.kumuluz.ee.common.utils.StringUtils;
import com.kumuluz.ee.configuration.ConfigurationSource;
import com.kumuluz.ee.configuration.utils.ConfigurationDispatcher;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import org.eclipse.microprofile.faulttolerance.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Maps MP Fault Tolerance keys to KumuluzEE Fault Tolerance keys.
 *
 * @author Urban Malc
 * @since 2.0.0
 */
public class FaultToleranceConfigMapper implements ConfigurationSource {

    private ConfigurationUtil configurationUtil;
    private AnnotationOverrides annotationOverrides;

    private static final String MP_NON_FALLBACK_ENABLED_KEY = "MP_Fault_Tolerance_NonFallback_Enabled";
    private static final String KUMULUZ_NON_FALLBACK_ENABLED_KEY = "kumuluzee.fault-tolerance.non-fallback-enabled";

    static final String MP_METRICS_ENABLED_KEY = "MP_Fault_Tolerance_Metrics_Enabled";
    private static final String KUMULUZ_METRICS_ENABLED_KEY = "kumuluzee.fault-tolerance.metrics-enabled";

    private static final Class[] FAULT_TOLERANCE_ANNOTATIONS = {
            Asynchronous.class,
            Timeout.class,
            Retry.class,
            Fallback.class,
            CircuitBreaker.class,
            Bulkhead.class,
    };

    static final Map<String, String> FT_ANNOTATION_NAME_TO_HYPHEN_CASE;

    static {
        FT_ANNOTATION_NAME_TO_HYPHEN_CASE = new HashMap<>();

        Arrays.stream(FAULT_TOLERANCE_ANNOTATIONS)
                .map(Class::getSimpleName)
                .forEach(an -> FT_ANNOTATION_NAME_TO_HYPHEN_CASE.put(an, StringUtils.camelCaseToHyphenCase(an)));
    }

    // matches all of the following patterns:
    // ?/?/<ft-annotation>/?
    // ?/<ft-annotation>/?
    // <ft-annotation>/?
    private static final Pattern KEY_TRANSLATION_PATTERN = Pattern.compile("^([^/]+/)?([^/]+/)?(" +
            Arrays.stream(FAULT_TOLERANCE_ANNOTATIONS)
                    .map(Class::getSimpleName)
                    .collect(Collectors.joining("|")) +
            ")/([^/]+)$");

    @Override
    public void init(ConfigurationDispatcher configurationDispatcher) {
        this.configurationUtil = ConfigurationUtil.getInstance();
        this.annotationOverrides = new AnnotationOverrides();
    }

    @Override
    public Optional<String> get(String key) {

        if (MP_NON_FALLBACK_ENABLED_KEY.equals(key)) {
            return configurationUtil.get(KUMULUZ_NON_FALLBACK_ENABLED_KEY);
        }

        if (MP_METRICS_ENABLED_KEY.equals(key)) {
            return configurationUtil.get(KUMULUZ_METRICS_ENABLED_KEY);
        }

        Matcher matcher = KEY_TRANSLATION_PATTERN.matcher(key);
        if (matcher.find()) {

            // key matches ft patterns, extract parameters from the key
            String klass = matcher.group(1);
            if (klass != null) {
                klass = klass.substring(0, klass.length() - 1); // remove trailing slash
            }
            String method = matcher.group(2);
            if (method != null) {
                method = method.substring(0, method.length() - 1); // remove trailing slash
            }
            String annotation = matcher.group(3);
            annotation = FT_ANNOTATION_NAME_TO_HYPHEN_CASE.get(annotation);
            String parameterName = matcher.group(4);
            if (parameterName != null) {
                parameterName = StringUtils.camelCaseToHyphenCase(parameterName);
            }

            if (klass == null) {
                // class & method = null, key is for global configuration
                return annotationOverrides.getGlobalParameter(annotation, parameterName);
            }

            // class is defined, key is for annotation override
            return annotationOverrides.getAnnotationOverrideParameter(klass, method, annotation, parameterName);
        }

        return Optional.empty();
    }

    /**
     * Low priority so keys defined in specification still take precedence.
     *
     * @return configuration source ordinal
     */
    @Override
    public Integer getOrdinal() {
        return 10;
    }

    @Override
    public Optional<Boolean> getBoolean(String key) {

        Optional<String> value = get(key);

        return value.map(Boolean::valueOf);
    }

    @Override
    public Optional<Integer> getInteger(String key) {

        Optional<String> value = get(key);

        if (value.isPresent()) {

            try {
                return Optional.of(Integer.valueOf(value.get()));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Long> getLong(String key) {

        Optional<String> value = get(key);

        if (value.isPresent()) {

            try {
                return Optional.of(Long.valueOf(value.get()));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Double> getDouble(String key) {

        Optional<String> value = get(key);

        if (value.isPresent()) {

            try {
                return Optional.of(Double.valueOf(value.get()));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Float> getFloat(String key) {

        Optional<String> value = get(key);

        if (value.isPresent()) {

            try {
                return Optional.of(Float.valueOf(value.get()));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }

        } else {
            return Optional.empty();
        }
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
