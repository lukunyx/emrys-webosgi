package org.emrys.webosgi.core.classloader;

import java.util.Hashtable;
import java.util.Map;

import org.emrys.webosgi.core.service.IWABServletContext;
import org.osgi.framework.Bundle;

/**
 * In Web Container, all Servlet Resources(like Filter, Listener, Servlet, jsp
 * file, etc.), should be load and invoked in a context class loader which
 * allows to load WAB bundle or some framework resource. This class loader
 * factory initialized and buffered context class loader for each web bundle.
 * 
 * @author Leo Chang
 * @version 2011-1-13
 */
public class WabClassLoaderFactory {
	private static Map<Bundle, ClassLoader> wbCtxClassLoaders = new Hashtable<Bundle, ClassLoader>();

	/**
	 * Get buffered or generate the WAB Context Class Loader for the given wab
	 * bundle.
	 * 
	 * @param bundle
	 * @return
	 */
	public static ClassLoader getWabClassLoader(IWABServletContext wabServletCtx) {
		ClassLoader wabClassLoader = wbCtxClassLoaders.get(wabServletCtx
				.getBundle());
		if (wabClassLoader == null) {
			wabClassLoader = new WebAppClassLoader(wabServletCtx);
			wbCtxClassLoaders.put(wabServletCtx.getBundle(), wabClassLoader);
		}
		return wabClassLoader;
	}

	/**
	 * Clear buffered Data
	 */
	public static void clean() {
		wbCtxClassLoaders.clear();
		wbCtxClassLoaders = null;
	}
}
