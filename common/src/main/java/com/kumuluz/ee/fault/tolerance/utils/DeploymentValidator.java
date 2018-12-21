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
package com.kumuluz.ee.fault.tolerance.utils;

import org.eclipse.microprofile.faulttolerance.*;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.*;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Validates application fault tolerance logic during deployment, discovering configuration problems before actual
 * execution.
 *
 * @author Urban Malc
 * @since 1.1.0
 */
public class DeploymentValidator implements Extension {

    public <T> void processAnnotatedType(@Observes ProcessAnnotatedType<T> anType, BeanManager beanManager) {
        AnnotatedType<T> type = anType.getAnnotatedType();

        if (type.isAnnotationPresent(Timeout.class)) {
            validateTimeout(type, null);
        }

        if (type.isAnnotationPresent(Retry.class)) {
            validateRetry(type, null);
        }

        if (type.isAnnotationPresent(CircuitBreaker.class)) {
            validateCircuitBreaker(type, null);
        }

        if (type.isAnnotationPresent(Bulkhead.class)) {
            validateBulkhead(type, null);
        }

        for (AnnotatedMethod<? super T> method : anType.getAnnotatedType().getMethods()) {

            if (method.isAnnotationPresent(Asynchronous.class)) {
                validateAsynchronous(type, method);
            }

            if (method.isAnnotationPresent(Timeout.class)) {
                validateTimeout(type, method);
            }

            if (method.isAnnotationPresent(Retry.class)) {
                validateRetry(type, method);
            }

            if (method.isAnnotationPresent(Fallback.class)) {
                validateFallback(type, method);
            }

            if (method.isAnnotationPresent(CircuitBreaker.class)) {
                validateCircuitBreaker(type, method);
            }

            if (method.isAnnotationPresent(Bulkhead.class)) {
                validateBulkhead(type, method);
            }
        }
    }

    private <T> void validateBulkhead(AnnotatedType<T> type, AnnotatedMethod<? super T> method) {
        Bulkhead bulkhead = (method == null) ? type.getAnnotation(Bulkhead.class) :
                method.getAnnotation(Bulkhead.class);

        if (bulkhead.value() <= 0) {
            throwDefinitionException(type, method, "Bulkhead value parameter must be greater than 0.");
        }
        if (bulkhead.waitingTaskQueue() <= 0) {
            throwDefinitionException(type, method, "Bulkhead waitingTaskQueue parameter must be greater than 0.");
        }
    }

    private <T> void validateCircuitBreaker(AnnotatedType<T> type, AnnotatedMethod<? super T> method) {
        CircuitBreaker circuitBreaker = (method == null) ? type.getAnnotation(CircuitBreaker.class) :
                method.getAnnotation(CircuitBreaker.class);

        if (circuitBreaker.delay() < 0) {
            throwDefinitionException(type, method, "CircuitBreaker delay parameter must be greater than or equal " +
                    "to 0.");
        }
        if (circuitBreaker.requestVolumeThreshold() < 1) {
            throwDefinitionException(type, method, "CircuitBreaker requestVolumeThreshold parameter must be greater " +
                    "than or equal to 1.");
        }
        if (circuitBreaker.failureRatio() < 0.0 || circuitBreaker.failureRatio() > 1.0) {
            throwDefinitionException(type, method, "CircuitBreaker failureRatio parameter must be between " +
                    "0.0 and 1.0 inclusive.");
        }
        if (circuitBreaker.successThreshold() < 1) {
            throwDefinitionException(type, method, "CircuitBreaker successThreshold parameter must be greater " +
                    "than or equal to 1.");
        }
    }

