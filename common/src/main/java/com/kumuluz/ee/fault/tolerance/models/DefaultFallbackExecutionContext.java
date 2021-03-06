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
package com.kumuluz.ee.fault.tolerance.models;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;

import java.lang.reflect.Method;

/**
 * Default ExecutionContext implementation for fallback
 *
 * @author Luka Šarc
 * @since 1.0.0
 */
public class DefaultFallbackExecutionContext implements ExecutionContext {

    private Method method;
    private Object[] parameters;
    private Throwable failure;

    public void setMethod(Method method) {
        this.method = method;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    @Override
    public Object[] getParameters() {
        return parameters;
    }

    public void setFailiure(Throwable failure) {
        this.failure = failure;
    }

    @Override
    public Throwable getFailure() {
        return failure;
    }
}
