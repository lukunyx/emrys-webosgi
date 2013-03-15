package org.emrys.support.site;

import java.io.File;

import javax.servlet.http.HttpServletRequest;

import org.emrys.webosgi.core.resource.extension.IPublishedFileResolver;

/**
 * Resolver for features folder, the clients call http service to get update
 * site.xml file in updating components.
 * 
 * @author Leo Chang
 * @version 2010-10-28
 */
public class FeaturesSiteResolver implements IPublishedFileResolver {

	public File resolve(HttpServletRequest req, String path, String alias,
			String quickID) {
		File root = Activator.getInstance().getFeaUpdateSitesRoot().toFile();
		if (root != null && root.exists())
			return root;

		return null;
	}
}
