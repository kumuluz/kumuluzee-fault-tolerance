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
package com.kumuluz.ee.fault.tolerance.commands;

import com.kumuluz.ee.fault.tolerance.enums.CircuitBreakerType;
import com.kumuluz.ee.fault.tolerance.models.ExecutionMetadata;
import com.netflix.hystrix.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Circuit breaker with support for success threshold.
 *
 * Adapted from {@link com.netflix.hystrix.HystrixCircuitBreaker.HystrixCircuitBreakerImpl}.
 *
 * @author Urban Malc
 * @since 1.1.0
 */
public class SuccessThresholdCircuitBreaker implements HystrixCircuitBreaker {

    private final HystrixCommandProperties properties;

    private final AtomicReference<Status> status = new AtomicReference<>(Status.CLOSED);
    private final AtomicLong circuitOpened = new AtomicLong(-1);

    private int successThreshold;

    private AtomicLong remainingHalfOpenInvocations = new AtomicLong(successThreshold);

    private AtomicLong successfulInvocations = new AtomicLong(0);
    private AtomicLong failedInvocations = new AtomicLong(0);

    private SuccessThresholdCircuitBreaker(HystrixCommandProperties properties, Integer successThreshold) {
        this.properties = properties;
        this.successThreshold = (successThreshold == null) ? 1 : successThreshold;
    }

    enum Status {
        CLOSED, OPEN, HALF_OPEN
    }

    @Override
    public void markSuccess() {
        if (successfulInvocations.incrementAndGet() == successThreshold) {
            //This thread wins the race to close the circuit
            circuitOpened.set(-1L);

            this.status.set(Status.CLOSED);
            this.successfulInvocations.set(0);
            this.failedInvocations.set(0);
            this.remainingHalfOpenInvocations.set(successThreshold);
        }
    }

    @Override
    public void markNonSuccess() {
        if (status.compareAndSet(Status.HALF_OPEN, Status.OPEN)) {
            //This thread wins the race to re-open the circuit - it resets the start time for the sleep window
            circuitOpened.set(System.currentTimeMillis());
            this.successfulInvocations.set(0);
            this.failedInvocations.set(0);
            this.remainingHalfOpenInvocations.set(successThreshold);
        } else {
            this.failedInvocations.incrementAndGet();
            checkThresholds();
        }
    }

    @Override
    public boolean isOpen() {
        if (properties.circuitBreakerForceOpen().get()) {
            return true;
        }
        if (properties.circuitBreakerForceClosed().get()) {
            return false;
        }
        return circuitOpened.get() >= 0;
    }

    /**
     * Shouldn't matter, only used for Hystrix internal tests.
     */
    @Override
    public boolean allowRequest() {
        throw new UnsupportedOperationException();
    }

    private boolean isAfterSleepWindow() {
        final long circuitOpenTime = circuitOpened.get();
        final long currentTime = System.currentTimeMillis();
        final long sleepWindowTime = properties.circuitBreakerSleepWindowInMilliseconds().get();
        return currentTime > circuitOpenTime + sleepWindowTime;
    }

    private void checkThresholds() {
        long failed = this.failedInvocations.get();
        long sum = failed + this.successfulInvocations.get();

        if (sum >= this.properties.circuitBreakerRequestVolumeThreshold().get() &&
                (double) failed / sum >= (double) this.properties.circuitBreakerErrorThresholdPercentage().get() / 100) {
            if (status.compareAndSet(Status.CLOSED, Status.OPEN)) {
                circuitOpened.set(System.currentTimeMillis());
                this.failedInvocations.set(0);
                this.successfulInvocations.set(0);
                this.remainingHalfOpenInvocations.set(successThreshold);
            }
        }
    }

    @Override
    public boolean attemptExecution() {
        if (properties.circuitBreakerForceOpen().get()) {
            return false;
        }
        if (properties.circuitBreakerForceClosed().get()) {
            return true;
        }
        if (circuitOpened.get() == -1) {
            return true;
        } else {
            if (isAfterSleepWindow()) {
                //only the first few requests after sleep window should execute
                //if the executing command succeeds, the status will transition to CLOSED
                //if the executing command fails, the status will transition to OPEN
                //if the executing command gets unsubscribed, the status will transition to OPEN
                return this.remainingHalfOpenInvocations.decrementAndGet() >= 0;
            } else {
                return false;
            }
        }
    }

    public static class CustomCbFactory extends Factory {
        // String is HystrixCommandKey.name() (we can't use HystrixCommandKey directly as we can't guarantee it implements hashcode/equals correctly)
        private static ConcurrentHashMap<String, HystrixCircuitBreaker> circuitBreakersByCommand = new ConcurrentHashMap<>();

        /**
         * Get the {@link HystrixCircuitBreaker} instance for a given {@link HystrixCommandKey}.
         * <p>
         * This is thread-safe and ensures only 1 {@link HystrixCircuitBreaker} per {@link HystrixCommandKey}.
         *
         * @param key
         *            {@link HystrixCommandKey} of {@link HystrixCommand} instance requesting the {@link HystrixCircuitBreaker}
         * @param group
         *            Pass-thru to {@link HystrixCircuitBreaker}
         * @param properties
         *            Pass-thru to {@link HystrixCircuitBreaker}
         * @param metrics
         *            Pass-thru to {@link HystrixCircuitBreaker}
         * @return {@link HystrixCircuitBreaker} for {@link HystrixCommandKey}
         */
        public static HystrixCircuitBreaker getInstance(HystrixCommandKey key,
                                                        HystrixCommandGroupKey group,
                                                        HystrixCommandProperties properties,
                                                        HystrixCommandMetrics metrics,
                                                        ExecutionMetadata metadata) {
            String mapKey = group.name() + "." + key.name();
            // this should find it for all but the first time
            HystrixCircuitBreaker previouslyCached = circuitBreakersByCommand.get(mapKey);
            if (previouslyCached != null) {
                return previouslyCached;
            }

            // if we get here this is the first time so we need to initialize

            // Create and add to the map ... use putIfAbsent to atomically handle the possible race-condition of
            // 2 threads hitting this point at the same time and let ConcurrentHashMap provide us our thread-safety
            // If 2 threads hit here only one will get added and the other will get a non-null response instead.
            HystrixCircuitBreaker instance = (metadata.getCircuitBreakerType().equals(CircuitBreakerType.HYSTRIX)) ?
                    Factory.getInstance(key, group, properties, metrics) :
                    new SuccessThresholdCircuitBreaker(properties, metadata.getCircuitBreakerSuccessThreshold());
            HystrixCircuitBreaker cbForCommand = circuitBreakersByCommand.putIfAbsent(mapKey, instance);
            if (cbForCommand == null) {
                // this means the putIfAbsent step just created a new one so let's retrieve and return it
                return circuitBreakersByCommand.get(mapKey);
            } else {
                // this means a race occurred and while attempting to 'put' another one got there before
                // and we instead retrieved it and will now return it
                return cbForCommand;
            }
        }
    }
}
