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
package com.kumuluz.ee.fault.tolerance.config;

import com.kumuluz.ee.fault.tolerance.interfaces.ConfigWrapper;
import org.eclipse.microprofile.faulttolerance.*;

import javax.enterprise.context.ApplicationScoped;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Utility for Fault Tolerance configuration with Microprofile config.
 *
 * @author Urban Malc
 * @since 1.1.0
 */
@ApplicationScoped
public class MicroprofileConfigUtil {

    private ConfigWrapper config;
    private boolean nonFallbackEnabled;

    public MicroprofileConfigUtil() {
        try {
            Class.forName("org.eclipse.microprofile.config.Config");
            config = new MicroprofileConfig();
        } catch (ClassNotFoundException e) {
            // mp config not found, using no-op config
            config = new NoopConfig();
        }

        nonFallbackEnabled = config.getOptionalValue("MP_Fault_Tolerance_NonFallback_Enabled", Boolean.class)
                .orElse(true);
    }

    public ConfigWrapper getConfig() {
        return config;
    }

    public Bulkhead configOverriddenBulkhead(Class clazz, Method method, Bulkhead annotation) {

        if (annotation == null || !isAnnotationEnabled(clazz, method, Bulkhead.class)) {
            return null;
        }

        int value = getConfigProperty(clazz, method, Bulkhead.class, "value", Integer.class).orElse(annotation.value());
        int waitingTaskQueue = getConfigProperty(clazz, method, Bulkhead.class, "waitingTaskQueue", Integer.class).orElse(annotation.waitingTaskQueue());

        return new Bulkhead() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return Bulkhead.class;
            }

            @Override
            public int value() {
                return value;
            }

