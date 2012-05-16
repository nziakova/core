/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.weld.injection;

import static org.jboss.weld.logging.messages.UtilMessage.QUALIFIER_ON_FINAL_FIELD;
import static org.jboss.weld.util.collections.WeldCollections.immutableList;
import static org.jboss.weld.util.collections.WeldCollections.immutableSet;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionPoint;
import javax.inject.Inject;

import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedCallable;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedConstructor;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedField;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedMethod;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedParameter;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedType;
import org.jboss.weld.bootstrap.events.ProcessInjectionPointImpl;
import org.jboss.weld.ejb.EJBApiAbstraction;
import org.jboss.weld.exceptions.DefinitionException;
import org.jboss.weld.injection.attributes.FieldInjectionPointAttributes;
import org.jboss.weld.injection.attributes.InferingFieldInjectionPointAttributes;
import org.jboss.weld.injection.attributes.InferingParameterInjectionPointAttributes;
import org.jboss.weld.injection.attributes.ParameterInjectionPointAttributes;
import org.jboss.weld.injection.attributes.SpecialParameterInjectionPoint;
import org.jboss.weld.injection.spi.EjbInjectionServices;
import org.jboss.weld.injection.spi.JpaInjectionServices;
import org.jboss.weld.injection.spi.ResourceInjectionServices;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.persistence.PersistenceApiAbstraction;
import org.jboss.weld.util.Beans;
import org.jboss.weld.util.collections.ArraySet;

/**
 * Factory class that producer {@link InjectionPoint} instances for fields, parameters, methods and constructors. The
 * {@link ProcessInjectionPoint} event is fired for each created injection point unless the {@link #silentInstance()} is used.
 *
 * @author Jozef Hartinger
 *
 */
public class InjectionPointFactory {

    private InjectionPointFactory() {
    }

    private static final InjectionPointFactory INSTANCE = new InjectionPointFactory();
    private static final InjectionPointFactory SILENT_INSTANCE = new InjectionPointFactory() {

        @Override
        protected <T, X> FieldInjectionPointAttributes<T, X> processInjectionPoint(FieldInjectionPointAttributes<T, X> injectionPointAttributes, BeanManagerImpl manager) {
            // NOOP
            return injectionPointAttributes;
        }

        @Override
        protected <T, X> ParameterInjectionPointAttributes<T, X> processInjectionPoint(ParameterInjectionPointAttributes<T, X> injectionPointAttributes, BeanManagerImpl manager) {
            // NOOP
            return injectionPointAttributes;
        }
    };

    /**
     * Returns the default {@link InjectionPointFactory} singleton.
     * @return the default {@link InjectionPointFactory} singleton
     */
    public static InjectionPointFactory instance() {
        return INSTANCE;
    }

    /**
     * Returns an {@link InjectionPointFactory} instance that never produces a {@link ProcessInjectionPoint} event. This is used
     * for creating observer method injection points of extensions and proxy classes.
     * @return an {@link InjectionPointFactory} instance
     */
    public static InjectionPointFactory silentInstance() {
        return SILENT_INSTANCE;
    }

    /**
     * Notifies CDI extension of a given {@link InjectionPoint}.
     */
    protected <T, X> FieldInjectionPointAttributes<T, X> processInjectionPoint(FieldInjectionPointAttributes<T, X> injectionPointAttributes, BeanManagerImpl manager) {
        return ProcessInjectionPointImpl.fire(injectionPointAttributes, manager);
    }

    /**
     * Notifies CDI extension of a given {@link InjectionPoint}.
     */
    protected <T, X> ParameterInjectionPointAttributes<T, X> processInjectionPoint(ParameterInjectionPointAttributes<T, X> injectionPointAttributes, BeanManagerImpl manager) {
        return ProcessInjectionPointImpl.fire(injectionPointAttributes, manager);
    }

    /*
     * Creation of basic InjectionPoints
     */

    /**
     * Creates a new {@link FieldInjectionPoint} and fires the {@link ProcessInjectionPoint} event.
     * @param field
     * @param declaringBean
     * @param declaringComponentClass used for resolution of type variables of the injection point type
     * @param manager
     * @return
     */
    public <T, X> FieldInjectionPoint<T, X> createFieldInjectionPoint(EnhancedAnnotatedField<T, X> field, Bean<?> declaringBean, Class<?> declaringComponentClass, BeanManagerImpl manager) {
        FieldInjectionPointAttributes<T, X> attributes = InferingFieldInjectionPointAttributes.of(field, declaringBean, declaringComponentClass, manager);
        attributes = processInjectionPoint(attributes, manager);
        return new FieldInjectionPoint<T, X>(attributes);
    }

