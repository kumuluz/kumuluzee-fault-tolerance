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

import com.kumuluz.ee.fault.tolerance.config.MicroprofileConfigUtil;
import org.eclipse.microprofile.metrics.MetricRegistry;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Utility class for initialising {@link MetricRegistry}, if metrics are present on the classpath.
 *
 * @author Urban Malc
 * @since 1.1.0
 */
@ApplicationScoped
public class MetricsUtil {

    private static final Logger log = Logger.getLogger(MetricsUtil.class.getSimpleName());

    @Inject
    private MicroprofileConfigUtil configUtil;

    private MetricRegistry registry = null;

    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {

        // first check if fault tolerance - metrics integration is enabled
        if (configUtil.getConfig().getOptionalValue("MP_Fault_Tolerance_Metrics_Enabled", Boolean.class)
                .orElse(true)) {
            try {
                Class.forName("com.kumuluz.ee.metrics.MetricsExtension");
                registry = CDI.current().select(MetricRegistry.class).get();
                log.info("KumuluzEE Metrics found, Fault Tolerance metrics will be initialized.");
            } catch (ClassNotFoundException ignored) {
                log.info("KumuluzEE Metrics not found, Fault Tolerance metrics will not be initialized.");
            }
        } else {
            log.info("MicroProfile Fault Tolerance Metrics integration disabled.");
        }
    }

    public Optional<MetricRegistry> getRegistry() {
        return Optional.ofNullable(registry);
    }
}
