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

import com.kumuluz.ee.fault.tolerance.annotations.Bulkhead;
import com.kumuluz.ee.fault.tolerance.annotations.CircuitBreaker;
import com.kumuluz.ee.fault.tolerance.annotations.Retry;
import com.kumuluz.ee.fault.tolerance.annotations.Timeout;
import com.kumuluz.ee.fault.tolerance.interfaces.FallbackHandler;

import java.lang.reflect.Method;

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

    public ExecutionMetadata(Class targetClass, Method method, String commandKey, String groupKey) {
        this.targetClass = targetClass;
        this.method = method;
        this.commandKey = commandKey;
        this.groupKey = groupKey;
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
}
