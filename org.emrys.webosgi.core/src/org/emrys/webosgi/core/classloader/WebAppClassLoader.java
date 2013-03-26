package org.emrys.webosgi.core.classloader;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.FileLocator;
import org.emrys.webosgi.core.FwkActivator;
import org.emrys.webosgi.core.service.IWABServletContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * The WAB Context Class loader use URLClassLoader to load WEB-INF/lib/*.jar at
 * first. The parent class loader is the current WAB's proxy bundle.
 * 
 * @author Leo Chang
 * @version 2011-1-13
 */
public class WebAppClassLoader extends URLClassLoader {
	private static final String[] packageExcepted = { "java.", "javax.", // Java
			// extensions
			"org.xml.sax.", // SAX 1 & 2
			"org.w3c.dom.", // DOM 1 & 2
			"org.apache.xerces.", // Xerces 1 & 2
			"org.apache.xalan." // Xalan
	};
	static final String WAB_CLASSPATH_ROOT = "WEB-INF/classes/";
	private final IWABServletContext wabServletCtx;

	public WebAppClassLoader(IWABServletContext wabServletCtx) {
		super(new URL[0], new WabBundleClassLoader(wabServletCtx));
		this.wabServletCtx = wabServletCtx;
		// Add WEB-INF/classes to class-paths.
		try {
			URL classesRoot = wabServletCtx.getResource("/WEB-INF/classes");
			if (classesRoot != null)
				this.addURL(classesRoot);
		} catch (MalformedURLException e) {
			// e.printStackTrace();
		}
		// Add all dependencies WEB-INF/lib/*.jars' URL to super URLClassLoader.
		addBundleClassPathJars(wabServletCtx.getBundle());
		Bundle[] fragments = FwkActivator.getFragments(wabServletCtx
				.getBundle());
		if (fragments != null) {
			for (int i = 0; i < fragments.length; i++) {
				addBundleClassPathJars(fragments[i]);
			}
		}
	}

	/**
	 * Add the given WAB WEB-INF/lib/*.jars' classpath to URLClassLoader's
	 * search paths.
	 * 
	 * @param bundle
	 */
	private void addBundleClassPathJars(Bundle bundle) {
		Dictionary headers = bundle.getHeaders();
		String classPath = (String) headers.get(Constants.BUNDLE_CLASSPATH);
		if (classPath != null) {
			StringTokenizer tokenizer = new StringTokenizer(classPath, ","); //$NON-NLS-1$
			while (tokenizer.hasMoreTokens()) {
				String candidate = tokenizer.nextToken().trim();
				if (candidate.contains("WEB-INF/lib/") && candidate.endsWith(".jar")) { //$NON-NLS-1$
					URL entry = bundle.getEntry(candidate);
					if (entry != null) {
						try {
							URL classPathURL = FileLocator.toFileURL(entry);
							this.addURL(classPathURL);
						} catch (Exception e) {
							// e.printStackTrace();
							FwkActivator.getInstance().log(e);
						}
					}
				}
			}
		}
	}

	@Override
	public URL getResource(String name) {
		URL url = findResource(name);
		if (url != null)
			return url;
		return getParentResLoader().getResource(name);
	}

	@Override
	public Enumeration<URL> getResources(String resName) throws IOException {
		final Enumeration<URL>[] resources = new Enumeration[2];
		resources[0] = findResources(resName);
		resources[1] = getParentResLoader().getResources(resName);
		return new Enumeration<URL>() {
			int index = 1;

			public boolean hasMoreElements() {
				while (this.index >= 0) {
					if (resources[index].hasMoreElements())
						return true;
					index -= 1;
				}
				return false;
			}

			public URL nextElement() {
				while (this.index >= 0) {
					Enumeration<URL> e = resources[index];
					if (e.hasMoreElements())
						return e.nextElement();
					index -= 1;
				}
				throw new NoSuchElementException();
			}
		};
	}

	protected ClassLoader getParentResLoader() {
		// Let the OSGi framework Context CL to be the parent resource CL.
		ClassLoader fwkParentCL = this.getClass().getClassLoader().getParent();
		if (fwkParentCL != null)
			return fwkParentCL;
		return getSystemClassLoader();
	}

	@Override
	public URL findResource(String name) {
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
		// If the class-path resource starts with "/" the URLClassLoader not
		// return null.
		if (name.startsWith("/"))
			name = name.substring(1);
		return super.findResource(name);
	}

	@Override
	protected synchronized Class<?> loadClass(String className,
			boolean resolveClass) throws ClassNotFoundException {
		Class loadedClazz = null;
		// Check if the class name is any excepted from this class loader, let
		// the parent class loader to load them.
		if (checkIfExceptedClass(className)) {
			loadedClazz = getParent().loadClass(className);
		} else {
			loadedClazz = findLoadedClass(className);
			if (loadedClazz == null) {
				try {
					loadedClazz = findClass(className);
				} catch (ClassNotFoundException e) {
				}
				if (loadedClazz == null) {
					loadedClazz = getParent().loadClass(className);
				}
			}
		}

		if (resolveClass)
			resolveClass(loadedClazz);

		return loadedClazz;
	}

	protected boolean checkIfExceptedClass(String className) {
		for (String pkPrefix : packageExcepted) {
			if (className.startsWith(pkPrefix))
				return true;
		}
		return false;
	}

	@Override
	public Enumeration<URL> findResources(String path) throws IOException {
		return super.findResources(path);
	}
}