package org.emrys.support.site;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.emrys.webosgi.common.util.FileUtil;
import org.emrys.webosgi.core.resource.IWebResConstants;
import org.emrys.webosgi.core.resource.extension.IUploadFileReciever;

/**
 * The update site Folder reciever.
 * 
 * @author Leo Chang
 * @version 2010-10-25
 */
public class FeatureSiteReciever implements IUploadFileReciever {
	/**
	 * The update site event listeners.
	 */
	public static Set<IUpdateSiteEventListener> listeners;
	/**
	 * The singleton instance.
	 */
	public static FeatureSiteReciever Instance = null;
	/**
	 * the upload type of Update Site
	 */
	public static final String UPLOAD_TYPE_FEATURE_SITE = "Bundle_Feature_Site";

	/**
	 * @return the singleton instance.
	 */
	public static FeatureSiteReciever getInstance() {
		return Instance;
	}

	/**
	 * Default constructor.
	 */
	public FeatureSiteReciever() {
		Instance = this;
	}

	public static void addUpdateSiteListener(IUpdateSiteEventListener listener) {
		if (listeners == null)
			listeners = new HashSet<IUpdateSiteEventListener>();
		listeners.add(listener);
	}

	public static void removeUpdateSiteListener(
			IUpdateSiteEventListener listener) {
		if (listeners == null)
			return;
		listeners.remove(listener);
	}

	public boolean process(HttpServletResponse response,
			Map<File, Map<String, String>> fileParams) {
		IPath updateSitesRoot = Activator.getInstance().getFeaUpdateSitesRoot();
		Iterator<Entry<File, Map<String, String>>> it = fileParams.entrySet()
				.iterator();

		while (it.hasNext()) {
			Entry entry = it.next();
			File file = (File) entry.getKey();
			Map<String, String> params = (Map<String, String>) entry.getValue();

			String uploadType = params.get(IWebResConstants.FILE_PARA_TEYP);
			if (uploadType != null
					&& uploadType.equals(UPLOAD_TYPE_FEATURE_SITE)) {
				try {
					if (file.isDirectory()) {
						String version = getSiteVersion(file);
						// Remove \r\n at the end of version string if exists.
						version = version.replaceAll("\\s+", "");
						IPath targetSitePath = updateSitesRoot.append(
								file.getName()).append(version);

						Activator.getInstance().log(
								"Recieved Update Site Files, saved into: "
										+ targetSitePath.toPortableString(), 0,
								true);

						// FIXME:the last version of the same feature site will
						// be deleted
						// temporarily.
						if (targetSitePath.toFile().exists()) {
							FileUtil.deleteAllFile(targetSitePath.toFile(),
									null);
						}

						FileUtil.copyDirectiory(file.getAbsolutePath(),
								targetSitePath.toOSString());

						triggerFeatureUpdateEvent(file.getName(), version);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				return true;
			}
		}

		return false;
	}

	/**
	 * Trigger the Feature Update Event.
	 * 
	 * @param name
	 * @param ver
	 */
	private void triggerFeatureUpdateEvent(String updateSiteName, String version) {
		if (listeners == null || listeners.size() == 0)
			return;
		for (IUpdateSiteEventListener l : listeners) {
			try {
				l.siteUpdated(updateSiteName, version);
			} catch (Throwable t) {
				// e.printStackTrace();
				Activator.getInstance().log(t);
			}
		}
	}

	/**
	 * @param file
	 * @return
	 */
	private String getSiteVersion(File file) {
		File versionfile = new Path(file.getAbsolutePath()).append(
				"version.txt").toFile();

		if (versionfile.exists()) {
			try {
				StringBuffer sb = FileUtil
						.getContent(versionfile, "ISO-8859-1");
				return sb.toString().replaceAll("[\\t ]+", "_");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return "current";
	}

	public boolean interested(String fileType) {
		return UPLOAD_TYPE_FEATURE_SITE.equalsIgnoreCase(fileType);
	}
}