    /**
     * Creates a new {@link ParameterInjectionPoint} and fires the {@link ProcessInjectionPoint} event.
     * @param parameter
     * @param declaringBean
     * @param declaringComponentClass used for resolution of type variables of the injection point type
     * @param manager
     * @return
     */
    public <T, X> ParameterInjectionPoint<T, X> createParameterInjectionPoint(EnhancedAnnotatedParameter<T, X> parameter, Bean<?> declaringBean,
            Class<?> declaringComponentClass, BeanManagerImpl manager) {
        ParameterInjectionPointAttributes<T, X> attributes = InferingParameterInjectionPointAttributes.of(parameter, declaringBean, declaringComponentClass, manager);
        attributes = processInjectionPoint(attributes, manager);
        return new ParameterInjectionPointImpl<T, X>(attributes);
    }

    /*
     * Creation of callable InjectionPoints
     */

    public <T> ConstructorInjectionPoint<T> createConstructorInjectionPoint(Bean<T> declaringBean, EnhancedAnnotatedType<T> type, BeanManagerImpl manager) {
        EnhancedAnnotatedConstructor<T> constructor = Beans.getBeanConstructor(type);
        return createConstructorInjectionPoint(declaringBean, type.getJavaClass(), constructor, manager);
    }

    public <T> ConstructorInjectionPoint<T> createConstructorInjectionPoint(Bean<T> declaringBean, Class<?> declaringComponentClass, EnhancedAnnotatedConstructor<T> constructor, BeanManagerImpl manager) {
        return new ConstructorInjectionPoint<T>(constructor, declaringBean, declaringComponentClass, this, manager);
    }

    public <T, X> MethodInjectionPoint<T, X> createMethodInjectionPoint(EnhancedAnnotatedMethod<T, X> enhancedMethod, Bean<?> declaringBean, Class<?> declaringComponentClass, boolean observerOrDisposer, BeanManagerImpl manager) {
        return new MethodInjectionPoint<T, X>(enhancedMethod, declaringBean, declaringComponentClass, observerOrDisposer, this, manager);
    }

    /*
     * Utility methods for field InjectionPoints
     */
    public List<Set<FieldInjectionPoint<?, ?>>> getFieldInjectionPoints(Bean<?> declaringBean, EnhancedAnnotatedType<?> type, BeanManagerImpl manager) {
        List<Set<FieldInjectionPoint<?, ?>>> injectableFieldsList = new ArrayList<Set<FieldInjectionPoint<?, ?>>>();
        EnhancedAnnotatedType<?> t = type;
        while (t != null && !t.getJavaClass().equals(Object.class)) {
            ArraySet<FieldInjectionPoint<?, ?>> fields = new ArraySet<FieldInjectionPoint<?, ?>>();
            for (EnhancedAnnotatedField<?, ?> annotatedField : t.getDeclaredEnhancedFields(Inject.class)) {
                if (!annotatedField.isStatic()) {
                    addFieldInjectionPoint(annotatedField, fields, declaringBean, type.getJavaClass(), manager);
                }
            }
            injectableFieldsList.add(0, immutableSet(fields));
            t = t.getEnhancedSuperclass();
        }
        return immutableList(injectableFieldsList);
    }

    private void addFieldInjectionPoint(EnhancedAnnotatedField<?, ?> annotatedField, Set<FieldInjectionPoint<?, ?>> injectableFields, Bean<?> declaringBean, Class<?> declaringComponentClass, BeanManagerImpl manager) {
        if (!annotatedField.isAnnotationPresent(Produces.class)) {
            if (annotatedField.isFinal()) {
                throw new DefinitionException(QUALIFIER_ON_FINAL_FIELD, annotatedField);
            }
            injectableFields.add(createFieldInjectionPoint(annotatedField, declaringBean, declaringComponentClass, manager));
        }
    }

