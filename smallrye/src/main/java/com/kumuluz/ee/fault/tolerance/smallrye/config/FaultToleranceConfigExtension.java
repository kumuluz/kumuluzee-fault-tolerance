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
package com.kumuluz.ee.fault.tolerance.smallrye.config;

import com.kumuluz.ee.common.ConfigExtension;
import com.kumuluz.ee.common.config.EeConfig;
import com.kumuluz.ee.common.dependencies.EeExtensionDef;
import com.kumuluz.ee.common.dependencies.EeExtensionGroup;
import com.kumuluz.ee.common.wrapper.KumuluzServerWrapper;
import com.kumuluz.ee.configuration.ConfigurationSource;

import java.util.LinkedList;
import java.util.List;

/**
 * KumuluzEE config extension which registers {@link FaultToleranceConfigMapper}.
 *
 * @author Urban Malc
 * @since 2.0.0
 */
@EeExtensionDef(name = "FaultToleranceSmallRyeMp", group = EeExtensionGroup.CONFIG)
public class FaultToleranceConfigExtension implements ConfigExtension {

    @Override
    public void load() {

    }

    @Override
    public void init(KumuluzServerWrapper kumuluzServerWrapper, EeConfig eeConfig) {

    }

    @Override
    public List<ConfigurationSource> getConfigurationSources() {

        List<ConfigurationSource> configSources = new LinkedList<>();
        configSources.add(new FaultToleranceConfigMapper());
        configSources.add(new MetricsKeyConfigOverride());

        return configSources;
    }
}
