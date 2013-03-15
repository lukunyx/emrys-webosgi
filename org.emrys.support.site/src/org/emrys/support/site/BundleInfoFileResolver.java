package org.emrys.support.site;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.emrys.webosgi.common.util.FileUtil;
import org.emrys.webosgi.core.resource.extension.IPublishedFileResolver;

/**
 * 
 * @author Leo Chang
 * @version 2010-10-25
 */
public class BundleInfoFileResolver implements IPublishedFileResolver {
	public static final String BUNDLE_INFO_REQ_PARAM_SITE_VERSION = "site-versions";
	public static final String BUNDLE_INFO_REQ_PARAM_BUILDER_URL = "builder-url";

	// http://192.168.11.49:8080/com.hirisun.osgi.launcher/fs/bundleinfo.xml?builder-url=file:/D:/autobuild/autobuilder1.0/&site-versions=com.hirisun.components.updatesite
	// com.hirisun.components.updatesite_1.0.0 or
	// com.hirisun.components.updatesite =
	// current. Multiple site split with "[,; ]{+}" or blank char.
	public File resolve(HttpServletRequest req, String path, String alias,
			String quickID) {
		try {
			File tf = File.createTempFile("tmp_bundleinfo", ".xml");
			tf.deleteOnExit();
			FileOutputStream fo = new FileOutputStream(tf);

			// Try to add builder's all bundles.
			String builderUrlStr = req
					.getParameter(BUNDLE_INFO_REQ_PARAM_BUILDER_URL);
			if (builderUrlStr != null) {
				IPath builderPluginsPath = null;
				URL builderUrl = null;
				try {
					builderUrl = new URL(builderUrlStr);
					builderPluginsPath = new Path(builderUrl.getPath());
				} catch (Exception e1) {
					// e1.printStackTrace();
				}

				if (builderPluginsPath == null)
					builderPluginsPath = new Path(builderUrlStr);

				File originalBundleInfoFile = builderPluginsPath
						.append(
								"configuration/org.eclipse.equinox.simpleconfigurator/bundles.info")
						.toFile();
				if (originalBundleInfoFile.exists()) {
					StringBuffer content = FileUtil.getContent(
							originalBundleInfoFile, "UTF-8");
					ByteArrayInputStream bin = new ByteArrayInputStream(content
							.toString().getBytes("UTF-8"));
					while (bin.available() > 0) {
						fo.write(bin.read());
					}
					bin.close();
					bin = null;
				}
			}

			String sitesVersions = req
					.getParameter(BUNDLE_INFO_REQ_PARAM_SITE_VERSION);
			Map<String, String> siteVersionMap = new HashMap<String, String>();
			if (sitesVersions != null && sitesVersions.length() > 0) {
				String[] items = sitesVersions.split("[,; ]+");
				for (String item : items) {
					int i = item.indexOf('_');
					String siteId = null;
					String version = "current";
					if (i == -1) {
						siteId = item;
					} else {
						siteId = item.substring(0, i);
						version = item.substring(i + 1);
					}
					siteVersionMap.put(siteId, version);
				}
			}

			File featuresRoot = Activator.getInstance().getFeaUpdateSitesRoot()
					.toFile();

			File[] fs = featuresRoot.listFiles();
			if (fs.length > 0) {
				for (File f : fs) {
					// if siteVersionMap not empty, then only resovle these
					// update site.
					visiteUpdateSite(fo, f, siteVersionMap);
				}
			} else {
				// if has no feature site yet, write a null char.
				fo.write(0);
			}

			fo.flush();
			fo.close();
			return tf;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * @param fo
	 * @param siteFile
	 * @param siteVersionMap
	 */
	private void visiteUpdateSite(FileOutputStream fo, File siteFile,
			Map<String, String> siteVersionMap) {
		File[] versionFiles = siteFile.listFiles();
		String version = null;
		if (siteVersionMap.containsKey(siteFile.getName()))
			version = siteVersionMap.get(siteFile.getName());
		else if (siteVersionMap.size() > 0)
			// If siteVersionMap not empty and not contains the current visited
			// update
			// site, ignore it.
			return;
		else
			version = "current";

		for (File versionFile : versionFiles) {
			if (version.equalsIgnoreCase(versionFile.getName())) {
				try {
					visitFeatureForBundles(fo, versionFile);
					break;
				} catch (Exception e) {
					// e.printStackTrace();
				}
			}
		}
	}

	private static void visitFeatureForBundles(OutputStream os, File pluginsRoot) {
		File plugins = new Path(pluginsRoot.getAbsolutePath())
				.append("plugins").toFile();
		if (plugins.exists() && plugins.isDirectory()) {
			File[] fileList = plugins.listFiles();
			PrintWriter writer = new PrintWriter(new BufferedWriter(
					new OutputStreamWriter(os)));

			for (int i = 0; i < fileList.length; i++) {
				File bundleFile = fileList[i];
				// Note: take care folder of bundle.
				String prefix = bundleFile.getName();
				if (bundleFile.isFile()) {
					prefix = bundleFile.getName().replaceAll(".jar", "");
				} else {
					File manifestFile = new Path(bundleFile.getAbsolutePath())
							.append("META-INF/MANIFEST.MF").toFile();
					if (manifestFile == null || !manifestFile.exists())
						continue;
				}

				int index = prefix.indexOf('_');
				String bundleName = null;
				String version = "1.0.0"; // Default version

				if (index == -1)
					bundleName = prefix;
				else {
					bundleName = prefix.substring(0, index);
					version = prefix.substring(index + 1);
				}

				// Ignore all none hirisun components jars.
				/*
				 * if (!bundleName.startsWith("com.hirisun.")) continue;
				 */
				try {
					writer.println(bundleName + "," + version + ","
							+ bundleFile.toURI().toURL() + ",4," + "false");
					writer.flush();
				} catch (MalformedURLException e) {
					// e.printStackTrace();
				}
			}
		}
	}
}
