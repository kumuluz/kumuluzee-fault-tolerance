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
package com.kumuluz.ee.fault.tolerance.metrics;

import org.eclipse.microprofile.metrics.*;

/**
 * Metrics collection for {@link org.eclipse.microprofile.faulttolerance.CircuitBreaker}.
 *
 * @author Urban Malc
 * @since 1.1.0
 */
public class CircuitBreakerMetricsCollection extends BaseMetricsCollection {

    private static final String CIRCUIT_BREAKER_PREFIX = "circuitbreaker.";

    private Counter callsSucceeded;
    private Counter callsFailed;
    private Counter callsPrevented;
    private Counter opened;

    public CircuitBreakerMetricsCollection(MetricRegistry registry) {
        super(registry);
    }

    @Override
    public void initMetrics() {
        Metadata callsSucceededMetadata = createMetadata(
                metricsPrefix + CIRCUIT_BREAKER_PREFIX + "callsSucceeded.total",
                MetricType.COUNTER,
                MetricUnits.NONE,
                "Number of calls allowed to run by the circuit breaker that returned successfully");
        callsSucceeded = registry.counter(callsSucceededMetadata);

        Metadata callsFailedMetadata = createMetadata(
                metricsPrefix + CIRCUIT_BREAKER_PREFIX + "callsFailed.total",
                MetricType.COUNTER,
                MetricUnits.NONE,
                "Number of calls allowed to run by the circuit breaker that then failed");
        callsFailed = registry.counter(callsFailedMetadata);

        Metadata callsPreventedMetadata = createMetadata(
                metricsPrefix + CIRCUIT_BREAKER_PREFIX + "callsPrevented.total",
                MetricType.COUNTER,
                MetricUnits.NONE,
                "Number of calls prevented from running by an open circuit breaker");
        callsPrevented = registry.counter(callsPreventedMetadata);

        Metadata openedMetadata = createMetadata(
                metricsPrefix + CIRCUIT_BREAKER_PREFIX + "opened.total",
                MetricType.COUNTER,
                MetricUnits.NONE,
                "Number of times the circuit breaker has moved from closed state to open state");
        opened = registry.counter(openedMetadata);
    }

    public void registerOpenGauge(Gauge<Long> gauge) {
        registerGauge(metricsPrefix + CIRCUIT_BREAKER_PREFIX + "open.total", gauge, MetricUnits.NANOSECONDS,
                "Amount of time the circuit breaker has spent in open state");
    }

    public void registerHalfOpenGauge(Gauge<Long> gauge) {
        registerGauge(metricsPrefix + CIRCUIT_BREAKER_PREFIX + "halfOpen.total", gauge, MetricUnits.NANOSECONDS,
                "Amount of time the circuit breaker has spent in half-open state");
    }

    public void registerClosedGauge(Gauge<Long> gauge) {
        registerGauge(metricsPrefix + CIRCUIT_BREAKER_PREFIX + "closed.total", gauge, MetricUnits.NANOSECONDS,
                "Amount of time the circuit breaker has spent in closed state");
    }

    public Counter getCallsSucceeded() {
        return callsSucceeded;
    }

    public Counter getCallsFailed() {
        return callsFailed;
    }

    public Counter getCallsPrevented() {
        return callsPrevented;
    }

    public Counter getOpened() {
        return opened;
    }
}
