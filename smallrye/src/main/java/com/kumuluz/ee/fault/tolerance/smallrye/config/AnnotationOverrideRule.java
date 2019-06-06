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

import java.util.*;

/**
 * Container for a single annotation override rule as defined in configuration.
 *
 * @author Urban Malc
 * @since 2.0.0
 */
public class AnnotationOverrideRule {

    private static final Set<String> VALID_ANNOTATIONS = new HashSet<>(FaultToleranceConfigMapper.FT_ANNOTATION_NAME_TO_HYPHEN_CASE.values());

    private String klass;
    private String method;
    private String annotation;
    private Map<String, String> parameters;

    public AnnotationOverrideRule(String klass, String method, String annotation) {
        this.klass = klass;
        this.method = method;
        this.annotation = annotation;
        this.parameters = new HashMap<>();
    }

    public List<String> validationErrors() {
        List<String> errors = new LinkedList<>();

        if (StringUtils.isNullOrEmpty(klass)) {
            errors.add("The 'class' key must be present and non-empty");
        }
        if (!VALID_ANNOTATIONS.contains(annotation)) {
            errors.add("Annotation " + annotation + " is not a valid Fault Tolerance annotation");
        }

        return errors;
    }

    public void addParameter(String key, String value) {
        parameters.put(key, value);
    }

    public String getMethod() {
        return method;
    }

    public String getAnnotation() {
        return annotation;
    }

    public String getParameter(String parameterName) {
        return parameters.get(parameterName);
    }

    @Override
    public String toString() {
        return "[Class: " + klass + ", Method: " + method + ", Annotation: " + annotation + "]";
    }
}
