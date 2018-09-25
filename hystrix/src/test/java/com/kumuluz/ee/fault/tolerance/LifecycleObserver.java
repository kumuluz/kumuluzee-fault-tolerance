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
package com.kumuluz.ee.fault.tolerance;

import com.kumuluz.ee.fault.tolerance.utils.DeploymentValidator;
import com.netflix.hystrix.HystrixGenericCommand;
import org.jboss.arquillian.container.spi.event.container.BeforeDeploy;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * Adds fault tolerance library to the deployment.
 *
 * @author Urban Malc
 * @since 1.1.0
 */
public class LifecycleObserver {

    public void beforeDeploy(@Observes BeforeDeploy event, TestClass testClass) {
        ((WebArchive)(event.getDeployment().getArchive())).addAsLibrary(buildArchive());
    }

    private Archive<?> buildArchive() {

        return ShrinkWrap.create(JavaArchive.class, "kumuluzee-fault-tolerance.jar")
                .addPackages(true, "com.kumuluz.ee.fault.tolerance")
                .addClass(HystrixGenericCommand.class) // temporary, see class javadoc
                .addAsServiceProvider(com.kumuluz.ee.common.Extension.class, HystrixFaultToleranceExtension.class)
                .addAsServiceProvider(javax.enterprise.inject.spi.Extension.class, DeploymentValidator.class)
                .addAsResource("META-INF/beans.xml");
    }
}
