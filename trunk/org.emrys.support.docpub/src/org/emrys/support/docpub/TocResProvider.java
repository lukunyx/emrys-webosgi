/**
 * 
 */
package org.emrys.support.docpub;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.emrys.webosgi.help.ITocResProvider;


/**
 * @author LeoChang
 * 
 */
public class TocResProvider implements ITocResProvider {

	public URL getResource(String path) {
		if (path != null
				&& path.contains(Activator.getInstance().getBundleSymbleName())) {
			IPath reqPath = new Path(path);
			String fullPluginId = reqPath.segment(2);
			fullPluginId = fullPluginId.replace(
					AutoFindTocProvider.FEATURE_SITE_TOC_CON_PREFIX, "");
			String[] segs = fullPluginId.split("\\$");
			if (segs.length == 3) {
				String siteName = segs[0];
				String versionName = segs[1];
				String pluginName = segs[2];

				Iterator<IPath> it = AutoFindTocProvider.docJarPaths.keySet()
						.iterator();
				while (it.hasNext()) {
					IPath bufferedPath = it.next();
					if (bufferedPath.toPortableString().contains(
							new Path(siteName + "/" + versionName).append(
									"plugins").append(pluginName)
									.toPortableString())) {
						File unzippedDocRoot = AutoFindTocProvider.docJarPaths
								.get(bufferedPath);
						String resRelPath = reqPath.removeFirstSegments(3)
								.toPortableString();
						File resFile = new File(unzippedDocRoot, resRelPath);
						try {
							if (resFile.exists())
								return resFile.toURI().toURL();
						} catch (MalformedURLException e) {
							// e.printStackTrace();
						}
					}
				}
			}
		}
		return null;
	}
}