    private <T> void validateFallback(AnnotatedType<T> type, AnnotatedMethod<? super T> method) {

        if (method == null) {
            return;
        }

        Fallback fallback = method.getAnnotation(Fallback.class);

        if (fallback.fallbackMethod().equals("") && fallback.value().equals(Fallback.DEFAULT.class)) {
            throwDefinitionException(type, method, "Fallback annotation does not specify fallback method or fallback " +
                    "handler class.");
        }

        if (!fallback.fallbackMethod().equals("") && !fallback.value().equals(Fallback.DEFAULT.class)) {
            throwDefinitionException(type, method, "Fallback annotation specifies fallback method and fallback " +
                    "handler class. Only one can be defined.");
        }

        if (!fallback.fallbackMethod().equals("")) {
            List<Method> matchingByName = type.getMethods().stream()
                    .map(AnnotatedMethod::getJavaMember)
                    .filter(m -> m.getName().equals(fallback.fallbackMethod()))
                    .collect(Collectors.toList());

            if (matchingByName.isEmpty()) {
                throwDefinitionException(type, method, "Fallback method with name " + fallback.fallbackMethod() +
                        " not found.");
            }

            List<Method> matchingByParameters = matchingByName.stream()
                    .filter(m -> fallbackMethodParametersEqual(method, m))
                    .collect(Collectors.toList());

            if (matchingByParameters.isEmpty()) {
                throwDefinitionException(type, method, "Parameters of method annotated with @Fallback do " +
                        "not match the parameters of its fallback method.");
            }

            List<Method> matchingByReturnType = matchingByParameters.stream()
                    .filter(m -> fallbackMethodReturnTypeEqual(method, m))
                    .collect(Collectors.toList());

            if (matchingByReturnType.isEmpty()) {
                throwDefinitionException(type, method, "Return type of method annotated with @Fallback does " +
                        "not match the return type of its fallback method.");
            }
        } else {
            Method[] handleInterfaceMethods = FallbackHandler.class.getMethods();
            for (Method interfaceMethod : handleInterfaceMethods) {
                if (interfaceMethod.getName().equals("handle")) {
                    try {
                        Method fallbackMethod = fallback.value().getMethod(interfaceMethod.getName(),
                                interfaceMethod.getParameterTypes());

                        if (!fallbackMethodReturnTypeEqual(method, fallbackMethod)) {
                            throwDefinitionException(type, method, "Return type of method annotated with @Fallback " +
                                    "does not match the return type of its fallback method.");
                        }
                    } catch (NoSuchMethodException e) {
                        throwDefinitionException(type, method, "Could not locate fallback method in FallbackHandler.");
                    }
                    break;
                }
            }
        }
    }

    private <T> boolean fallbackMethodParametersEqual(AnnotatedMethod<? super T> annotatedMethod,
                                                      Method fallbackMethod) {
        Class[] methodParamteres = annotatedMethod.getJavaMember().getParameterTypes();
        Class[] fallbackParameters = fallbackMethod.getParameterTypes();

        if (methodParamteres.length != fallbackParameters.length) {
            return false;
        }
        for (int i = 0; i < methodParamteres.length; i++) {
            if (!methodParamteres[i].equals(fallbackParameters[i])) {
                return false;
            }
        }

        return true;
    }

    private <T> boolean fallbackMethodReturnTypeEqual(AnnotatedMethod<? super T> annotatedMethod,
                                                      Method fallbackMethod) {
        return annotatedMethod.getJavaMember().getReturnType().equals(fallbackMethod.getReturnType());
    }

    private <T> void validateRetry(AnnotatedType<T> type, AnnotatedMethod<? super T> method) {
        Retry retry = (method == null) ? type.getAnnotation(Retry.class) : method.getAnnotation(Retry.class);

        if (retry.maxRetries() < -1) {
            throwDefinitionException(type, method, "Retry maxRetries parameter must be greater than or equal to -1.");
        }
        if (retry.delay() < 0) {
            throwDefinitionException(type, method, "Retry delay parameter must be greater than or equal to 0.");
        }
        if (retry.maxDuration() != 0 &&
                Duration.of(retry.maxDuration(), retry.durationUnit())
                        .compareTo(Duration.of(retry.delay(), retry.delayUnit())) < 0) {
            throwDefinitionException(type, method, "Retry maxDuration parameter must be greater than or equal to" +
                    " delay parameter.");
        }
        if (retry.jitter() < 0) {
            throwDefinitionException(type, method, "Retry jitter parameter must be greater than or equal to 0.");
        }
    }

    private <T> void validateTimeout(AnnotatedType<T> type, AnnotatedMethod<? super T> method) {
        Timeout timeout = (method == null) ? type.getAnnotation(Timeout.class) : method.getAnnotation(Timeout.class);

        if (timeout.value() < 0) {
            throwDefinitionException(type, method, "Timeout value must be greater than zero.");
        }
    }

    private <T> void validateAsynchronous(AnnotatedType<T> type, AnnotatedMethod<? super T> method) {
        if (method != null && !method.getJavaMember().getReturnType().isAssignableFrom(Future.class)) {
            throwDefinitionException(type, method, "Method annotated with @Asynchronous must return a Future.");
        }
    }

    private <T> void throwDefinitionException(AnnotatedType<T> type, AnnotatedMethod<? super T> method,
                                                String message) {
        String methodDescriptor = (method == null) ? "" : "#" + method.getJavaMember().getName();

        throw new FaultToleranceDefinitionException("[" + type.getJavaClass().getName() + methodDescriptor + "] "
                + message);
    }
}
