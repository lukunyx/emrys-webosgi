package org.emrys.webosgi.core.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.emrys.webosgi.common.util.BundleServiceUtil;
import org.emrys.webosgi.common.util.FileUtil;
import org.emrys.webosgi.core.FwkActivator;
import org.emrys.webosgi.core.IFwkConstants;
import org.emrys.webosgi.core.internal.FwkRuntime;
import org.emrys.webosgi.core.service.IWABServletContext;
import org.osgi.framework.Bundle;

/**
 * Some convenient method for Web Application Bundle.
 * 
 * @author Leo Chang
 * @since 2010-10-13
 */
public class WebBundleUtil extends BundleServiceUtil implements IFwkConstants {

	private static Bundle webHostBundle;

	/**
	 * Check if the given bundle is host web bundle in the case this framework
	 * is bridged in a JavaEE server.
	 * 
	 * @param bundle
	 * @return
	 */
	public static boolean isHostWebBundle(Bundle bundle) {
		// If framework is OSGi embbedde, and not bridged in JavaEE server. No
		// host web exists.
		if (FwkRuntime.getInstance().isOSGiEmbedded())
			return false;

		if (webHostBundle != null)
			return webHostBundle == bundle;

		// The host web bundle's location equals with the top servlet's real
		// context path.
		String fwkServletCtxRoot = FwkActivator.getInstance().getJeeContainer()
				.getServletContext().getRealPath("/");
		// If OSGi HttpService Embeded, this root path maybe null.
		if (fwkServletCtxRoot == null)
			return false;
		IPath fwkServletCtxRootPath = new Path(fwkServletCtxRoot);

		try {
			IPath thisBundleLocPath = new Path(FileLocator
					.getBundleFile(bundle).getAbsolutePath());
			boolean b = fwkServletCtxRootPath.equals(thisBundleLocPath);
			if (b)
				webHostBundle = bundle;
			return b;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Find Host web bundle if the framework is bridged in a JavaEE server. In
	 * this case, the host web bundle exists. Otherwise, if the framework
	 * embedded in a OSGi runtime adapting to HttpSerivce, there is no host web
	 * bundle.
	 * 
	 * @return
	 */
	public static Bundle findHostWebBundle() {
		// If framework is OSGi embbedde, and not bridged in JavaEE server. No
		// host web exists.
		if (FwkRuntime.getInstance().isOSGiEmbedded())
			return null;

		if (webHostBundle != null) {
			return webHostBundle;
		}

		Bundle[] bundles = FwkActivator.getInstance().getBundle()
				.getBundleContext().getBundles();
		for (Bundle b : bundles) {
			if (isHostWebBundle(b)) {
				webHostBundle = b;
				break;
			}
		}

		return null;
	}

	/**
	 * Try to extract the WebContent directory from WAB bundle if the bundle is
	 * a zipped file. If extracted resource not exists, the WebContent will be
	 * extracted to local directory. If the force-update argment true and the
	 * bundle file last-modify timestamp is newer than the mark in extracted
	 * resource.
	 * 
	 * @param wabundle
	 * @param forceUpdate
	 * @return
	 * @throws IOException
	 */
	public static IPath getExtractedWebContentRoot(Bundle wabundle,
			boolean forceUpdate) throws IOException {

		IPath webContentPath = WebBundleUtil.findWebContentPath(wabundle);
		if (webContentPath == null)
			return null;

		File webContentRoot = null;
		// If the webbundle url like webbundle:// , FileLocator cann't find the
		// real bundle file. We use Bundle.getLocation() at first.
		File bundleFile = null;
		URL loc = new URL(wabundle.getLocation());
		if (loc.getProtocol().equals("file"))
			bundleFile = new File(loc.getPath());
		else if (loc.getProtocol().equals("webbundle")) {
			// See OSGi Enterprise speci4.2 128.4.1
			String p = loc.getPath();
			try {
				URL furl = new URL(p);
				if (furl.getProtocol().equals("file"))
					bundleFile = new File(furl.getPath());
			} catch (Exception e) {
			}

		}

		if (bundleFile == null) {
			// Else, the bundle file may be generated in the OSGi .configure
			// directory.
			bundleFile = FileLocator.getBundleFile(wabundle);
		}
		if (bundleFile.isDirectory()) {
			webContentRoot = new Path(bundleFile.getAbsolutePath()).append(
					webContentPath).toFile();
		} else if (bundleFile.isFile()) {
			if (webContentPath != null) {
				webContentRoot = BundleServiceUtil.getBundleWorkspaceRoot(
						wabundle).append("WebContent").toFile();
				// Synchronized the web content prepare to a webundle.
				synchronized (wabundle) {
					File metadataFile = new File(webContentRoot, ".metadata");
					if (forceUpdate && webContentRoot.exists()) {
						// Check the .metadata file in existant WebContent to
						// compare the last-modified timestamp. If last-modified
						// not exists or not equals with the bundle file's, do
						// delete all existant WebContent resource.
						boolean toDelete = true;
						if (metadataFile.exists()) {
							try {
								Properties prop = new Properties();
								prop.load(new FileInputStream(metadataFile));
								String lastModified = prop
										.getProperty("last-modified");
								if (Long.toString(bundleFile.lastModified())
										.equals(lastModified)) {
									toDelete = false;
								}
							} catch (Exception e) {
								// e.printStackTrace();
							}
						}
						if (toDelete)
							FileUtil.deleteAllFile(webContentRoot, null);
					}

					if (!webContentRoot.exists()) {
						webContentRoot.mkdirs();
						FileUtil.unZipFile(bundleFile, webContentPath
								.toPortableString(), webContentRoot);
						// Record the last-modified timestamp in .metadata file.
						try {
							Properties prop = new Properties();
							prop.setProperty("last-modified", Long
									.toString(bundleFile.lastModified()));
							prop.store(new FileOutputStream(metadataFile),
									"UTF-8");
						} catch (Exception e) {
							// e.printStackTrace();
						}
					}
				}
			}
		}

		if (webContentRoot != null)
			return new Path(webContentRoot.getAbsolutePath());
		return null;
	}

	/**
	 * Get the file URL of the Web Content root of a bundle. Optionally, if this
	 * bundle is a jar file, the web content will be unzipped to a temporary
	 * folder and return this temporary folder's file url. If you want to get
	 * real web content folder in the local workspace, use
	 * getResolvedBundleWebContent() for instead.
	 * 
	 * @param bundle
	 * @param ensureLocal
	 *            if ture, the zipped WebCotent entry will be extracted to
	 *            temporarily folder in local file system.
	 * @return
	 * @throws IOException
	 */
	public static URL findWebContentURL(Bundle webundle, boolean ensureLocal)
			throws IOException {
		IPath webContentPath = findWebContentPath(webundle);
		if (webContentPath != null) {
			if (!ensureLocal)
				return webundle.getEntry(webContentPath.toPortableString());

			URL[] es = FileLocator.findEntries(webundle, webContentPath);
			if (es.length > 0) {
				// Extract the zipped entry to local file system.
				return FileLocator.toFileURL(es[0]);
			}
		}

		return null;
	}

	/**
	 * Find the web content path relative to the bundle root which contains
	 * WEB-INF/web.xml file. We prefer to "WebContent/WEB-INF/web.xml" path. If
	 * not, find ./WEB-INF/web.xml , return one with the shortest path.
	 * 
	 * @param bundle
	 * @return
	 */
	public static IPath findWebContentPath(Bundle bundle) {
		IPath prefPath = new Path("WebContent/WEB-INF/web.xml");
		IPath shortestOne = null;
		Enumeration<URL> entries = bundle.findEntries("/", "web.xml", true);
		// We prefer to "WebContent/WEB-INF/web.xml" path. If not, find
		// ./WEB-INF/web.xml , return one with the shortest
		// path.
		while (entries != null && entries.hasMoreElements()) {
			IPath webxmlPath = new Path(entries.nextElement().getPath());
			IPath bundleFilePath = new Path(bundle.getEntry("/").getPath());

			// OSGi Euqinox r4.2 not has IPath.makeRelativeTo();
			// IPath relPath = webxmlPath.makeRelativeTo(bundleFilePath);
			int commonLength = webxmlPath.matchingFirstSegments(bundleFilePath);
			IPath relPath = webxmlPath.removeFirstSegments(commonLength);

			// Validate the relative path of web.xml.
			if (relPath.segmentCount() >= 2
					&& relPath.segment(relPath.segmentCount() - 2).equals(
							"WEB-INF")) {
				IPath p = relPath.removeLastSegments(2);
				if (prefPath.equals(relPath))
					return p;
				if (shortestOne == null
						|| p.segmentCount() < shortestOne.segmentCount())
					shortestOne = p;
			}
		}
		return shortestOne;
	}

	public static String getWabContextPathHeader(Bundle wabundle) {
		// Find the Web-ContextPath from MANIFEST.MF file.
		String wabCtxPath = (String) wabundle.getHeaders().get(
				"Web-ContextPath");
		return wabCtxPath;
	}

	public static boolean isWebAppBundle(Bundle bundle) {
		String ctxPath = getWabContextPathHeader(bundle);
		return StringUtils.isNotEmpty(ctxPath);
	}

	/**
	 * Find the wab bundle by its servlet context path.
	 * 
	 * @param wabCtxPath
	 * @return
	 */
	public static Bundle findWabByContextPath(String wabCtxPath) {
		IWABServletContext ctx = FwkActivator.getInstance().getJeeContainer()
				.getWABServletContext(wabCtxPath);
		if (ctx != null)
			return ctx.getBundle();
		return null;
	}
}