    private Set<WeldInjectionPoint<?, ?>> getFieldInjectionPointsWithSpecialAnnotation(Bean<?> declaringBean, EnhancedAnnotatedType<?> type, Class<? extends Annotation> annotationType, BeanManagerImpl manager) {
        ArraySet<WeldInjectionPoint<?, ?>> injectionPoints = new ArraySet<WeldInjectionPoint<?, ?>>();
        for (EnhancedAnnotatedField<?, ?> field : type.getEnhancedFields(annotationType)) {
            injectionPoints.add(createFieldInjectionPoint(field, declaringBean, type.getJavaClass(), manager));
        }
        return immutableSet(injectionPoints);
    }

    public Set<WeldInjectionPoint<?, ?>> getEjbInjectionPoints(Bean<?> declaringBean, EnhancedAnnotatedType<?> type, BeanManagerImpl manager) {
        if (manager.getServices().contains(EjbInjectionServices.class)) {
            Class<? extends Annotation> ejbAnnotationType = manager.getServices().get(EJBApiAbstraction.class).EJB_ANNOTATION_CLASS;
            return getFieldInjectionPointsWithSpecialAnnotation(declaringBean, type, ejbAnnotationType, manager);
        } else {
            return Collections.emptySet();
        }
    }

    public Set<WeldInjectionPoint<?, ?>> getPersistenceContextInjectionPoints(Bean<?> declaringBean, EnhancedAnnotatedType<?> type, BeanManagerImpl manager) {
        if (manager.getServices().contains(JpaInjectionServices.class)) {
            Class<? extends Annotation> persistenceContextAnnotationType = manager.getServices().get(PersistenceApiAbstraction.class).PERSISTENCE_CONTEXT_ANNOTATION_CLASS;
            return getFieldInjectionPointsWithSpecialAnnotation(declaringBean, type, persistenceContextAnnotationType, manager);
        } else {
            return Collections.emptySet();
        }
    }

    public Set<WeldInjectionPoint<?, ?>> getPersistenceUnitInjectionPoints(Bean<?> declaringBean, EnhancedAnnotatedType<?> type, BeanManagerImpl manager) {
        if (manager.getServices().contains(JpaInjectionServices.class)) {
            Class<? extends Annotation> persistenceUnitAnnotationType = manager.getServices().get(PersistenceApiAbstraction.class).PERSISTENCE_UNIT_ANNOTATION_CLASS;
            return getFieldInjectionPointsWithSpecialAnnotation(declaringBean, type, persistenceUnitAnnotationType, manager);
        } else {
            return Collections.emptySet();
        }
    }

    public Set<WeldInjectionPoint<?, ?>> getResourceInjectionPoints(Bean<?> declaringBean, EnhancedAnnotatedType<?> type, BeanManagerImpl manager) {
        if (manager.getServices().contains(ResourceInjectionServices.class)) {
            Class<? extends Annotation> resourceAnnotationType = manager.getServices().get(EJBApiAbstraction.class).RESOURCE_ANNOTATION_CLASS;
            return getFieldInjectionPointsWithSpecialAnnotation(declaringBean, type, resourceAnnotationType, manager);
        } else {
            return Collections.emptySet();
        }
    }

    /*
     * Utility methods for parameter InjectionPoints
     */

    public <X> List<ParameterInjectionPoint<?, X>> getParameterInjectionPoints(EnhancedAnnotatedCallable<?, X, ?> callable, Bean<?> declaringBean, Class<?> declaringComponentClass, BeanManagerImpl manager, boolean observerOrDisposer) {
        List<ParameterInjectionPoint<?, X>> parameters = new ArrayList<ParameterInjectionPoint<?, X>>();

        /*
         * bean that the injection point belongs to
         * this is null for observer and disposer methods
         */
        Bean<?> bean = null;
        if (!observerOrDisposer) {
            bean = declaringBean;
        }

        for (EnhancedAnnotatedParameter<?, X> parameter : callable.getEnhancedParameters()) {
            if (isSpecialParameter(parameter)) {
                parameters.add(SpecialParameterInjectionPoint.of(parameter, bean, declaringBean.getBeanClass(), manager));
            } else {
                parameters.add(createParameterInjectionPoint(parameter, bean, declaringComponentClass, manager));
            }
        }
        return immutableList(parameters);
    }

    private boolean isSpecialParameter(EnhancedAnnotatedParameter<?, ?> parameter) {
        return parameter.isAnnotationPresent(Disposes.class) || parameter.isAnnotationPresent(Observes.class);
    }
}
