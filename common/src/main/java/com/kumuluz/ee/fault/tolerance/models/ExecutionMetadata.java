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
package com.kumuluz.ee.fault.tolerance.models;

import com.kumuluz.ee.fault.tolerance.enums.CircuitBreakerType;
import com.kumuluz.ee.fault.tolerance.metrics.*;
import org.eclipse.microprofile.faulttolerance.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Model for holding information fault tolerance needs to execute method.
 *
 * @author Luka Šarc
 * @since 1.0.0
 */
public class ExecutionMetadata {

    private final Class targetClass;
    protected final Method method;
    private final String commandKey;
    private final String groupKey;

    private boolean asynchronous;
    private Class<? extends FallbackHandler> fallbackHandlerClass;
    private Method fallbackMethod;

    private Bulkhead bulkhead;
    private Timeout timeout;
    private Retry retry;
    private CircuitBreaker circuitBreaker;

    private Integer circuitBreakerSuccessThreshold;
    private CircuitBreakerType circuitBreakerType;

    private Map<String, CommonMetricsCollection> commonMetricsCollections;
    private Map<String, RetryMetricsCollection> retryMetricsCollections;
    private Map<String, TimeoutMetricsCollection> timeoutMetricsCollections;
    private Map<String, FallbackMetricsCollection> fallbackMetricsCollectionMap;
    private Map<String, CircuitBreakerMetricsCollection> cbMetricsCollectionMap;
    private Map<String, BulkheadMetricsCollection> bulkheadMetricsCollectionMap;

    public ExecutionMetadata(Class targetClass, Method method, String commandKey, String groupKey) {
        this.targetClass = targetClass;
        this.method = method;
        this.commandKey = commandKey;
        this.groupKey = groupKey;

        this.commonMetricsCollections = new HashMap<>();
        this.retryMetricsCollections = new HashMap<>();
        this.timeoutMetricsCollections = new HashMap<>();
        this.fallbackMetricsCollectionMap = new HashMap<>();
        this.cbMetricsCollectionMap = new HashMap<>();
        this.bulkheadMetricsCollectionMap = new HashMap<>();
    }

    public String getIdentifier() {
        return this.groupKey + "." + this.commandKey;
    }

    public Class getTargetClass() {
        return targetClass;
    }

    public Method getMethod() {
        return method;
    }

    public String getCommandKey() {
        return commandKey;
    }

    public String getGroupKey() {
        return groupKey;
    }

    public boolean isAsynchronous() {
        return asynchronous;
    }

    public void setAsynchronous(boolean asynchronous) {
        this.asynchronous = asynchronous;
    }

    public Class<? extends FallbackHandler> getFallbackHandlerClass() {
        return fallbackHandlerClass;
    }

    public void setFallbackHandlerClass(Class<? extends FallbackHandler> fallbackHandlerClass) {
        this.fallbackHandlerClass = fallbackHandlerClass;
    }

    public Method getFallbackMethod() {
        return fallbackMethod;
    }

    public void setFallbackMethod(Method fallbackMethod) {
        this.fallbackMethod = fallbackMethod;
    }

    public Bulkhead getBulkhead() {
        return bulkhead;
    }

    public void setBulkhead(Bulkhead bulkhead) {
        this.bulkhead = bulkhead;
    }

    public Timeout getTimeout() {
        return timeout;
    }

    public void setTimeout(Timeout timeout) {
        this.timeout = timeout;
    }

    public Retry getRetry() {
        return retry;
    }

    public void setRetry(Retry retry) {
        this.retry = retry;
    }

    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    public void setCircuitBreaker(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }

    public Integer getCircuitBreakerSuccessThreshold() {
        return circuitBreakerSuccessThreshold;
    }

    public void setCircuitBreakerSuccessThreshold(Integer circuitBreakerSuccessThreshold) {
        this.circuitBreakerSuccessThreshold = circuitBreakerSuccessThreshold;
    }

    public CircuitBreakerType getCircuitBreakerType() {
        return circuitBreakerType;
    }

    public void setCircuitBreakerType(CircuitBreakerType circuitBreakerType) {
        this.circuitBreakerType = circuitBreakerType;
    }

    public Optional<CommonMetricsCollection> getCommonMetricsCollection(String methodName) {
        return Optional.ofNullable(commonMetricsCollections.get(methodName));
    }

    public Optional<RetryMetricsCollection> getRetryMetricsCollection(String methodName) {
        return Optional.ofNullable(retryMetricsCollections.get(methodName));
    }

    public Optional<TimeoutMetricsCollection> getTimeoutMetricsCollection(String methodName) {
        return Optional.ofNullable(timeoutMetricsCollections.get(methodName));
    }

    public Optional<FallbackMetricsCollection> getFallbackMetricsCollection(String methodName) {
        return Optional.ofNullable(fallbackMetricsCollectionMap.get(methodName));
    }

    public Optional<CircuitBreakerMetricsCollection> getCbMetricsCollection(String methodName) {
        return Optional.ofNullable(cbMetricsCollectionMap.get(methodName));
    }

    public Optional<BulkheadMetricsCollection> getBulkheadMetricsCollection(String methodName) {
        return Optional.ofNullable(bulkheadMetricsCollectionMap.get(methodName));
    }

    public void addCommonMetricsCollection(Method method, CommonMetricsCollection commonMetricsCollection) {
        if (commonMetricsCollections.putIfAbsent(method.getName(), commonMetricsCollection) == null) {
            initMetricsCollection(method.getName(), commonMetricsCollection);
        }
    }

    public void addRetryMetricsCollection(Method method, RetryMetricsCollection retryMetricsCollection) {
        if (retryMetricsCollections.putIfAbsent(method.getName(), retryMetricsCollection) == null) {
            initMetricsCollection(method.getName(), retryMetricsCollection);
        }
    }

    public void addTimeoutMetricsCollection(Method method, TimeoutMetricsCollection timeoutMetricsCollection) {
        if (timeoutMetricsCollections.putIfAbsent(method.getName(), timeoutMetricsCollection) == null) {
            initMetricsCollection(method.getName(), timeoutMetricsCollection);
        }
    }

    public void addFallbackMetricsCollection(Method method, FallbackMetricsCollection fallbackMetricsCollection) {
        if (fallbackMetricsCollectionMap.putIfAbsent(method.getName(), fallbackMetricsCollection) == null) {
            initMetricsCollection(method.getName(), fallbackMetricsCollection);
        }
    }

    public void addCbMetricsCollection(Method method, CircuitBreakerMetricsCollection cbMetricsCollection) {
        if (cbMetricsCollectionMap.putIfAbsent(method.getName(), cbMetricsCollection) == null) {
            initMetricsCollection(method.getName(), cbMetricsCollection);
        }
    }

    public void addBulkheadMetricsCollection(Method method, BulkheadMetricsCollection bulkheadMetricsCollection) {
        if (bulkheadMetricsCollectionMap.putIfAbsent(method.getName(), bulkheadMetricsCollection) == null) {
            initMetricsCollection(method.getName(), bulkheadMetricsCollection);
        }
    }

    private void initMetricsCollection(String methodName, BaseMetricsCollection baseMetricsCollection) {
        baseMetricsCollection.setMetricsPrefix("ft." + this.targetClass.getCanonicalName() + "." + methodName + ".");
        baseMetricsCollection.initialize();
    }
}
