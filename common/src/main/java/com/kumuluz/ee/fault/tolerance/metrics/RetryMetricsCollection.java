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
 * Metrics collection for {@link org.eclipse.microprofile.faulttolerance.Retry}.
 *
 * @author Urban Malc
 * @since 1.1.0
 */
public class RetryMetricsCollection extends BaseMetricsCollection {

    private static final String RETRY_PREFIX = "retry.";

    private Counter callsSucceededNotRetried;
    private Counter callsSucceededRetried;
    private Counter callsFailed;
    private Counter retriesTotal;

    public RetryMetricsCollection(MetricRegistry registry) {
        super(registry);
    }

    @Override
    public void initMetrics() {
        Metadata callsSucceededNotRetriedMetadata = createMetadata(
                metricsPrefix + RETRY_PREFIX + "callsSucceededNotRetried.total",
                MetricType.COUNTER,
                MetricUnits.NONE,
                "The number of times the method was called and succeeded without retrying");
        this.callsSucceededNotRetried = registry.counter(callsSucceededNotRetriedMetadata);

        Metadata callsSucceededRetriedMetadata = createMetadata(
                metricsPrefix + RETRY_PREFIX + "callsSucceededRetried.total",
                MetricType.COUNTER,
                MetricUnits.NONE,
                "The number of times the method was called and succeeded after retrying at least once");
        this.callsSucceededRetried = registry.counter(callsSucceededRetriedMetadata);

        Metadata callsFailedMetadata = createMetadata(
                metricsPrefix + RETRY_PREFIX + "callsFailed.total",
                MetricType.COUNTER,
                MetricUnits.NONE,
                "The number of times the method was called and ultimately failed after retrying");
        this.callsFailed = registry.counter(callsFailedMetadata);

        Metadata retriesTotalMetadata = createMetadata(
                metricsPrefix + RETRY_PREFIX + "retries.total",
                MetricType.COUNTER,
                MetricUnits.NONE,
                "The total number of times the method was retried");
        this.retriesTotal = registry.counter(retriesTotalMetadata);
    }

    public Counter getCallsSucceededNotRetried() {
        return callsSucceededNotRetried;
    }

    public Counter getCallsSucceededRetried() {
        return callsSucceededRetried;
    }

    public Counter getCallsFailed() {
        return callsFailed;
    }

    public Counter getRetriesTotal() {
        return retriesTotal;
    }
}
