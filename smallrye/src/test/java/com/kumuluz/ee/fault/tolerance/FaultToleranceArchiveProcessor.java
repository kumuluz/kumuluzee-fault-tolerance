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

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import java.util.logging.Logger;

/**
 * Adds Hystrix specific configuration to the archive and makes sure that beans.xml is present.
 *
 * @author Urban Malc
 * @since 2.0.0
 */
public class FaultToleranceArchiveProcessor implements ApplicationArchiveProcessor {

    Logger log = Logger.getLogger(FaultToleranceArchiveProcessor.class.getName());

    @Override
    public void process(Archive<?> archive, TestClass testClass) {

        archive.as(WebArchive.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");

        if (testClass.getName().equals("org.eclipse.microprofile.fault.tolerance.tck.bulkhead.BulkheadAsynchRetryTest")){
            archive.as(WebArchive.class).addAsResource("config.properties");
        }

    }
}
