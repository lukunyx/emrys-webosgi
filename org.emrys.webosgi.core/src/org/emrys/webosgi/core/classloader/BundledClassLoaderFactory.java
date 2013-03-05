package org.emrys.webosgi.core.classloader;

import java.net.URLClassLoader;
import java.util.Hashtable;
import java.util.Map;

import org.emrys.webosgi.core.service.IWABServletContext;
import org.osgi.framework.Bundle;

/**
 * In JEE Container, all servlet resource(like filter, listener, serlvet, jsp
 * file, etc.), should be load and invoked in a customized classloader which
 * allows to load server's classes maybe. This classloader factory initialized
 * and buffered bundled proxing classloader for each web bundle.
 * 
 * @author Leo Chang
 * @version 2011-1-13
 */
public class BundledClassLoaderFactory {
	private static Map<Bundle, URLClassLoader> wbJspUrlClassLoaders = new Hashtable<Bundle, URLClassLoader>();
	private static Map<Bundle, ClassLoader> wbCtxClassLoaders = new Hashtable<Bundle, ClassLoader>();

	/**
	 * Get buffered or generate the Bundled java EE ClassLoader for the given
	 * bundle.
	 * 
	 * @param bundle
	 * @return
	 */
	public static ClassLoader getBundledJeeClassLoader(Bundle bundle) {
		ClassLoader resLoader = wbCtxClassLoaders.get(bundle);
		if (resLoader == null) {
			resLoader = new BundledJeeContextClassLoader(bundle);
			wbCtxClassLoaders.put(bundle, resLoader);
		}
		return resLoader;
	}

	/**
	 * Get buffered or generate the Jsp URLClassLoader for the given bundle.
	 * 
	 * @param bundle
	 * @return
	 */
	public static URLClassLoader getBundledJspUrlClassLoader(
			IWABServletContext wabCtx) {
		Bundle bundle = wabCtx.getBundle();
		URLClassLoader resLoader = wbJspUrlClassLoaders.get(bundle);
		if (resLoader == null) {
			resLoader = new BundleJspClassLoader(bundle, wabCtx
					.getWabClassLoader());
			wbJspUrlClassLoaders.put(bundle, resLoader);
		}
		return resLoader;
	}

	/**
	 * Clear buffered Data
	 */
	public static void clean() {
		wbCtxClassLoaders.clear();
		wbCtxClassLoaders = null;
		wbJspUrlClassLoaders.clear();
		wbJspUrlClassLoaders = null;
	}
}
