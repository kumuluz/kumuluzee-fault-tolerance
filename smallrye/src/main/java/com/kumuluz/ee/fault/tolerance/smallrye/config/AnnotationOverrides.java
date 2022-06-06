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

import com.kumuluz.ee.configuration.utils.ConfigurationUtil;

import java.util.*;
import java.util.logging.Logger;

/**
 * Stores all the KumuluzEE specific configuration for Fault Tolerance. Used for mapping keys from MP Fault Tolerance
 * keys to KumuluzEE specific ones.
 *
 * @author Urban Malc
 * @since 2.0.0
 */
public class AnnotationOverrides {

    private static final Logger LOG = Logger.getLogger(AnnotationOverrides.class.getName());

    private final ConfigurationUtil configUtil;

    private Map<String, Map<String, String>> globalAnnotationOverrides;
    private Map<String, List<AnnotationOverrideRule>> annotationRules;

    public AnnotationOverrides() {
        this.configUtil = ConfigurationUtil.getInstance();

        initGlobalAnnotationOverrides();
        initAnnotationRules();
    }

    private void initGlobalAnnotationOverrides() {
        globalAnnotationOverrides = new HashMap<>();

        for (String annotationName : FaultToleranceConfigMapper.FT_ANNOTATION_NAME_TO_HYPHEN_CASE.values()) {
            globalAnnotationOverrides.put(annotationName, new HashMap<>());
            configUtil.getMapKeys("kumuluzee.fault-tolerance." + annotationName)
                    .ifPresent(l -> l.forEach(
                            paramName -> configUtil.get("kumuluzee.fault-tolerance." + annotationName + "." + paramName)
                                    .ifPresent(
                                            paramValue -> globalAnnotationOverrides.get(annotationName)
                                                    .put(paramName, paramValue)
                                    )
                            )
                    );
        }
    }

    private void initAnnotationRules() {
        annotationRules = new HashMap<>();

        int rulesListLen = configUtil.getListSize("kumuluzee.fault-tolerance.annotation-overrides").orElse(-1);

        if (rulesListLen <= 0) {
            return;
        }

        for (int i = 0; i < rulesListLen; i++) {

            String prefix = "kumuluzee.fault-tolerance.annotation-overrides[" + i + "].";

            String klass = configUtil.get(prefix + "class").orElse(null);
            String method = configUtil.get(prefix + "method").orElse(null);
            String annotation = configUtil.get(prefix + "annotation").orElse(null);

            AnnotationOverrideRule rule = new AnnotationOverrideRule(klass, method, annotation);

            List<String> validationErrors = rule.validationErrors(); // validate and collect errors if present
            if (validationErrors.size() > 0) {
                LOG.warning("Rule " + rule + " is invalid. Reason: " +
                        String.join("; ", validationErrors));
                continue;
            }

            // rule is valid, extract parameters

            configUtil.getMapKeys(prefix + "parameters").ifPresent(
                    keys -> keys.forEach(
                            key -> configUtil.get(prefix + "parameters." + key).ifPresent(
                                    value -> rule.addParameter(key, value)
                            )
                    )
            );

            if (!annotationRules.containsKey(klass)) {
                annotationRules.put(klass, new LinkedList<>());
            }
            annotationRules.get(klass).add(rule);
        }
    }

    public Optional<String> getGlobalParameter(String annotation, String parameterName) {
        if (!globalAnnotationOverrides.containsKey(annotation)) {
            return Optional.empty();
        }

        return Optional.ofNullable(globalAnnotationOverrides.get(annotation).get(parameterName));
    }

    public Optional<String> getAnnotationOverrideParameter(String klass, String method,
                                                           String annotation, String parameterName) {
        if (!annotationRules.containsKey(klass)) {
            return Optional.empty();
        }

        for (AnnotationOverrideRule rule : annotationRules.get(klass)) {
            // rule matches if annotations are equal and methods are undefined on both sides (class override) or equal
            // on both sides (method override)
            if (rule.getAnnotation().equals(annotation) &&
                    (method == null && rule.getMethod() == null || method != null && method.equals(rule.getMethod()))) {
                // return parameter if present
                return Optional.ofNullable(rule.getParameter(parameterName));
            }
        }

        return Optional.empty();
    }
}
