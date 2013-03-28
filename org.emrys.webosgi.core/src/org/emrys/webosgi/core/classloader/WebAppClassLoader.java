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
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.FileLocator;
import org.emrys.webosgi.common.util.FileUtil;
import org.emrys.webosgi.core.FwkActivator;
import org.emrys.webosgi.core.internal.FwkRuntime;
import org.emrys.webosgi.core.service.IWABServletContext;
import org.ops4j.pax.url.war.ServiceConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

import aQute.lib.osgi.Jar;

/**
 * The WAB Context Class loader use URLClassLoader to load WEB-INF/lib/*.jar at
 * first. The parent class loader is the current WAB's proxy bundle.
 * 
 * @author Leo Chang
 * @version 2011-1-13
 */
public class WebAppClassLoader extends URLClassLoader {
	// If we bundled Hibernate2.5, if let system loader to load
	// javax.validation.Validation, a class define not found exception threw.
	private static final String[] packageNotDelegated = { "javax.validation." };

	private static final String[] packageDelegated = { "java.", "javax.",// Java
			// extensions
			"org.xml.sax.", // SAX 1 & 2
			"org.w3c.dom.", // DOM 1 & 2
			"org.apache.xerces.", // Xerces 1 & 2, not in JDK6, IBM JVM offers.
			"org.apache.xalan." // Xalan, not exists in JDK6, IBM JVM offers.
	};
	static final String WAB_CLASSPATH_ROOT = "WEB-INF/classes/";
	private final IWABServletContext wabServletCtx;
	private boolean isEnclosedWarBundle;

	public WebAppClassLoader(IWABServletContext wabServletCtx) {
		super(new URL[0], new WabBundleClassLoader(wabServletCtx));
		this.wabServletCtx = wabServletCtx;

		// If the WAB WebContent relative path is just the WAB bundle root, this
		// WAB is just a ordinary war. And if this war has a plenty of jars, the
		// OSGi bundle loader is really slow. We use a war lib eclosed loader in
		// this case to optimize performance. However, there is a defect that
		// the bundle loaded class can't find the war enlosed resource in this
		// case. This defect can be solved by not using war enclosed load.
		if (isWarEnclosed()) {
			isEnclosedWarBundle = true;
			initWarEnclosedClassPath();
		}

		// Concentrated all concentrated tld files as a temporarily jar file
		// to optimize performance.
		File concentratedTldJar = concentrateTld();
		if (concentratedTldJar != null) {
			try {
				this.addURL(concentratedTldJar.toURI().toURL());
			} catch (MalformedURLException e) {
			}
		}
	}

	private boolean isWarEnclosed() {
		// A WAB bundle without any original Bundle hander in its MANIFEST.MF
		// file is war enclosed loaded WAB.
		Bundle wab = wabServletCtx.getBundle();
		File webContentRoot = wabServletCtx.getWebContentRoot();
		if (webContentRoot != null) {
			// Check "WAR-URL" from Wab bundle heander, our WabBundle url
			// heandler added this mark.
			String warUrl = (String) wab.getHeaders().get(
					ServiceConstants.INSTR_WAR_URL);
			if (StringUtils.isNotEmpty(warUrl)) {
				try {
					File warFile = new File(new URL(warUrl).getPath());
					if (warFile.isFile() && warUrl.endsWith(".war"))
						return true;
					// If the war file is a directory, check if a bundle with
					// symbolic name.
					Jar jar = new Jar(warFile.getAbsolutePath(), warFile);
					Manifest manifest = jar.getManifest();
					String symbolicName = manifest.getMainAttributes()
							.getValue(Constants.BUNDLE_SYMBOLICNAME);
					if (StringUtils.isEmpty(symbolicName))
						return true;
				} catch (Exception e) {
				}
			}

			// If not any .class or jars not under WEB-INF/classes or WEB/lib
			// dir, return true.
			boolean result = true;
			Enumeration<URL> entries = wab.findEntries("/", "*.class", true);
			while (entries != null && entries.hasMoreElements()) {
				String path = entries.nextElement().getPath();
				if (!path.contains("WEB-INF/classes")) {
					result = false;
					break;
				}
			}

			if (result) {
				entries = wab.findEntries("/", "*.jar", true);
				while (entries != null && entries.hasMoreElements()) {
					String path = entries.nextElement().getPath();
					if (!path.contains("WEB-INF/lib")) {
						// TODO: Check if this jar is in WAB's Bundle-ClassPath
						// header.
						result = false;
						break;
					}
				}
			}

			return result;

			// If the WAB's WebContent dir is just the root of WAB bundle, and
			// this WAB has WebContent with /WEB-INF/web.xml ./classes ./lib.
			// Especially, to reduce the risk of the defect that bundle loaded
			// class cann't find the resource of this loader, we check if the
			// count of jars over some limit.
			/*IPath webContentRelPath = WebBundleUtil
					.findWebContentPath(wabServletCtx.getBundle());
			if (webContentRelPath != null
					&& webContentRelPath.segmentCount() == 0) {
				File webLibDir = new File(webContentRoot, "WEB-INF/lib");
				if (!wabServletCtx.isHostBundle() && webLibDir.exists()
						&& webLibDir.list().length > 50)
					return true;
			}*/
		}
		return false;
	}

