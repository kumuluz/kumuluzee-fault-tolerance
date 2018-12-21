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
 * Metrics collection for {@link org.eclipse.microprofile.faulttolerance.Timeout}.
 *
 * @author Urban Malc
 * @since 1.1.0
 */
public class TimeoutMetricsCollection extends BaseMetricsCollection {

    private static final String TIMEOUT_PREFIX = "timeout.";

    private Histogram executionDuration;
    private Counter callsTimedOut;
    private Counter callsNotTimedOut;

    public TimeoutMetricsCollection(MetricRegistry registry) {
        super(registry);
    }

    @Override
    public void initMetrics() {
        Metadata executionDurationMetadata = createMetadata(
                metricsPrefix + TIMEOUT_PREFIX + "executionDuration",
                MetricType.HISTOGRAM,
                MetricUnits.NANOSECONDS,
                "Histogram of execution times for the method");
        executionDuration = registry.histogram(executionDurationMetadata);

        Metadata callsTimedOutMetadata = createMetadata(
                metricsPrefix + TIMEOUT_PREFIX + "callsTimedOut.total",
                MetricType.COUNTER,
                MetricUnits.NONE,
                "The number of times the method timed out");
        callsTimedOut = registry.counter(callsTimedOutMetadata);

        Metadata callsNotTimedOutMetadata = createMetadata(
                metricsPrefix + TIMEOUT_PREFIX + "callsNotTimedOut.total",
                MetricType.COUNTER,
                MetricUnits.NONE,
                "The number of times the method completed without timing out");
        callsNotTimedOut = registry.counter(callsNotTimedOutMetadata);
    }

    public Histogram getExecutionDuration() {
        return executionDuration;
    }

    public Counter getCallsTimedOut() {
        return callsTimedOut;
    }

    public Counter getCallsNotTimedOut() {
        return callsNotTimedOut;
    }
}
