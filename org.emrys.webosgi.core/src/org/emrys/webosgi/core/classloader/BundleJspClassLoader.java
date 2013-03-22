package org.emrys.webosgi.core.classloader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.emrys.webosgi.common.util.BundleProxyClassLoader;
import org.emrys.webosgi.common.util.FileUtil;
import org.emrys.webosgi.core.FwkActivator;
import org.emrys.webosgi.core.internal.FwkRuntime;
import org.emrys.webosgi.core.service.IWABServletContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * simulate the class org.eclipse.equinox.internal.jsp.jasper.JspClassLoader to
 * implements the web bundle class loader.
 * 
 * @author Leo Chang
 * @version 2011-1-13
 */
public class BundleJspClassLoader extends URLClassLoader {
	/**
	 * Apache Jasper's Bundle
	 */
	private static final Bundle JASPERBUNDLE = FwkActivator.getJasperBundle();
	/**
	 * the parent class loader of this class
	 */
	private static final ClassLoader PARENT = BundleJspClassLoader.class
			.getClassLoader().getParent();
	/**
	 * Java package prefix.
	 */
	private static final String JAVA_PACKAGE = "java."; //$NON-NLS-1$
	/**
	 * Define a empty class loader.
	 */
	private static final ClassLoader EMPTY_CLASSLOADER = new ClassLoader() {
		@Override
		public URL getResource(String name) {
			return null;
		}

		@Override
		public Enumeration findResources(String name) throws IOException {
			return new Enumeration() {
				public boolean hasMoreElements() {
					return false;
				}

				public Object nextElement() {
					return null;
				}
			};
		}

		@Override
		public Class loadClass(String name) throws ClassNotFoundException {
			throw new ClassNotFoundException(name);
		}
	};
	private final File generatedTldJar;
	private final Bundle bundle;

	public BundleJspClassLoader(IWABServletContext wabSerlvetCtx,
			ClassLoader jspersParentClassloader) {
		super(new URL[0], new BundledJeeContextClassLoader(wabSerlvetCtx,
				new BundleProxyClassLoader(JASPERBUNDLE, null, null
				/*
				 * new JSPContextFinder (jspersParentClassloader == null ?
				 * EMPTY_CLASSLOADER : jspersParentClassloader)
				 */)));

		this.bundle = wabSerlvetCtx.getBundle();
		// Add all dependencies jars' url to super URLClassLoader, for jasper
		// jsp compiler will search them for tld files. We concentrate tld files
		// in a temporarily jar to optimize performace.
		addBundleClassPathJars(bundle);
		Bundle[] fragments = FwkActivator.getFragments(bundle);
		if (fragments != null) {
			for (int i = 0; i < fragments.length; i++) {
				addBundleClassPathJars(fragments[i]);
			}
		}

		// Concentrated all concentrated tld files as a temporarily jar file to
		// optimize performance.
		generatedTldJar = concentrateTld();
	}

	private File concentrateTld() {
		URL[] jarUrls = super.getURLs();
		File workTmpDir = (File) FwkRuntime.getInstance()
				.getFrameworkAttribute(FwkRuntime.ATTR_JEE_WORK_DIR);
		File tldJarTmpRoot = new File(workTmpDir, bundle.getSymbolicName()
				+ "_tld");
		FileUtil.deleteAllFile(tldJarTmpRoot, null);
		for (URL url : jarUrls) {
			try {
				URLConnection conn = url.openConnection();
				if (conn instanceof JarURLConnection) {
					scanJar((JarURLConnection) conn, tldJarTmpRoot);
				} else {
					String urlStr = url.toString();
					if (urlStr.startsWith("file:") && urlStr.endsWith(".jar")) {
						URL jarURL = new URL("jar:" + urlStr + "!/");
						scanJar((JarURLConnection) jarURL.openConnection(),
								tldJarTmpRoot);
					}
				}
			} catch (MalformedURLException e) {
				// e.printStackTrace();
			} catch (IOException e) {
				// e.printStackTrace();
			}
		}

		// Not found any internal tld files, return null.
		String[] tldFiles = tldJarTmpRoot.list();
		if (tldFiles == null || tldFiles.length == 0)
			return null;

		try {
			FileUtil.zipFolder(tldJarTmpRoot.getAbsolutePath(), tldJarTmpRoot
					.getAbsolutePath()
					+ ".jar");
			return new File(tldJarTmpRoot.getAbsolutePath() + ".jar");
		} catch (Exception e) {
			// e.printStackTrace();
		} finally {
			FileUtil.deleteAllFile(tldJarTmpRoot, null);
		}
		return null;
	}

	private void scanJar(JarURLConnection conn, File tldJarFile) {
		JarFile jarFile = null;
		try {
			jarFile = conn.getJarFile();
			Enumeration entries = jarFile.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = (JarEntry) entries.nextElement();
				String name = entry.getName();
				if (!name.startsWith("META-INF/"))
					continue;
				if (!name.endsWith(".tld"))
					continue;
				InputStream stream = jarFile.getInputStream(entry);
				File tldFile = new File(tldJarFile, entry.getName());
				FileUtil.writeToFile(stream, tldFile);
			}
		} catch (Exception e) {
			// e.printStackTrace();
		} finally {
			if (jarFile != null) {
				try {
					jarFile.close();
				} catch (Throwable t) {
					// ignore
				}
			}
		}
	}

	/**
	 * Add the given bundle's classpath to URLClassLoader's search paths.
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
				if (candidate.endsWith(".jar")) { //$NON-NLS-1$
					URL entry = bundle.getEntry(candidate);
					if (entry != null) {
						URL jarEntryURL;
						try {
							jarEntryURL = new URL(
									"jar:" + entry.toString() + "!/"); //$NON-NLS-1$ //$NON-NLS-2$
							super.addURL(jarEntryURL);
						} catch (MalformedURLException e) {
							// TODO should log this.
						}
					}
				}
			}
		}
	}

	@Override
	protected Class loadClass(String name, boolean resolve)
			throws ClassNotFoundException {
		if (PARENT != null && name.startsWith(JAVA_PACKAGE))
			return PARENT.loadClass(name);
		// return super.findClass(name);
		throw new ClassNotFoundException(name);
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		try {
			return super.loadClass(name);
		} catch (Exception e) {
			// e.printStackTrace();
		}

		return getParent().loadClass(name);
	}

	// Classes should "not" be loaded by this classloader from the URLs - it is
	// just used for TLD
	// resource discovery.
	@Override
	protected Class findClass(String name) throws ClassNotFoundException {
		return super.loadClass(name);
	}

	@Override
	public URL findResource(String name) {
		return super.findResource(name);
	}

	@Override
	public Enumeration<URL> findResources(String name) throws IOException {
		return super.findResources(name);
	}

	@Override
	public URL[] getURLs() {
		// Return all concentrated tld files as a temporarily jar file to
		// optimize performance.
		try {
			if (generatedTldJar != null)
				return new URL[] { generatedTldJar.toURI().toURL() };
		} catch (MalformedURLException e) {
			// e.printStackTrace();
		}
		return new URL[0];
	}
}