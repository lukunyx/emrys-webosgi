package org.emrys.webosgi.core.classloader;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.osgi.framework.adaptor.BundleClassLoader;
import org.emrys.webosgi.common.ComActivator;
import org.emrys.webosgi.common.ComponentCore;
import org.emrys.webosgi.common.util.BundleProxyClassLoader;
import org.emrys.webosgi.core.service.IWABServletContext;

import sun.reflect.Reflection;

/**
 * In JEE Container, all servlet resource(like filter, listener, serlvet, jsp
 * file, etc.), should be load and invoked in a customized classloader which
 * allows to load server's classes maybe. This classloader wrappers a bundle as
 * the classloader with highest priority, and set the Thread Context Classloader
 * as parent({@link org.eclipse.core.runtime.internal.adaptor.ContextFinder} has
 * the server's classloader as its parent).
 * 
 * @author Leo Chang
 * @version 2011-6-3
 */
public class BundledJeeContextClassLoader extends BundleProxyClassLoader {
	private static final String WAB_CLASSPATH_ROOT = "WEB-INF/classes/";
	/**
	 * Backing bundle's class loader
	 */
	private ClassLoader backingBundleCL;
	private final IWABServletContext wabServletCtx;

	/**
	 * 
	 * @param wabServletCtx
	 *            the WAB Servlet Context to load class and resource with at
	 *            first.
	 */
	public BundledJeeContextClassLoader(IWABServletContext wabServletCtx) {
		// Use OSGi framework's Context CL as parent. This bundle's class
		// loader's parent CL.
		this(wabServletCtx, BundledJeeContextClassLoader.class.getClassLoader()
				.getParent());
	}

	/**
	 * @param wabServletCtx
	 *            WAB bundle servlet context to load class and resource with at
	 *            first.
	 * @param parentCL
	 *            parent Class Loader if failed load class and resource form
	 *            Bundle.
	 */
	public BundledJeeContextClassLoader(IWABServletContext wabServletCtx,
			ClassLoader parentCL) {
		super(wabServletCtx.getBundle(), null, parentCL);
		this.wabServletCtx = wabServletCtx;
		// The OSGi specied WAB may have no Web ComActivator.
		ComActivator backingBundleActivator = ComponentCore.getInstance()
				.getBundleActivator(getBundle().getBundleId());
		if (backingBundleActivator != null)
			backingBundleCL = backingBundleActivator.getClass()
					.getClassLoader();
	}

	@Override
	protected URL findResource(String name) {
		// Because the WAB Classes Root has been set as Bundle-ClassPath header,
		// if the name of class-path resource contains "WEB-INF/classes" use the
		// segments behind. This check may solve some problem when Spring try to
		// load class-path configure file. And if some WAB has many class-path
		// resources in "WEB-INF/classes" to load, it's take a really long time
		// to obtain the resource by invoke method Bundle.getResource(name).
		// Here we search for the resource in "WEB-INF/classes" directory at
		// first.
		int i = 0;
		if ((i = name.indexOf(WAB_CLASSPATH_ROOT)) > -1) {
			try {
				URL url = wabServletCtx.getResource(name.substring(i));
				if (url != null)
					return url;
			} catch (MalformedURLException e) {
			}

			name = name.substring(i + WAB_CLASSPATH_ROOT.length());
		}

		return super.findResource(name);
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		return super.loadClass(name);
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve)
			throws ClassNotFoundException {
		// The classical problem is the
		// "java.lang.LinkageError: loader constraint violation"
		// excepiton when Spring2.5.6 try to generate
		// "org.aopalliance.aop.Advice" interface by JDK
		// Proxy mechanism. If a bundle dependent Spring2.5.6 bundles and these
		// bundle has aop jars,
		// spring try to generate proxy class by ThreadContextClassLoader, this
		// problem will occur.
		// Here we can find caller class is from another bundle, try
		// to load class from that bundle at first. This let the caller's bundle
		// has high priorityto find. The Equinox
		// BuddyPolicy loading can solve some problem like this if the third
		// bundle try to load class with its bundle classoader.
		// Eclipse-BuddyPolicy: registered
		// Eclipse-RegisterBuddy: org.opengoss.orm.hibernate
		/*Class<?> clazz = searchCallerBundle(name);
		if (clazz != null) {
			if (resolve)
				resolveClass(clazz);
			return clazz;
		}*/

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
