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
package com.kumuluz.ee.circuit.breaker.commands;

import com.kumuluz.ee.circuit.breaker.models.ExecutionMetadata;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.exception.HystrixBadRequestException;

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

    private final ExecutionMetadata metadata;
    private final InvocationContext ic;

    public HystrixGenericCommand(HystrixCommand.Setter setter, InvocationContext ic, ExecutionMetadata metadata) {

        super(setter);

        this.ic = ic;
        this.metadata = metadata;
    }

    @Override
    protected Object run() throws Exception {

        log.finest("Run method called for " + ic);

        Object result;

        try {
            result = ic.proceed();
        } catch (Throwable e) {
            if (isFallbackInvokeable(e))
                throw e;

            throw new HystrixBadRequestException(e.getMessage(), e);
        }

        return result;
    }

    @Override
    protected Object getFallback() {

        log.finest("Calling fallback method " + metadata.getFallbackMethod());

        Exception executionException = getExceptionFromThrowable(getExecutionException());

        if (executionException != null)
            log.finest("Callback fired by " + executionException.getClass().getName());

        try {
            return metadata.getFallbackMethod()
                    .invoke(ic.getTarget(),
                            ic.getParameters());
        } catch (IllegalAccessException e) {
            log.severe("Exception occured while trying to invoke fallback method: " + e.getClass().getName());
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            log.severe("Exception occured while trying to invoke fallback method: " + e.getClass().getName());
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
