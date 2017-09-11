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
package com.kumuluz.ee.fault.tolerance.interfaces;

import com.kumuluz.ee.fault.tolerance.enums.FaultToleranceType;
import com.kumuluz.ee.fault.tolerance.models.ConfigurationProperty;
import org.jboss.weld.context.RequestContext;

import javax.interceptor.InvocationContext;
import java.util.Optional;

/**
 * Util for setting up basic configuration and passing execution of intercepted method within
 * fault tolerance executor
 *
 * @author Luka Šarc
 * @since 1.0.0
 */
public interface FaultToleranceUtil {

    Object execute(InvocationContext invocationContext, RequestContext requestContext) throws Exception;

    boolean isWatchEnabled(ConfigurationProperty property);

    void watch(ConfigurationProperty property);

    void removeWatch(ConfigurationProperty property);

    void updateConfigurations();

    Optional<ConfigurationProperty> findConfig(FaultToleranceType type, String propertyPath);

    Optional<ConfigurationProperty> findConfig(String groupKey, FaultToleranceType type, String propertyPath);

    Optional<ConfigurationProperty> findConfig(String commandKey, String groupKey, FaultToleranceType type, String propertyPath);

}
