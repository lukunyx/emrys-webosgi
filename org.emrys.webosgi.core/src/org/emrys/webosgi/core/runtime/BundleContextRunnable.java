package org.emrys.webosgi.core.runtime;

import org.eclipse.core.runtime.IStatus;
import org.emrys.webosgi.core.internal.FwkRuntime;
import org.emrys.webosgi.core.jeewrappers.BundledHttpServletRequestWrapper;
import org.emrys.webosgi.core.service.IOSGiWebContainer;
import org.emrys.webosgi.core.service.IWABServletContext;

/**
 * 调用bundled Api的封装类，该类中会帮调用者用bundle上下文的classloader替换当前线程的上下文ClassLoader.
 * 比如在调用到某个Web
 * Bundle中的Servlet、Listener、Filter时，霄1�7要替换当前线程上下文ClassLoader，则可以使用此类来封装调用的方法
 * 〄1�7
 * 
 * @author Leo Chang
 * @version 2011-4-6
 */
public abstract class BundleContextRunnable implements Runnable {

	/**
	 * the context bundle
	 */
	protected IWABServletContext ctx;
	/**
	 * the result of execution.
	 */
	private IStatus result;
	private final IOSGiWebContainer jeeContainer;
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

	public BundleContextRunnable(IWABServletContext ctx, boolean switchReqPath,
			boolean switchCtxCL) {
		this.ctx = ctx;
		this.jeeContainer = FwkRuntime.getInstance().getWebContainer();
		this.switchReqPath = switchReqPath;
		this.switchCtxCL = switchCtxCL;
	}

	public BundleContextRunnable(IWABServletContext ctx) {
		this.ctx = ctx;
		this.jeeContainer = FwkRuntime.getInstance().getWebContainer();
		this.switchReqPath = false;
		this.switchCtxCL = true;
	}

	public void setBundleCtx(IWABServletContext ctx) {
		this.ctx = ctx;
	}

	public final void run() {
		ClassLoader originalCtxClassLoader = null;
		ClassLoader classLoader = null;

		if (switchCtxCL) {
			originalCtxClassLoader = Thread.currentThread()
					.getContextClassLoader();
			classLoader = ctx.getWabClassLoader();
		}

		BundledHttpServletRequestWrapper topReq = null;
		String originalServletPath = null;

		if (switchReqPath) {
			topReq = (BundledHttpServletRequestWrapper) jeeContainer
					.getReqThreadVariants().get(
							OSGiWebContainer.THREAD_V_REQUEST);
			originalServletPath = topReq.getServletPath();
			// String originalPathInfo = topReq.getPathInfo();
		}

		try {
			if (switchCtxCL) {
				Thread.currentThread().setContextClassLoader(classLoader);
			}
			if (switchReqPath) {
				String wabCtxPath = ctx.getWABContextPath();
				if (originalServletPath.startsWith(wabCtxPath))
					topReq.setServletPath(originalServletPath.replaceFirst(
							wabCtxPath, ""));
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
