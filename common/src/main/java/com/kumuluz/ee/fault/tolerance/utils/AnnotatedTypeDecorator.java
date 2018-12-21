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
package com.kumuluz.ee.fault.tolerance.utils;

import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Adds annotations to AnnotatedType.
 *
 * @author Urban Malc
 * @since 1.1.0
 */
public class AnnotatedTypeDecorator<X, A extends Annotation> implements AnnotatedType<X> {

    private final AnnotatedType<X> decoratedType;
    private final Class<A> annotationClass;
    private Annotation decoratingAnnotation;

    private final Set<Annotation> annotations;

    public AnnotatedTypeDecorator(AnnotatedType<X> decoratedType,
                                  Class<A> annotationClass, Annotation decoratingAnnotation) {
        this.decoratedType = decoratedType;
        this.annotationClass = annotationClass;
        this.decoratingAnnotation = decoratingAnnotation;

        Set<Annotation> annotations = new HashSet<>(decoratedType.getAnnotations());
        annotations.add(decoratingAnnotation);
        this.annotations = Collections.unmodifiableSet(annotations);
    }

    @Override
    public Class<X> getJavaClass() {
        return decoratedType.getJavaClass();
    }

    @Override
    public Set<AnnotatedConstructor<X>> getConstructors() {
        return decoratedType.getConstructors();
    }

    @Override
    public Set<AnnotatedMethod<? super X>> getMethods() {
        return decoratedType.getMethods();
    }

    @Override
    public Set<AnnotatedField<? super X>> getFields() {
        return decoratedType.getFields();
    }

    @Override
    public Type getBaseType() {
        return decoratedType.getBaseType();
    }

    @Override
    public Set<Type> getTypeClosure() {
        return decoratedType.getTypeClosure();
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> aClass) {
        if (annotationClass.equals(aClass)) {
            return (T) decoratingAnnotation;
        }
        return decoratedType.getAnnotation(aClass);
    }

    @Override
    public Set<Annotation> getAnnotations() {

        return annotations;
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> aClass) {
        if (aClass.equals(annotationClass)) {
            return true;
        }

        return decoratedType.isAnnotationPresent(aClass);
    }
}