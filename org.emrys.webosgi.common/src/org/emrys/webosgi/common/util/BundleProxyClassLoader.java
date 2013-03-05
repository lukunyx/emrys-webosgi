package org.emrys.webosgi.common.util;

import java.io.IOException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;

import org.eclipse.core.runtime.FileLocator;
import org.osgi.framework.Bundle;

/**
 * ClassLoader backed by an OSGi bundle. Provides the ability to use a separate
 * class loader as fall back.
 * 
 * Contains facilities for tracing class loading behaviour so that issues can be
 * easily resolved.
 * 
 * For debugging please see {@link DebugUtils}.
 * 
 * @author Adrian Colyer
 * @author Andy Piper
 * @author Costin Leau
 * @author Leo Chang
 */
public class BundleProxyClassLoader extends ClassLoader {
	private final ClassLoader bridge;
	private final Bundle backingBundle;

	/**
	 * Factory method for creating a class loader over the given bundle.
	 * 
	 * @param aBundle
	 *            bundle to use for class loading and resource acquisition
	 * @return class loader adapter over the given bundle
	 */
	public static BundleProxyClassLoader createBundleClassLoaderFor(
			Bundle aBundle) {
		return createBundleClassLoaderFor(aBundle, null);
	}

	/**
	 * Factory method for creating a class loader over the given bundle and with
	 * a given class loader as fall-back. In case the bundle cannot find a class
	 * or locate a resource, the given class loader will be used as fall back.
	 * 
	 * @param bundle
	 *            bundle used for class loading and resource acquisition
	 * @param bridge
	 *            class loader used as fall back in case the bundle cannot load
	 *            a class or find a resource. Can be <code>null</code>
	 * @return class loader adapter over the given bundle and class loader
	 */
	public static BundleProxyClassLoader createBundleClassLoaderFor(
			final Bundle bundle, final ClassLoader bridge) {
		return (BundleProxyClassLoader) AccessController
				.doPrivileged(new PrivilegedAction() {
					public Object run() {
						return new BundleProxyClassLoader(bundle, bridge);
					}
				});
	}

	/**
	 * Private constructor.
	 * 
	 * Constructs a new <code>BundleProxyClassLoader</code> instance.
	 * 
	 * @param bundle
	 * @param bridgeLoader
	 */
	public BundleProxyClassLoader(Bundle bundle, ClassLoader bridgeLoader) {
		super(null);
		this.backingBundle = bundle;
		this.bridge = bridgeLoader;
	}

	/**
	 * @param bundle
	 * @param object
	 * @param webContainerClassLoader
	 */
	public BundleProxyClassLoader(Bundle bundle, ClassLoader bridgeLoader,
			ClassLoader parent) {
		super(parent);
		this.backingBundle = bundle;
		this.bridge = bridgeLoader;
	}

	@Override
	protected Class findClass(String name) throws ClassNotFoundException {
		try {
			return this.backingBundle.loadClass(name);
		} catch (ClassNotFoundException cnfe) {
			throw new ClassNotFoundException(name + " not found from bundle ["
					+ backingBundle.getSymbolicName() + "]", cnfe);
		} catch (NoClassDefFoundError ncdfe) {
			// This is almost always an error
			// This is caused by a dependent class failure,
			// so make sure we search for the right one.
			String cname = ncdfe.getMessage().replace('/', '.');
			NoClassDefFoundError e = new NoClassDefFoundError(cname
					+ " not found from bundle by BundleProxyClassLoader");
			e.initCause(ncdfe);
			throw e;
		}
	}

	@Override
	protected URL findResource(String name) {
		URL url = this.backingBundle.getResource(name);
		// For JavaEE middle wares like spring, openjpa, a osgi bundleres
		// protocol may cann't be process normally. Here resolve the url of this
		// kind to ordinary format.
		return resolveResource(url);
	}

	@Override
	protected Enumeration findResources(String name) throws IOException {
		final Enumeration enm = this.backingBundle.getResources(name);
		if (enm == null)
			return null;
		return new Enumeration<URL>() {
			public boolean hasMoreElements() {
				return enm.hasMoreElements();
			}

			public URL nextElement() {
				URL url = (URL) enm.nextElement();
				return resolveResource(url);
			}
		};
		// return enm;
	}

	/**
	 * For JavaEE middle wares like spring, openjpa, a osgi bundleres protocol
	 * may cann't be process normally. Here resolve the url of this kind to
	 * ordinary format. Special for a bundle wrapped WebContent folder inside,
	 * to all resource under this folder, unzipped them from jar if need.
	 * 
	 * @param url
	 * @return
	 */
	private URL resolveResource(URL originalUrl) {
		if (originalUrl == null)
			return null;
		try {
			URL url = FileLocator.resolve(originalUrl);
			if (url.getProtocol().equals("jar")
					&& url.toExternalForm().contains("!/WebContent/"))
				return FileLocator.toFileURL(originalUrl);
			else
				return url;
		} catch (IOException e) {
			// e.printStackTrace();
		}
		return originalUrl;
	}

	@Override
	public URL getResource(String name) {
		URL resource = findResource(name);
		if (bridge != null && resource == null) {
			resource = bridge.getResource(name);
		}
		if (resource != null)
			return resource;

		if (getParent() != null)
			return super.getResource(name);

		return null;

	}

	@Override
	protected Class loadClass(String name, boolean resolve)
			throws ClassNotFoundException {
		if (name == null || name.length() == 0)
			throw new ClassNotFoundException(
					"The class with empty name cann't be found.");
		Class clazz = null;
		try {
			clazz = findClass(name);
		} catch (ClassNotFoundException cnfe) {
			if (bridge != null)
				clazz = bridge.loadClass(name);
		}
		// Try to load class from parent ClassLoader.
		if (clazz == null && getParent() != null)
			clazz = super.loadClass(name, resolve);

		if (resolve)
			resolveClass(clazz);

		return clazz;
	}

	/**
	 * Returns the bundle to which this class loader delegates calls to.
	 * 
	 * @return the backing bundle
	 */
	public Bundle getBundle() {
		return backingBundle;
	}
}