	private void initWarEnclosedClassPath() {
		// Add WEB-INF/classes to class-paths.
		try {
			URL classesRoot = wabServletCtx.getResource("/WEB-INF/classes");
			if (classesRoot != null)
				this.addURL(classesRoot);

			// Add all dependencies WEB-INF/lib/*.jars' URL to super
			// URLClassLoader.
			addBundleClassPathJars(wabServletCtx.getBundle());
			Bundle[] fragments = FwkActivator.getFragments(wabServletCtx
					.getBundle());
			if (fragments != null) {
				for (int i = 0; i < fragments.length; i++) {
					addBundleClassPathJars(fragments[i]);
				}
			}
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException();
		}
	}

	private File concentrateTld() {
		URL[] jarUrls = super.getURLs();
		File workTmpDir = (File) FwkRuntime.getInstance()
				.getFrameworkAttribute(FwkRuntime.ATTR_JEE_WORK_DIR);
		File tldJarTmpRoot = new File(workTmpDir, wabServletCtx.getBundle()
				.getSymbolicName()
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
			} catch (IOException e) {
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

	public ClassLoader getParentClassLoader() {
		// If it is war enclosed class load, use this Framework's OSGi bundle
		// loader as its parent CL.
		if (isEnclosedWarBundle)
			return WebAppClassLoader.class.getClassLoader();
		return this.getParent();
	}

	protected ClassLoader getParentResLoader() {
		if (isEnclosedWarBundle) {
			// Use this Framework's OSGi bundle loader as its parent CL.
			ClassLoader fwkParentCL = WebAppClassLoader.class.getClassLoader();
			if (fwkParentCL != null)
				return fwkParentCL;
			return getSystemClassLoader();
		}
		return this.getParent();
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
		// 1. Check if the class name is any excepted from this class loader,
		// let the parent class loader to load them. For example, J2SE
		// classes start with "java.", "javax.", etc.
		boolean delegated = checkIfDelegatedClass(className);
		if (delegated) {
			try {
				loadedClazz = getParentClassLoader().loadClass(className);
			} catch (ClassNotFoundException e) {
			}
		}

		// 2. Try to load from local class paths.
		if (loadedClazz == null) {
			loadedClazz = findLoadedClass(className);
			if (loadedClazz == null) {
				try {
					loadedClazz = findClass(className);
				} catch (ClassNotFoundException e) {
					// 3. If local not found, try to load from parent CL if not
					// exluded from this loader.
					if (!delegated) {
						loadedClazz = getParentClassLoader().loadClass(
								className);
					}
				}
			}
		}

		if (resolveClass)
			resolveClass(loadedClazz);

		return loadedClazz;
	}

	protected boolean checkIfDelegatedClass(String className) {
		for (String pkNoDeleagated : packageNotDelegated) {
			if (className.startsWith(pkNoDeleagated))
				return false;
		}
		for (String pkDelegated : packageDelegated) {
			if (className.startsWith(pkDelegated))
				return true;
		}
		return false;
	}

	@Override
	public Enumeration<URL> findResources(String path) throws IOException {
		return super.findResources(path);
	}
}