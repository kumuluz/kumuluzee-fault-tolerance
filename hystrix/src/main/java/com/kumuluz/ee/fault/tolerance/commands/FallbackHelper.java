package com.kumuluz.ee.fault.tolerance.commands;

import com.kumuluz.ee.fault.tolerance.exceptions.FaultToleranceConfigException;
import com.kumuluz.ee.fault.tolerance.exceptions.FaultToleranceException;
import com.kumuluz.ee.fault.tolerance.interfaces.FallbackHandler;
import com.kumuluz.ee.fault.tolerance.models.DefaultFallbackExecutionContext;
import com.kumuluz.ee.fault.tolerance.models.ExecutionMetadata;
import org.jboss.weld.context.RequestContext;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.interceptor.InvocationContext;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

/**
 * Created by luka on 09/09/2017.
 */
public class FallbackHelper {

    private static final Logger log = Logger.getLogger(FallbackHelper.class.getName());

    public static Object executeFallback(Throwable cause, ExecutionMetadata metadata, InvocationContext ic,
                                         RequestContext rc) throws Exception {

        if (cause != null) {
            log.finest("Callback for command '" + metadata.getCommandKey() + "' fired by " +
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
