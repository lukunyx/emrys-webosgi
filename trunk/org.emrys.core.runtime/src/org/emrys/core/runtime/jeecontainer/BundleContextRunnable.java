/*******************************************************************************
 * Copyright (c) 2011 Hirisun Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Hirisun License v1.0
 * which accompanies this distribution, and is available at
 * http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.jeecontainer;

import org.eclipse.core.runtime.IStatus;
import org.emrys.core.runtime.classloader.BundledClassLoaderFactory;
import org.emrys.core.runtime.internal.FwkRuntime;
import org.emrys.core.runtime.jeewrappers.BundledHttpServletRequestWrapper;


/**
 * 调用bundled Api的封装类，该类中会帮调用者用bundle上下文的classloader替换当前线程的上下文ClassLoader.
 * 比如在调用到某个Web
 * Bundle中的Servlet、Listener、Filter时，霄1�7要替换当前线程上下文ClassLoader，则可以使用此类来封装调用的方法〄1�7
 * 
 * @author Leo Chang - Hirisun
 * @version 2011-4-6
 */
public abstract class BundleContextRunnable implements Runnable {

	/**
	 * the context bundle
	 */
	protected IBundledServletContext ctx;
	/**
	 * the result of execution.
	 */
	private IStatus result;
	private final OSGiJEEContainer jeeContainer;
	private final boolean switchReqPath;
	private final boolean switchCtxCL;

	/**
	 * get execution result.
	 * 
	 * @return
	 */
	public IStatus getResult() {
		return result;
	}

	public BundleContextRunnable(IBundledServletContext ctx,
			boolean switchReqPath, boolean switchCtxCL) {
		this.ctx = ctx;
		this.jeeContainer = FwkRuntime.getInstance().getJeeContainer();
		this.switchReqPath = switchReqPath;
		this.switchCtxCL = switchCtxCL;
	}

	public BundleContextRunnable(IBundledServletContext ctx) {
		this.ctx = ctx;
		this.jeeContainer = FwkRuntime.getInstance().getJeeContainer();
		this.switchReqPath = false;
		this.switchCtxCL = true;
	}

	public void setBundleCtx(IBundledServletContext ctx) {
		this.ctx = ctx;
	}

	public final void run() {
		ClassLoader originalCtxClassLoader = null;
		ClassLoader classLoader = null;

		if (switchCtxCL) {
			originalCtxClassLoader = Thread.currentThread()
					.getContextClassLoader();
			classLoader = BundledClassLoaderFactory
					.getBundledJeeClassLoader(ctx.getBundle());
		}

		BundledHttpServletRequestWrapper topReq = null;
		String originalServletPath = null;

		if (switchReqPath) {
			topReq = (BundledHttpServletRequestWrapper) jeeContainer
					.getReqThreadVariants().get(
							OSGiJEEContainer.THREAD_V_REQUEST);
			originalServletPath = topReq.getServletPath();
			// String originalPathInfo = topReq.getPathInfo();
		}

		try {
			if (switchCtxCL) {
				Thread.currentThread().setContextClassLoader(classLoader);
			}
			if (switchReqPath) {
				String bundlePrefix = ctx.getBundleActivator()
						.getServiceNSPrefix();
				if (originalServletPath.startsWith("/" + bundlePrefix))
					topReq.setServletPath(originalServletPath.replaceFirst("/"
							+ bundlePrefix, ""));
				// topReq.setPathInfo(originalPathInfo);
			}

			result = execute();
		} finally {
			if (switchReqPath) {
				topReq.setServletPath(originalServletPath);
				// topReq.setPathInfo(originalPathInfo);
			}
			if (switchReqPath) {
				Thread.currentThread().setContextClassLoader(
						originalCtxClassLoader);
			}
		}
	}

	/**
	 * the executive work here.
	 * 
	 * @return the result statuts.
	 */
	protected abstract IStatus execute();
}