            @Override
            public int waitingTaskQueue() {
                return waitingTaskQueue;
            }
        };
    }

    public CircuitBreaker configOverriddenCircuitBreaker(Class clazz, Method method, CircuitBreaker annotation) {

        if (annotation == null || !isAnnotationEnabled(clazz, method, CircuitBreaker.class)) {
            return null;
        }

        Class<? extends Throwable>[] failOn = getConfigProperty(clazz, method, CircuitBreaker.class, "failOn", Class[].class).orElse(annotation.failOn());
        long delay = getConfigProperty(clazz, method, CircuitBreaker.class, "delay", Long.class).orElse(annotation.delay());
        ChronoUnit delayUnit = getConfigProperty(clazz, method, CircuitBreaker.class, "delayUnit", ChronoUnit.class).orElse(annotation.delayUnit());
        int requestVolumeThreshold = getConfigProperty(clazz, method, CircuitBreaker.class, "requestVolumeThreshold", Integer.class).orElse(annotation.requestVolumeThreshold());
        double failureRatio = getConfigProperty(clazz, method, CircuitBreaker.class, "failureRatio", Double.class).orElse(annotation.failureRatio());
        int successThreshold = getConfigProperty(clazz, method, CircuitBreaker.class, "successThreshold", Integer.class).orElse(annotation.successThreshold());

        return new CircuitBreaker() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return CircuitBreaker.class;
            }

            @Override
            public Class<? extends Throwable>[] failOn() {
                return failOn;
            }

            @Override
            public long delay() {
                return delay;
            }

            @Override
            public ChronoUnit delayUnit() {
                return delayUnit;
            }

            @Override
            public int requestVolumeThreshold() {
                return requestVolumeThreshold;
            }

            @Override
            public double failureRatio() {
                return failureRatio;
            }

            @Override
            public int successThreshold() {
                return successThreshold;
            }
        };
    }

    public Fallback configOverriddenFallback(Class clazz, Method method, Fallback annotation) {

        if (annotation == null || !isAnnotationEnabled(clazz, method, Fallback.class)) {
            return null;
        }

        Class<? extends FallbackHandler<?>> value = getConfigProperty(clazz, method, Fallback.class, "value", Class.class).orElse(annotation.value());
        String fallbackMethod = getConfigProperty(clazz, method, Fallback.class, "fallbackMethod", String.class).orElse(annotation.fallbackMethod());

        return new Fallback() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return Fallback.class;
            }

            @Override
            public Class<? extends FallbackHandler<?>> value() {
                return value;
            }

            @Override
            public String fallbackMethod() {
                return fallbackMethod;
            }
        };
    }

    public Retry configOverriddenRetry(Class clazz, Method method, Retry annotation) {

        if (annotation == null || !isAnnotationEnabled(clazz, method, Retry.class)) {
            return null;
        }

        int maxRetries = getConfigProperty(clazz, method, Retry.class, "maxRetries", Integer.class).orElse(annotation.maxRetries());
        long delay = getConfigProperty(clazz, method, Retry.class, "delay", Long.class).orElse(annotation.delay());
        ChronoUnit delayUnit = getConfigProperty(clazz, method, Retry.class, "delayUnit", ChronoUnit.class).orElse(annotation.delayUnit());
        long maxDuration = getConfigProperty(clazz, method, Retry.class, "maxDuration", Long.class).orElse(annotation.maxDuration());
        ChronoUnit durationUnit = getConfigProperty(clazz, method, Retry.class, "durationUnit", ChronoUnit.class).orElse(annotation.durationUnit());
        long jitter = getConfigProperty(clazz, method, Retry.class, "jitter", Long.class).orElse(annotation.jitter());
        ChronoUnit jitterDelayUnit = getConfigProperty(clazz, method, Retry.class, "jitterDelayUnit", ChronoUnit.class).orElse(annotation.jitterDelayUnit());
        Class<? extends Throwable>[] retryOn = getConfigProperty(clazz, method, Retry.class, "retryOn", Class[].class).orElse(annotation.retryOn());
        Class<? extends Throwable>[] abortOn = getConfigProperty(clazz, method, Retry.class, "abortOn", Class[].class).orElse(annotation.abortOn());

        return new Retry() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return Retry.class;
            }

            @Override
            public int maxRetries() {
                return maxRetries;
            }

            @Override
            public long delay() {
                return delay;
            }

            @Override
            public ChronoUnit delayUnit() {
                return delayUnit;
            }

            @Override
            public long maxDuration() {
                return maxDuration;
            }

            @Override
            public ChronoUnit durationUnit() {
                return durationUnit;
            }

            @Override
            public long jitter() {
                return jitter;
            }

            @Override
            public ChronoUnit jitterDelayUnit() {
                return jitterDelayUnit;
            }

            @Override
            public Class<? extends Throwable>[] retryOn() {
                return retryOn;
            }

            @Override
            public Class<? extends Throwable>[] abortOn() {
                return abortOn;
            }
        };
    }

    public Timeout configOverriddenTimeout(Class clazz, Method method, Timeout annotation) {

        if (annotation == null || !isAnnotationEnabled(clazz, method, Timeout.class)) {
            return null;
        }

        long value = getConfigProperty(clazz, method, Timeout.class, "value", Long.class).orElse(annotation.value());
        ChronoUnit unit = getConfigProperty(clazz, method, Timeout.class, "unit", ChronoUnit.class).orElse(annotation.unit());

        return new Timeout() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return Timeout.class;
            }

            @Override
            public long value() {
                return value;
            }

            @Override
            public ChronoUnit unit() {
                return unit;
            }
        };
    }

    public boolean isAnnotationEnabled(Class clazz, Method method, Class<? extends Annotation> annotation) {
        Optional<Boolean> value = getConfigPropertyForEnabled(clazz, method, annotation);

        return value.orElse((annotation.equals(Fallback.class)) || this.nonFallbackEnabled);
    }

    private Optional<Boolean> getConfigPropertyForEnabled(Class clazz, Method method, Class<? extends Annotation> annotation) {
        Optional<Boolean> value = Optional.empty();
        if (method != null) {
            value = config.getOptionalValue(getClassMethodKeyPrefix(clazz, method, annotation, "enabled"), Boolean.class);
        }
        if (!value.isPresent()) {
            value = config.getOptionalValue(getClassKeyPrefix(clazz, annotation, "enabled"), Boolean.class);
        }
        if (!value.isPresent()) {
            value = config.getOptionalValue(getKeyPrefix(annotation, "enabled"), Boolean.class);
        }

        return value;
    }

    private <T> Optional<T> getConfigProperty(Class clazz, Method method, Class<? extends Annotation> annotation, String propertyName, Class<T> tClass) {
        Optional<T> value;
        if (method != null) {
            value = config.getOptionalValue(getClassMethodKeyPrefix(clazz, method, annotation, propertyName), tClass);
        } else {
            value = config.getOptionalValue(getClassKeyPrefix(clazz, annotation, propertyName), tClass);
        }
        if (!value.isPresent()) {
            value = config.getOptionalValue(getKeyPrefix(annotation, propertyName), tClass);
        }

        return value;
    }

    private String getClassMethodKeyPrefix(Class clazz, Method method, Class<? extends Annotation> annotation, String propertyName) {
        return clazz.getName() + "/" + method.getName() + "/" + getKeyPrefix(annotation, propertyName);
    }

    private String getClassKeyPrefix(Class clazz, Class<? extends Annotation> annotation, String propertyName) {
        return clazz.getName() + "/" + getKeyPrefix(annotation, propertyName);
    }

    private String getKeyPrefix(Class<? extends Annotation> annotation, String propertyName) {
        return annotation.getSimpleName() + "/" + propertyName;
    }
}
