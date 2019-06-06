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
package com.kumuluz.ee.fault.tolerance.smallrye;

import com.kumuluz.ee.common.dependencies.EeExtensionGroup;
import com.kumuluz.ee.fault.tolerance.smallrye.beans.NoopMetricRegistry;
import org.eclipse.microprofile.metrics.MetricRegistry;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

/**
 * CDI extension that registers {@link NoopMetricRegistry} as a bean if KumuluzEE Metrics extension is not present.
 *
 * @author Urban Malc
 * @since 2.0.0
 */
public class SmallRyeCdiExtension implements Extension {

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event) {
        if (!SmallRyeFtExtension.isExtensionPresent(EeExtensionGroup.METRICS, "MetricsCommons")) {
            // add noop metrics registry producer to satisfy injection points
            // at this point metrics collection for fault tolerance is guaranteed to be disabled
            event.addBean()
                    .addType(MetricRegistry.class)
                    .addQualifier(Default.Literal.INSTANCE)
                    .scope(ApplicationScoped.class)
                    .produceWith(f -> new NoopMetricRegistry());
        }
    }
}
