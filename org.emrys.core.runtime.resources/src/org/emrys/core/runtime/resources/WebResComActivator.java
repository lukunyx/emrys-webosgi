/*******************************************************************************
 * Copyright (c) 2011 Hirisun Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Hirisun License v1.0
 * which accompanies this distribution, and is available at
 * http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.resources;

import java.io.File;

import org.emrys.core.runtime.ServiceInitException;
import org.emrys.core.runtime.WebComActivator;
import org.emrys.core.runtime.resources.extension.DefinesRoot;
import org.osgi.framework.BundleContext;


/**
 * This Activator inherit from WebComponentActivator and publish all resources
 * on Web Content path (see {@link WebComActivator#getResolvedWebContentRoot(boolean)}).
 * 
 * @author Leo Chang
 */
public class WebResComActivator extends WebComActivator {
	/**
	 * The resource repository root of this web component..
	 */
	private DefinesRoot publishedResRoot = null;

	@Override
	public void stop(BundleContext context) throws Exception {
		if (publishedResRoot != null)
			WebResCore.getInstance().unregisterWebContextRoot(publishedResRoot);
		super.stop(context);
	}

	@Override
	public void initWebConfig() throws ServiceInitException {
		super.initWebConfig();
		// Publish this Web Bundle's Resource
		File webContent = this.getResolvedWebContentRoot(false);
		if (webContent != null) {
			publishedResRoot = WebResCore.getInstance().registerWebContextRoot(this.getBundle(),
					this.getServiceNSPrefix(), webContent.getAbsolutePath());

			// the default resource visit controller will forbid the folder
			// browsing and file
			// modifying. If any component has to change this feature, do like
			// following.
			/*
			 * publishedResRoot.setVisitControler(new IResourceVisitController()
			 * { public boolean canRead(HttpServletRequest requestUrl) { return
			 * true; }
			 * 
			 * public boolean canModify(HttpServletRequest requestUrl) { return
			 * false; }
			 * 
			 * public boolean canBrowseFolder(HttpServletRequest req) { return
			 * false; } });
			 */
		}
	}
}
