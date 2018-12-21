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

import java.util.concurrent.atomic.AtomicLong;

/**
 * Metrics collection for {@link org.eclipse.microprofile.faulttolerance.Bulkhead}.
 *
 * @author Urban Malc
 * @since 1.1.0
 */
public class BulkheadMetricsCollection extends BaseMetricsCollection {

    private static final String BULKHEAD_PREFIX = "bulkhead.";

    private boolean isAsync;

    private Counter callsAccepted;
    private Counter callsRejected;
    private Histogram executionDuration;
    private Histogram waitingDuration;

    private AtomicLong currentlyExecuting;
    private AtomicLong currentlyWaiting;

    public BulkheadMetricsCollection(MetricRegistry registry, boolean isAsync) {
        super(registry);
        this.isAsync = isAsync;
    }

    @Override
    protected void initMetrics() {
        Metadata callsAcceptedMetadata = createMetadata(
                metricsPrefix + BULKHEAD_PREFIX + "callsAccepted.total",
                MetricType.COUNTER,
                MetricUnits.NONE,
                "Number of calls accepted by the bulkhead");
        this.callsAccepted = registry.counter(callsAcceptedMetadata);

        Metadata callsRejectedMetadata = createMetadata(
                metricsPrefix + BULKHEAD_PREFIX + "callsRejected.total",
                MetricType.COUNTER,
                MetricUnits.NONE,
                "Number of calls rejected by the bulkhead");
        this.callsRejected = registry.counter(callsRejectedMetadata);

        Metadata executionDurationMetadata = createMetadata(
                metricsPrefix + BULKHEAD_PREFIX + "executionDuration",
                MetricType.HISTOGRAM,
                MetricUnits.NANOSECONDS,
                "Histogram of method execution times. This does not include any time spent waiting in the " +
                        "bulkhead queue.");
        this.executionDuration = registry.histogram(executionDurationMetadata);

        this.currentlyExecuting = new AtomicLong(0);
        registerGauge(metricsPrefix + BULKHEAD_PREFIX + "concurrentExecutions",
                () -> this.currentlyExecuting.get(), MetricUnits.NONE, "Number of currently running executions");

        if (this.isAsync) {
            Metadata waitingDurationMetadata = createMetadata(
                    metricsPrefix + BULKHEAD_PREFIX + "waiting.duration",
                    MetricType.HISTOGRAM,
                    MetricUnits.NANOSECONDS,
                    "Histogram of the time executions spend waiting in the queue");
            this.waitingDuration = registry.histogram(waitingDurationMetadata);

            this.currentlyWaiting = new AtomicLong(0);
            registerGauge(metricsPrefix + BULKHEAD_PREFIX + "waitingQueue.population",
                    () -> this.currentlyWaiting.get(), MetricUnits.NONE, "Number of executions currently " +
                            "waiting in the queue");
        }
    }

    public Counter getCallsAccepted() {
        return callsAccepted;
    }

    public Counter getCallsRejected() {
        return callsRejected;
    }

    public Histogram getExecutionDuration() {
        return executionDuration;
    }

    public Histogram getWaitingDuration() {
        return waitingDuration;
    }

    public AtomicLong getCurrentlyExecuting() {
        return currentlyExecuting;
    }

    public AtomicLong getCurrentlyWaiting() {
        return currentlyWaiting;
    }
}
