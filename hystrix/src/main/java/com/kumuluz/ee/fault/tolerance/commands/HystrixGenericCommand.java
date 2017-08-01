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

import com.kumuluz.ee.fault.tolerance.models.ExecutionMetadata;
import com.netflix.config.ConfigurationManager;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import org.jboss.weld.context.RequestContext;

import javax.interceptor.InvocationContext;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

/**
 * Hystrix generic command for wrapping method execution in circuit breaker
 *
 * @author Luka Šarc
 */
public class HystrixGenericCommand extends HystrixCommand<Object> {

    private static final Logger log = Logger.getLogger(HystrixGenericCommand.class.getName());

    private final InvocationContext invocationContext;
    private final RequestContext requestContext;
    private final ExecutionMetadata metadata;

    public HystrixGenericCommand(HystrixCommand.Setter setter, InvocationContext invocationContext, RequestContext requestContext, ExecutionMetadata metadata) {

        super(setter);

        this.invocationContext = invocationContext;
        this.requestContext = requestContext;
        this.metadata = metadata;
    }

    @Override
    protected Object run() throws Exception {

        log.finest("Executin command '" + metadata.getCommandKey() + "'.");

        Object property = ConfigurationManager.getConfigInstance()
                .getProperty("hystrix.command." + metadata.getCommandKey() + ".execution.isolation.strategy");
        Object result;

        boolean threadExecution = property == null || property == HystrixCommandProperties.ExecutionIsolationStrategy.THREAD;

        try {
            if (threadExecution && !requestContext.isActive())
                requestContext.activate();

            result = invocationContext.proceed();

            if (threadExecution && requestContext.isActive())
                requestContext.deactivate();
        } catch (Throwable e) {
            if (isFallbackInvokeable(e))
                throw e;

            throw new HystrixBadRequestException(e.getMessage(), e);
        }

        return result;
    }

    @Override
    protected Object getFallback() {

        log.finest("Executing fallback for command '" + metadata.getCommandKey() + "'.");

        Exception executionException = getExceptionFromThrowable(getExecutionException());

        if (executionException != null) {
            log.finest("Callback for command '" + metadata.getCommandKey() + "' fired by " +
                    executionException.getClass().getName());
        }

        try {
            return metadata.getFallbackMethod()
                    .invoke(invocationContext.getTarget(),
                            invocationContext.getParameters());
        } catch (IllegalAccessException|InvocationTargetException e) {
            log.severe("Exception occured while trying to invoke fallback method for key '" +
                    metadata.getCommandKey() + "': " + e.getClass().getName());
            e.printStackTrace();
        }

        return null;
    }

    private boolean isFallbackInvokeable(Throwable e) {

        Class[] skipFallbackClasses = metadata.getSkipFallbackExceptions();

        if (skipFallbackClasses != null) {
            for (Class<?> fe : metadata.getSkipFallbackExceptions()) {
                if (fe.isInstance(e))
                    return false;
            }
        }

        return true;
    }

}
