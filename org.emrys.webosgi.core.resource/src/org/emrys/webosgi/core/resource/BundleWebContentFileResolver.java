package org.emrys.webosgi.core.resource;

import java.io.File;

import javax.servlet.http.HttpServletRequest;

import org.emrys.webosgi.core.resource.extension.IPublishedFileResolver;
import org.osgi.framework.Bundle;

/**
 * The web bundle resource resolver to publish the whole web content.
 * 
 * @author Leo Chang
 * @version 2010-11-2
 */
public class BundleWebContentFileResolver implements IPublishedFileResolver {
	/**
	 * the web bundle.
	 */
	private final Bundle bundle;
	private final String webContentPath;

	/**
	 * Constructor of a given web bundle.
	 * 
	 * @param bundle
	 * @param webConentPath
	 *            the local WebContent path of bundle.
	 */
	public BundleWebContentFileResolver(Bundle bundle, String webContentPath) {
		this.bundle = bundle;
		this.webContentPath = webContentPath;
	}

	public File resolve(HttpServletRequest req, String path, String alias,
			String quickID) {
		// Here we not take path argument into according, but just return the
		// WebContent root file. Here we can use Bundle.getEntry() for given
		// path to implement accoring to the OSGi 4.2 of Enterprise
		// specification.
		File tmpWebRoot = new File(webContentPath);
		if (tmpWebRoot.exists())
			return tmpWebRoot;
		return null;
	}
}
