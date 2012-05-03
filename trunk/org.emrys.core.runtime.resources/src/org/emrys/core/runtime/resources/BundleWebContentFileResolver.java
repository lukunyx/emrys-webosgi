/*******************************************************************************
 * Copyright (c) 2010 Hirisun Corporation. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Hirisun License v1.0 which accompanies this
 * distribution, and is available at http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.resources;

import java.io.File;

import javax.servlet.http.HttpServletRequest;

import org.emrys.common.IComActivator;
import org.emrys.common.util.BundleServiceUtil;
import org.emrys.core.runtime.WebComActivator;
import org.emrys.core.runtime.resources.extension.IPublishedFileResolver;
import org.osgi.framework.Bundle;


/**
 * The web bundle resource resolver to publish the whole web content.
 * 
 * @author Leo Chang - Hirisun
 * @version 2010-11-2
 */
public class BundleWebContentFileResolver implements IPublishedFileResolver {
	/**
	 * the web bundle.
	 */
	private final Bundle bundle;

	/**
	 * Constructor of a given web bundle.
	 * 
	 * @param bundle
	 * @param webConentPath
	 *            the web content path relative to the bundle's root.
	 */
	public BundleWebContentFileResolver(Bundle bundle, String webConentPath) {
		this.bundle = bundle;
	}

	public File resolve(HttpServletRequest req, String path, String alias, String quickID) {
		try {
			IComActivator activator = BundleServiceUtil.getBundleActivator(bundle);
			if (activator instanceof WebComActivator) {
				WebComActivator webBundleActivator = (WebComActivator) activator;
				// unzip webContent file form jar.
				File tmpWebRoot = webBundleActivator.getResolvedWebContentRoot(false);
				return tmpWebRoot;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
}
