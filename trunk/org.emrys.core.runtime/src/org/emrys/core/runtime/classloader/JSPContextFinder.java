/*******************************************************************************
 * Copyright (c) 2011 Hirisun Corporation. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Hirisun License v1.0 which accompanies this
 * distribution, and is available at http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.classloader;

import java.io.IOException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.emrys.common.util.BundleProxyClassLoader;
import org.emrys.core.runtime.FwkActivator;


/**
 *This class is a slightly augmented copy of
 * org.eclipse.core.runtime.internal.adaptor.ContextFinder in particular
 * basicFindClassLoaders has been altered to use PackageAdmin to determine if a
 * class originated from a bundle and to skip over the various JspClassloader
 * classes.
 * 
 * @author Leo Chang - Hirisun
 * @version 2011-3-26
 */
public class JSPContextFinder extends ClassLoader implements PrivilegedAction {
	static final class Finder extends SecurityManager {
		@Override
		public Class[] getClassContext() {
			return super.getClassContext();
		}
	}

	/**
	 * This is used to detect cycle that could be caused while delegating the
	 * loading to other classloaders It keeps track on a thread basis of the set
	 * of requested classes and resources
	 */
	private static ThreadLocal cycleDetector = new ThreadLocal();
	static Finder contextFinder;
	static {
		AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				contextFinder = new Finder();
				return null;
			}
		});
	}

	public JSPContextFinder(ClassLoader contextClassLoader) {
		super(contextClassLoader);
	}

	/**
	 * Return a list of all classloaders on the stack that are neither the
	 * JSPContextFinder classloader nor the boot classloader. The last
	 * classloader in the list is either a bundle classloader or the framework's
	 * classloader We assume that the bootclassloader never uses the context
	 * classloader to find classes in itself.
	 * 
	 * @return
	 */
	ArrayList basicFindClassLoaders() {
		Class[] stack = contextFinder.getClassContext();
		ArrayList result = new ArrayList(1);
		ClassLoader previousLoader = null;
		for (int i = 1; i < stack.length; i++) {
			ClassLoader tmp = stack[i].getClassLoader();
			if (checkClass(stack[i]) && tmp != null && tmp != this) {
				if (checkClassLoader(tmp)) {
					if (previousLoader != tmp) {
						result.add(tmp);
						previousLoader = tmp;
					}
				}
				// stop at the framework classloader or the first bundle
				// classloader
				if (FwkActivator.getBundle(stack[i]) != null)
					break;
			}
		}
		return result;
	}

	/**
	 * @param clazz
	 * @return
	 */
	private boolean checkClass(Class clazz) {
		return clazz != JSPContextFinder.class && clazz != BundleProxyClassLoader.class
				&& clazz != BundleJspClassLoader.class;
	}

	/**
	 * ensures that a classloader does not have the JSPContextFinder as part of
	 * the parent hierachy. A classloader which has the JSPContextFinder as a
	 * parent must not be used as a delegate, otherwise we endup in endless
	 * recursion.
	 * 
	 * @param classloader
	 * @return
	 */
	private boolean checkClassLoader(ClassLoader classloader) {
		if (classloader == null || classloader == getParent())
			return false;
		for (ClassLoader parent = classloader.getParent(); parent != null; parent = parent
				.getParent())
			if (parent == this)
				return false;
		return true;
	}

	/**
	 * @return
	 */
	private ArrayList findClassLoaders() {
		if (System.getSecurityManager() == null)
			return basicFindClassLoaders();
		return (ArrayList) AccessController.doPrivileged(this);
	}

	public Object run() {
		return basicFindClassLoaders();
	}

	/**
	 * Return whether the request for loading "name" should proceed. False is
	 * returned when a cycle is being detected
	 * 
	 * @param name
	 * @return
	 */
	private boolean startLoading(String name) {
		Set classesAndResources = (Set) cycleDetector.get();
		if (classesAndResources != null && classesAndResources.contains(name))
			return false;

		if (classesAndResources == null) {
			classesAndResources = new HashSet(3);
			cycleDetector.set(classesAndResources);
		}
		classesAndResources.add(name);
		return true;
	}

	/**
	 * @param name
	 */
	private void stopLoading(String name) {
		((Set) cycleDetector.get()).remove(name);
	}

	@Override
	protected Class loadClass(String arg0, boolean arg1) throws ClassNotFoundException {
		// Shortcut cycle
		if (startLoading(arg0) == false)
			throw new ClassNotFoundException(arg0);

		try {
			ArrayList toConsult = findClassLoaders();
			for (Iterator loaders = toConsult.iterator(); loaders.hasNext();)
				try {
					return ((ClassLoader) loaders.next()).loadClass(arg0);
				} catch (ClassNotFoundException e) {
					// go to the next class loader
				}
			return super.loadClass(arg0, arg1);
		} finally {
			stopLoading(arg0);
		}
	}

	@Override
	public URL getResource(String arg0) {
		// Shortcut cycle
		if (startLoading(arg0) == false)
			return null;
		try {
			ArrayList toConsult = findClassLoaders();
			for (Iterator loaders = toConsult.iterator(); loaders.hasNext();) {
				URL result = ((ClassLoader) loaders.next()).getResource(arg0);
				if (result != null)
					return result;
				// go to the next class loader
			}
			return super.getResource(arg0);
		} finally {
			stopLoading(arg0);
		}
	}

	@Override
	protected Enumeration findResources(String arg0) throws IOException {
		// Shortcut cycle
		if (startLoading(arg0) == false)
			return null;
		try {
			ArrayList toConsult = findClassLoaders();
			for (Iterator loaders = toConsult.iterator(); loaders.hasNext();) {
				Enumeration result = ((ClassLoader) loaders.next()).getResources(arg0);
				if (result != null && result.hasMoreElements())
					return result;
				// go to the next class loader
			}
			return super.findResources(arg0);
		} finally {
			stopLoading(arg0);
		}
	}
}
