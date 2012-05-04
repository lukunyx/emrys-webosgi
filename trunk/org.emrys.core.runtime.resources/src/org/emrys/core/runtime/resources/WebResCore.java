/*******************************************************************************
 * Copyright (c) 2011 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.resources;

import javax.servlet.http.HttpServletRequest;

import org.emrys.core.runtime.resources.extension.DefinesRoot;
import org.emrys.core.runtime.resources.extension.IPublishedFileResolver;
import org.emrys.core.runtime.resources.extension.IResourceVisitController;
import org.emrys.core.runtime.resources.extension.ResFolder;
import org.emrys.core.runtime.resources.extension.ResPublishSVCRegister;
import org.osgi.framework.Bundle;


/**
 * The File Service Core of Framework to provide the entrance method for File
 * Service.
 * 
 * @author Leo Chang
 * @version 2011-3-24
 */
public final class WebResCore {
	/**
	 * the singleton instance.
	 */
	private static WebResCore instance;
	/**
	 * the extension point register.
	 */
	private final ResPublishSVCRegister register;

	/**
	 * @return the singleton instance.
	 */
	public static WebResCore getInstance() {
		if (instance == null)
			instance = new WebResCore();
		return instance;
	}

	/**
	 * Default hidden constructor.
	 */
	protected WebResCore() {
		// initialize extension point register.
		register = ResPublishSVCRegister.getInstance();
		register.getVirtualRepositories(true);
	}

	/**
	 * register Web Content of a web bundle to publish.
	 * 
	 * @param bundle
	 * @param prefix
	 * @param rootPath
	 * @return
	 */
	public DefinesRoot registerWebContextRoot(Bundle bundle, String prefix,
			String rootPath) {
		String webBundleNsPrefix = prefix;
		DefinesRoot root = new DefinesRoot();
		ResFolder webContentFoler = new ResFolder();
		webContentFoler.setPath("/" + webBundleNsPrefix);
		webContentFoler.setAlias("/" + webBundleNsPrefix);
		IPublishedFileResolver resolver = new BundleWebContentFileResolver(
				bundle, rootPath);
		webContentFoler.setResolverID(resolver.getClass().getName());
		webContentFoler.setResolver(resolver);
		root.getResResolvers().put(resolver.getClass().getName(), resolver);
		root.getResources().add(webContentFoler);
		root.setSourceBundle(bundle);
		root.setName(prefix);
		root.setId(bundle.getSymbolicName());
		root.setVisitControler(new IResourceVisitController() {
			public boolean canRead(HttpServletRequest requestUrl) {
				return true;
			}

			public boolean canModify(HttpServletRequest requestUrl) {
				return false;
			}

			public boolean canBrowseFolder(HttpServletRequest req) {
				return false;
			}

			public long getLastModifiedTimeMillis() {
				return 0L;
			}
		});
		register.getExtraRepositories().add(root);
		refreshRepository();
		return root;
	}

	/**
	 * Unregister the web content of the web bundle.
	 * 
	 * @param root
	 */
	public void unregisterWebContextRoot(DefinesRoot root) {
		register.getExtraRepositories().remove(root);
		refreshRepository();
	}

	/**
	 * Refresh extension point register of Published Resource.
	 */
	public void refreshRepository() {
		register.getVirtualRepositories(true);
	}
}
