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
package com.kumuluz.ee.fault.tolerance.commands;

import com.kumuluz.ee.fault.tolerance.exceptions.FaultToleranceConfigException;
import com.kumuluz.ee.fault.tolerance.models.DefaultFallbackExecutionContext;
import com.kumuluz.ee.fault.tolerance.models.ExecutionMetadata;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;
import org.jboss.weld.context.RequestContext;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.interceptor.InvocationContext;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

/**
 * Fallback execution helper
 *
 * @author Luka Šarc
 * @since 1.0.0
 */
public class FallbackHelper {

    private static final Logger log = Logger.getLogger(FallbackHelper.class.getName());

    public static Object executeFallback(Throwable cause, ExecutionMetadata metadata, InvocationContext ic,
                                         RequestContext rc) throws Exception {

        if (cause != null) {
            log.finest("Callback for command '" + metadata.getIdentifier() + "' fired by " +
                    cause.getClass().getName());
        }

        boolean rcActivated = false;

        try {
            if (metadata.getFallbackHandlerClass() != null) {

                if (rc != null && !rc.isActive()) {
                    rc.activate();
                    rcActivated = true;
                }

                Instance<? extends FallbackHandler> fallbackCdi = CDI.current().select(metadata.getFallbackHandlerClass());
                FallbackHandler fallbackHandler = fallbackCdi.get();

                DefaultFallbackExecutionContext executionContext = new DefaultFallbackExecutionContext();
                executionContext.setMethod(metadata.getMethod());
                executionContext.setParameters(ic.getParameters());

                Object response = fallbackHandler.handle(executionContext);

                CDI.current().destroy(fallbackCdi);

                return response;
            } else if (metadata.getFallbackMethod() != null) {
                return metadata.getFallbackMethod().invoke(ic.getTarget(),
                        ic.getParameters());
            } else {
                String msg = "Fallback should not be invoked if both fallback mechanisms (" +
                        "fallbackHandler and fallbackMethod) are undefined.";
                log.severe(msg);
                throw new FaultToleranceConfigException(msg);
            }
        } catch (IllegalAccessException|InvocationTargetException e) {
            String msg = "Exception occured while trying to invoke fallback method for key '" +
                    metadata.getCommandKey() + "': " + e.getClass().getName();
            log.severe(msg);
            e.printStackTrace();
            throw new FaultToleranceException(msg, e);
        } finally {
            if (rcActivated && rc.isActive())
                rc.deactivate();
        }
    }

}
