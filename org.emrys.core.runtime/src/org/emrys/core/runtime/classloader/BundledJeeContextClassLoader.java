/*******************************************************************************
 * Copyright (c) 2011 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.classloader;

import org.eclipse.osgi.framework.adaptor.BundleClassLoader;
import org.emrys.common.ComActivator;
import org.emrys.common.ComponentCore;
import org.emrys.common.util.BundleProxyClassLoader;
import org.osgi.framework.Bundle;

import sun.reflect.Reflection;


/**
 * In JEE Container, all servlet resource(like filter, listener, serlvet, jsp
 * file, etc.), should be load and invoked in a customized classloader which
 * allows to load server's classes maybe. This classloader wrappers a bundle as
 * the classloader with highest priority, and set the Thread Context Classloader
 * as parent({@link org.eclipse.core.runtime.internal.adaptor.ContextFinder} has
 * the server's classloader as its parent).
 * 
 * @author Leo Chang - EMRYS
 * @version 2011-6-3
 */
public class BundledJeeContextClassLoader extends BundleProxyClassLoader {
	/**
	 * Backing bundle's class loader
	 */
	private ClassLoader backingBundleCL;

	/**
	 * 
	 * @param bundle
	 *            the Bundle to load class and resource with at first.
	 */
	public BundledJeeContextClassLoader(Bundle bundle) {
		super(bundle, null, Thread.currentThread().getContextClassLoader());
		ComActivator backingBundleActivator = ComponentCore.getInstance()
				.getBundleActivator(getBundle().getBundleId());
		if (backingBundleActivator != null)
			backingBundleCL = backingBundleActivator.getClass()
					.getClassLoader();
	}

	/**
	 * @param bundle
	 *            bundle the Bundle to load class and resource with at first.
	 * @param parentCL
	 *            parent Class Loader if failed load class and resource form
	 *            Bundle.
	 */
	public BundledJeeContextClassLoader(Bundle bundle, ClassLoader parentCL) {
		super(bundle, null, parentCL);
		ComActivator backingBundleActivator = ComponentCore.getInstance()
				.getBundleActivator(getBundle().getBundleId());
		if (backingBundleActivator != null)
			backingBundleCL = backingBundleActivator.getClass()
					.getClassLoader();
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		return super.loadClass(name);
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve)
			throws ClassNotFoundException {
		// If the caller class is in another bundle, not the backing bundle, try
		// to load class from
		// that bundle at first. This let the caller's bundle has high priority
		// to find.
		// The classical problem is the
		// "java.lang.LinkageError: loader constraint violation"
		// excepiton when Spring2.5.6 try to generate
		// "org.aopalliance.aop.Advice" interface by JDK
		// Proxy mechanism. If a bundle dependent Spring2.5.6 bundles and these
		// bundle has aop jars,
		// spring try to generate proxy class by ThreadContextClassLoader, this
		// problem will occur.
		Class<?> clazz = searchCallerBundle(name);
		if (clazz != null) {
			if (resolve)
				resolveClass(clazz);
			return clazz;
		}

		return super.loadClass(name, resolve);
	}

	/**
	 * Try to find and load class from caller class loader.
	 * 
	 * @return
	 */
	private Class searchCallerBundle(String className) {
		// NOTE: Performance reduce here, some optimization need.
		if (backingBundleCL != null) {
			StackTraceElement[] stackTraceElements = Thread.currentThread()
					.getStackTrace();
			// Find the OSGi Bundle ClassLoader(except this class's bundle, so
			// we visit the stack
			// from 3 level) in caller list.
			ClassLoader callerCL = null;
			for (int i = 3; i < stackTraceElements.length; i++) {
				StackTraceElement se = stackTraceElements[i];
				String stackClassName = se.getClassName();
				if (stackClassName.startsWith("java.")
						|| stackClassName.startsWith("javax.")
						|| stackClassName.startsWith("sun."))
					continue;

				// If this call was load by BundleJspClassLoader, skip.
				if (stackClassName.equals(BundleJspClassLoader.class.getName()))
					return null;

				// NOTE: sun.reflect.Reflection.getCallerClass() is protected
				// not to be accessed in
				// JDK.
				ClassLoader cl = Reflection.getCallerClass(i).getClassLoader();// backingBundleCL.loadClass(stackClassName).getClassLoader();
				if (cl instanceof BundleClassLoader) {
					callerCL = cl;
					break;
				}
			}

			if (callerCL != null && !backingBundleCL.equals(callerCL)) {
				Class clazz = null;
				try {
					clazz = callerCL.loadClass(className);
				} catch (Throwable t) {
					// t.printStackTrace();
				}
				if (clazz != null)
					return clazz;
			}
		}

		return null;
	}
}
