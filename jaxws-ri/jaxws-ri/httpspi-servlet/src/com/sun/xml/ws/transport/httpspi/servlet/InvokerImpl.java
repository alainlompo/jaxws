/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.xml.ws.transport.httpspi.servlet;

import javax.xml.ws.spi.Invoker;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.WebServiceException;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.annotation.Annotation;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

/**
 * @author Jitendra Kotamraju
 */
class InvokerImpl extends Invoker {
    private final Class implType;
    private final Object impl;
    private final Method postConstructMethod;
    private final Method preDestroyMethod;

    InvokerImpl(Class implType) {
        this.implType = implType;
        postConstructMethod = findAnnotatedMethod(implType, PostConstruct.class);
        preDestroyMethod = findAnnotatedMethod(implType, PreDestroy.class);
        try {
            impl = implType.newInstance();
        } catch (InstantiationException e) {
            throw new WebServiceException(e);
        } catch (IllegalAccessException e) {
            throw new WebServiceException(e);
        }
    }

    /*
     * Helper for invoking a method with elevated privilege.
     */
    private static void invokeMethod(final Method method, final Object instance, final Object... args) {
        if(method==null)    return;
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                try {
                    if (!method.isAccessible()) {
                        method.setAccessible(true);
                    }
                    method.invoke(instance,args);
                } catch (IllegalAccessException e) {
                    throw new WebServiceException(e);
                } catch (InvocationTargetException e) {
                    throw new WebServiceException(e);
                }
                return null;
            }
        });
    }

    public void inject(WebServiceContext webServiceContext) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        buildInjectionPlan(
            implType, WebServiceContext.class,false).inject(impl,webServiceContext);
        invokeMethod(postConstructMethod, impl);
    }

    public Object invoke(Method m, Object... args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        return m.invoke(impl, args);
    }

    /**
     * Encapsulates which field/method the injection is done,
     * and performs the injection.
     */
    private static interface InjectionPlan<T,R> {
        void inject(T instance,R resource);
        /**
         * Gets the number of injections to be performed.
         */
        int count();
    }

    /*
     * Injects to a field.
     */
    private static class FieldInjectionPlan<T,R> implements InjectionPlan<T,R> {
        private final Field field;

        public FieldInjectionPlan(Field field) {
            this.field = field;
        }

        public void inject(final T instance, final R resource) {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {
                    try {
                        if (!field.isAccessible()) {
                            field.setAccessible(true);
                        }
                        field.set(instance,resource);
                        return null;
                    } catch (IllegalAccessException e) {
                        throw new WebServiceException(e);
                    }
                }
            });
        }

        public int count() {
            return 1;
        }
    }

    /*
     * Injects to a method.
     */
    private static class MethodInjectionPlan<T,R> implements InjectionPlan<T,R> {
        private final Method method;

        public MethodInjectionPlan(Method method) {
            this.method = method;
        }

        public void inject(T instance, R resource) {
            invokeMethod(method, instance, resource);
        }

        public int count() {
            return 1;
        }
    }

    /*
     * Combines multiple {@link InjectionPlan}s into one.
     */
    private static class Compositor<T,R> implements InjectionPlan<T,R> {
        private final InjectionPlan<T,R>[] children;

        public Compositor(Collection<InjectionPlan<T,R>> children) {
            this.children = children.toArray(new InjectionPlan[children.size()]);
        }

        public void inject(T instance, R res) {
            for (InjectionPlan<T,R> plan : children)
                plan.inject(instance,res);
        }

        public int count() {
            int r = 0;
            for (InjectionPlan<T, R> plan : children)
                r += plan.count();
            return r;
        }
    }

    /*
     * Finds the method that has the given annotation, while making sure that
     * there's only at most one such method.
     */
    private static Method findAnnotatedMethod(Class clazz, Class<? extends Annotation> annType) {
        boolean once = false;
        Method r = null;
        for(Method method : clazz.getDeclaredMethods()) {
            if (method.getAnnotation(annType) != null) {
                if (once)
                    throw new WebServiceException("Only one method should have the annotation"+annType);
                if (method.getParameterTypes().length != 0)
                    throw new WebServiceException("Method"+method+"shouldn't have any arguments");
                r = method;
                once = true;
            }
        }
        return r;
    }

    /*
     * Creates an {@link InjectionPlan} that injects the given resource type to the given class.
     *
     * @param isStatic
     *      Only look for static field/method
     *
     */
    private static <T,R>
    InjectionPlan<T,R> buildInjectionPlan(Class<? extends T> clazz, Class<R> resourceType, boolean isStatic) {
        List<InjectionPlan<T,R>> plan = new ArrayList<InjectionPlan<T,R>>();

        Class<?> cl = clazz;
        while(cl != Object.class) {
            for(Field field: cl.getDeclaredFields()) {
                Resource resource = field.getAnnotation(Resource.class);
                if (resource != null) {
                    if(isInjectionPoint(resource, field.getType(),
                        "Incorrect type for field"+field.getName(),
                        resourceType)) {

                        if(isStatic && !Modifier.isStatic(field.getModifiers()))
                            throw new WebServiceException("Static resource "+resourceType+" cannot be injected to non-static "+field);

                        plan.add(new FieldInjectionPlan<T,R>(field));
                    }
                }
            }
            cl = cl.getSuperclass();
        }

        cl = clazz;
        while(cl != Object.class) {
            for(Method method : cl.getDeclaredMethods()) {
                Resource resource = method.getAnnotation(Resource.class);
                if (resource != null) {
                    Class[] paramTypes = method.getParameterTypes();
                    if (paramTypes.length != 1)
                        throw new WebServiceException("Incorrect no of arguments for method "+method);
                    if(isInjectionPoint(resource,paramTypes[0],
                        "Incorrect argument types for method"+method.getName(),
                        resourceType)) {

                        if(isStatic && !Modifier.isStatic(method.getModifiers()))
                            throw new WebServiceException("Static resource "+resourceType+" cannot be injected to non-static "+method);

                        plan.add(new MethodInjectionPlan<T,R>(method));
                    }
                }
            }
            cl = cl.getSuperclass();
        }

        return new Compositor<T,R>(plan);
    }

    /*
     * Returns true if the combination of {@link Resource} and the field/method type
     * are consistent for {@link WebServiceContext} injection.
     */
    private static boolean isInjectionPoint(Resource resource, Class fieldType, String errorMessage, Class resourceType ) {
        Class t = resource.type();
        if (t.equals(Object.class)) {
            return fieldType.equals(resourceType);
        } else if (t.equals(resourceType)) {
            if (fieldType.isAssignableFrom(resourceType)) {
                return true;
            } else {
                // type compatibility error
                throw new WebServiceException(errorMessage);
            }
        }
        return false;
    }
}
