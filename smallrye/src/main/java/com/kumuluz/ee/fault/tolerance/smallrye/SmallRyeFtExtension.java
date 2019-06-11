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

import com.kumuluz.ee.common.Extension;
import com.kumuluz.ee.common.config.EeConfig;
import com.kumuluz.ee.common.dependencies.*;
import com.kumuluz.ee.common.runtime.EeRuntime;
import com.kumuluz.ee.common.wrapper.KumuluzServerWrapper;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;

import java.util.logging.Logger;

/**
 * KumuluzEE Fault Tolerance extension implemented by SmallRye.
 *
 * @author Urban Malc
 * @since 2.0.0
 */
@EeExtensionDef(name = "SmallRye", group = EeExtensionGroup.FAULT_TOLERANCE)
@EeComponentDependencies({
        @EeComponentDependency(EeComponentType.CDI),
})
public class SmallRyeFtExtension implements Extension {

    private static final Logger LOG = Logger.getLogger(SmallRyeFtExtension.class.getName());

    private static boolean metricsEnabled = true;

    @Override
    public void init(KumuluzServerWrapper kumuluzServerWrapper, EeConfig eeConfig) {
        if (!isExtensionPresent(EeExtensionGroup.CONFIG, "MicroProfile")) {
            throw new IllegalStateException("KumuluzEE Config MP extension is required for SmallRye Fault Tolerance " +
                    "extension to function properly. Please make sure it is added to dependencies.");
        }

        if (!SmallRyeFtExtension.isExtensionPresent(EeExtensionGroup.METRICS, "MetricsCommons") &&
               ConfigurationUtil.getInstance().getBoolean("MP_Fault_Tolerance_Metrics_Enabled")
                       .orElse(true)) {
            // metrics extension is missing but collection of metrics is enabled
            LOG.info("KumuluzEE Metrics extension not found. Disabling metrics collection in KumuluzEE Fault " +
                    "Tolerance.");
            metricsEnabled = false;
        }
    }

    @Override
    public void load() {

    }

    @Override
    public boolean isEnabled() {
        // disabling the extension is currently not supported
        return true;
    }

    static boolean isExtensionPresent(String group, String implementationName) {
        return EeRuntime.getInstance().getEeExtensions().stream().anyMatch(ext ->
                ext.getGroup().equals(group) && ext.getImplementationName().equals(implementationName));
    }

    public static boolean isMetricsEnabled() {
        return metricsEnabled;
    }
}
