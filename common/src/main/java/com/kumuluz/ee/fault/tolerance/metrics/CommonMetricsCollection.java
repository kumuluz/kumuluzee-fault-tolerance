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
 * Metrics collection for metrics, common for all fault tolerance annotations.
 *
 * @author Urban Malc
 * @since 1.1.0
 */
public class CommonMetricsCollection extends BaseMetricsCollection {

    private Counter totalInvocations;
    private Counter failedInvocations;

    public CommonMetricsCollection(MetricRegistry registry) {
        super(registry);
    }

    @Override
    public void initMetrics() {
        Metadata totalInvocationsMetadata = createMetadata(
                metricsPrefix + "invocations.total",
                MetricType.COUNTER,
                MetricUnits.NONE,
                "The number of times the method was called");
        this.totalInvocations = registry.counter(totalInvocationsMetadata);

        Metadata failedInvocationsMetadata = createMetadata(
                metricsPrefix + "invocations.failed.total",
                MetricType.COUNTER,
                MetricUnits.NONE,
                "The number of times the method was called and, after all Fault Tolerance actions had been " +
                        "processed, threw a Throwable");
        this.failedInvocations = registry.counter(failedInvocationsMetadata);
    }

    public Counter getTotalInvocations() {
        return totalInvocations;
    }

    public Counter getFailedInvocations() {
        return failedInvocations;
    }
}
