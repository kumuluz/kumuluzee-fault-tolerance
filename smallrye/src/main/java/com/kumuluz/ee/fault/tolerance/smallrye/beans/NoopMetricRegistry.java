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
package com.kumuluz.ee.fault.tolerance.smallrye.beans;

import org.eclipse.microprofile.metrics.*;

import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * NO-OP Metric Registry, used only to satisfy injection point if KumuluzEE Metrics dependency is not present.
 *
 * @author Urban Malc
 * @since 2.0.0
 */
public class NoopMetricRegistry extends MetricRegistry {

    @Override
    public <T extends Metric> T register(String name, T metric) throws IllegalArgumentException {
        return null;
    }

    @Override
    public <T extends Metric> T register(Metadata metadata, T metric) throws IllegalArgumentException {
        return null;
    }

    @Override
    public <T extends Metric> T register(Metadata metadata, T metric, Tag... tags) throws IllegalArgumentException {
        return null;
    }

    @Override
    public Counter counter(String name) {
        return null;
    }

    @Override
    public Counter counter(String name, Tag... tags) {
        return null;
    }

    @Override
    public Counter counter(Metadata metadata) {
        return null;
    }

    @Override
    public Counter counter(Metadata metadata, Tag... tags) {
        return null;
    }

    @Override
    public ConcurrentGauge concurrentGauge(String name) {
        return null;
    }

    @Override
    public ConcurrentGauge concurrentGauge(String name, Tag... tags) {
        return null;
    }

    @Override
    public ConcurrentGauge concurrentGauge(Metadata metadata) {
        return null;
    }

    @Override
    public ConcurrentGauge concurrentGauge(Metadata metadata, Tag... tags) {
        return null;
    }

    @Override
    public Histogram histogram(String name) {
        return null;
    }

    @Override
    public Histogram histogram(String name, Tag... tags) {
        return null;
    }

    @Override
    public Histogram histogram(Metadata metadata) {
        return null;
    }

    @Override
    public Histogram histogram(Metadata metadata, Tag... tags) {
        return null;
    }

    @Override
    public Meter meter(String name) {
        return null;
    }

    @Override
    public Meter meter(String name, Tag... tags) {
        return null;
    }

    @Override
    public Meter meter(Metadata metadata) {
        return null;
    }

    @Override
    public Meter meter(Metadata metadata, Tag... tags) {
        return null;
    }

    @Override
    public Timer timer(String name) {
        return null;
    }

    @Override
    public Timer timer(String name, Tag... tags) {
        return null;
    }

    @Override
    public Timer timer(Metadata metadata) {
        return null;
    }

    @Override
    public Timer timer(Metadata metadata, Tag... tags) {
        return null;
    }

    @Override
    public SimpleTimer simpleTimer(String s) {
        return null;
    }

    @Override
    public SimpleTimer simpleTimer(String s, Tag... tags) {
        return null;
    }

    @Override
    public SimpleTimer simpleTimer(Metadata metadata) {
        return null;
    }

    @Override
    public SimpleTimer simpleTimer(Metadata metadata, Tag... tags) {
        return null;
    }

    @Override
    public boolean remove(String name) {
        return false;
    }

    @Override
    public boolean remove(MetricID metricID) {
        return false;
    }

    @Override
    public void removeMatching(MetricFilter filter) {

    }

    @Override
    public SortedSet<String> getNames() {
        return null;
    }

    @Override
    public SortedSet<MetricID> getMetricIDs() {
        return null;
    }

    @Override
    public SortedMap<MetricID, Gauge> getGauges() {
        return null;
    }

    @Override
    public SortedMap<MetricID, Gauge> getGauges(MetricFilter filter) {
        return null;
    }

    @Override
    public SortedMap<MetricID, Counter> getCounters() {
        return null;
    }

    @Override
    public SortedMap<MetricID, Counter> getCounters(MetricFilter filter) {
        return null;
    }

    @Override
    public SortedMap<MetricID, ConcurrentGauge> getConcurrentGauges() {
        return null;
    }

    @Override
    public SortedMap<MetricID, ConcurrentGauge> getConcurrentGauges(MetricFilter filter) {
        return null;
    }

    @Override
    public SortedMap<MetricID, Histogram> getHistograms() {
        return null;
    }

    @Override
    public SortedMap<MetricID, Histogram> getHistograms(MetricFilter filter) {
        return null;
    }

    @Override
    public SortedMap<MetricID, Meter> getMeters() {
        return null;
    }

    @Override
    public SortedMap<MetricID, Meter> getMeters(MetricFilter filter) {
        return null;
    }

    @Override
    public SortedMap<MetricID, Timer> getTimers() {
        return null;
    }

    @Override
    public SortedMap<MetricID, Timer> getTimers(MetricFilter filter) {
        return null;
    }

    @Override
    public SortedMap<MetricID, SimpleTimer> getSimpleTimers() {
        return null;
    }

    @Override
    public SortedMap<MetricID, SimpleTimer> getSimpleTimers(MetricFilter metricFilter) {
        return null;
    }

    @Override
    public Map<MetricID, Metric> getMetrics() {
        return null;
    }

    @Override
    public Map<String, Metadata> getMetadata() {
        return null;
    }
}
