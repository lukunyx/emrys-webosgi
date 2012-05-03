/*******************************************************************************
 * Copyright (c) 2010 Hirisun Corporation. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Hirisun License v1.0 which accompanies this
 * distribution, and is available at http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.resources.extension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.RegistryFactory;
import org.emrys.core.runtime.resources.ResroucesCom;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;


/**
 * The ResPublish Extension point registration manager.
 * 
 * @author Leo Chang - Hirisun
 * @version 2010-10-11
 */
public class ResPublishSVCRegister implements BundleListener {
	/**
	 * the extension point's ID.
	 */
	public static final String RES_EXT_POINT_ID = ResroucesCom.getInstance().getBundleSymbleName()
			+ ".resPlublishService";
	/**
	 * the extra resources except those form extension point.
	 */
	private final List<DefinesRoot> extraRepositories = new ArrayList<DefinesRoot>();
	/**
	 * the resource rigistered from extension point.
	 */
	private Vector<DefinesRoot> vReps;
	private boolean internalUpdateMark = false;
	/**
	 * the singleton instance.
	 */
	private static ResPublishSVCRegister instance;

	/**
	 * Get the singleton instance.
	 * 
	 * @return
	 */
	public static ResPublishSVCRegister getInstance() {
		if (instance == null)
			instance = new ResPublishSVCRegister();
		return instance;
	}

	/**
	 * The hidden constructor.
	 */
	protected ResPublishSVCRegister() {
		ResroucesCom.getInstance().getBundle().getBundleContext().addBundleListener(this);
	}

	/**
	 * Get all published resources.
	 * 
	 * @param forceUpdate
	 * @return
	 */
	public List<DefinesRoot> getVirtualRepositories(boolean forceUpdate) {
		if (internalUpdateMark || forceUpdate || vReps == null) {
			internalUpdateMark = false;
			vReps = new Vector<DefinesRoot>();
			IExtensionPoint extPoint = RegistryFactory.getRegistry().getExtensionPoint(
					RES_EXT_POINT_ID);
			IExtension[] exts = extPoint.getExtensions();
			for (IExtension ext : exts) {
				DefinesRoot repo = new DefinesRoot();
				repo.setId(ext.getUniqueIdentifier());
				repo.setName(ext.getLabel());
				repo.setVisitControler(getVisitController(ext));
				initResolvers(ext, repo);
				initFolders(ext, repo);
				initFiles(ext, repo);
				String contributeBundleName = ext.getContributor().getName();
				Bundle bundle = Platform.getBundle(contributeBundleName);
				repo.setSourceBundle(bundle);
				mappingResolvers(repo);
				vReps.add(repo);
			}
		}

		ArrayList<DefinesRoot> result = new ArrayList<DefinesRoot>(vReps);
		result.addAll(extraRepositories);
		return result;
	}

	/**
	 * Map the resource to its resource.
	 * 
	 * @param repo
	 */
	private void mappingResolvers(DefinesRoot repo) {
		List<BaseResource> reses = repo.getResources();
		for (BaseResource res : reses) {
			mappingResolvers(res, repo);
		}
	}

	/**
	 * Map the resource to its resource.
	 * 
	 * @param res
	 * @param repo
	 */
	private void mappingResolvers(BaseResource res, DefinesRoot repo) {
		if (res.getResolverID() != null) {
			IPublishedFileResolver resolver = repo.getResResolvers().get(res.getResolverID());
			res.setResolver(resolver);
		}

		if (res instanceof ResFolder) {
			List<BaseResource> reses = ((ResFolder) res).getResources();
			for (BaseResource r : reses) {
				mappingResolvers(r, repo);
			}
		}
	}

	/**
	 * @param element
	 * @param repo
	 */
	private void initFiles(Object element, Object parent) {
		List<IConfigurationElement> ces = getConfigurationElements(element, "file");
		for (IConfigurationElement ce : ces) {
			String path = ce.getAttribute("path");
			String quick_ID = ce.getAttribute("quik-visit-id");
			String resolveId = ce.getAttribute("resolver-id");
			String alias = ce.getAttribute("alias");
			ResFile res = new ResFile();
			res.setPath(path);
			res.setAlias(alias);
			res.setQuickID(quick_ID);
			res.setResolverID(resolveId);
			res.setVisitControler(getVisitController(ce));
			if (parent instanceof DefinesRoot)
				((DefinesRoot) parent).getResources().add(res);
			if (parent instanceof ResFolder)
				((ResFolder) parent).getResources().add(res);
		}
	}

	private void initResolvers(IExtension root, DefinesRoot rootModel) {
		List<IConfigurationElement> ces = getConfigurationElements(root, "resolver");
		for (IConfigurationElement ce : ces) {
			try {
				String id = ce.getAttribute("id");
				IPublishedFileResolver resolver = (IPublishedFileResolver) ce
						.createExecutableExtension("class");
				rootModel.getResResolvers().put(id, resolver);
			} catch (InvalidRegistryObjectException e) {
				e.printStackTrace();
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @param ext
	 * @param repo
	 */
	private void initFolders(Object element, Object parent) {
		List<IConfigurationElement> ces = getConfigurationElements(element, "folder");
		for (IConfigurationElement ce : ces) {
			String path = ce.getAttribute("path");
			String quick_ID = ce.getAttribute("quik-visit-id");
			String alias = ce.getAttribute("quik-visit-id");
			String resolveId = ce.getAttribute("resolver-id");
			ResFolder subFolder = new ResFolder();
			subFolder.setPath(path);
			subFolder.setAlias(alias);
			subFolder.setQuickID(quick_ID);
			subFolder.setResolverID(resolveId);
			subFolder.setVisitControler(getVisitController(ce));
			if (parent instanceof DefinesRoot)
				((DefinesRoot) parent).getResources().add(subFolder);
			if (parent instanceof ResFolder)
				((ResFolder) parent).getResources().add(subFolder);

			initFolders(ce, subFolder);
			initFiles(ce, subFolder);
		}
	}

	/**
	 * @param ext
	 * @param repo
	 * @return
	 * 
	 */
	private IResourceVisitController getVisitController(Object parentEle) {
		List<IConfigurationElement> ces = getConfigurationElements(parentEle, "authority");
		for (IConfigurationElement ce : ces) {
			try {
				IResourceVisitController controller = (IResourceVisitController) ce
						.createExecutableExtension("visit-controller");
				return controller;
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private List<IConfigurationElement> getConfigurationElements(Object parent, String name) {
		IConfigurationElement[] ces = new IConfigurationElement[0];
		if (parent instanceof IExtension)
			ces = ((IExtension) parent).getConfigurationElements();
		if (parent instanceof IConfigurationElement)
			ces = ((IConfigurationElement) parent).getChildren();

		if (name != null && name.length() > 0) {
			List result = new ArrayList<IConfigurationElement>();
			for (IConfigurationElement ce : ces) {
				if (ce.getName().equals(name)) {
					result.add(ce);
				}
			}
			return result;
		} else
			return Arrays.asList(ces);
	}

	public List<DefinesRoot> getExtraRepositories() {
		return extraRepositories;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.framework.BundleListener#bundleChanged(org.osgi.framework.BundleEvent)
	 */
	public void bundleChanged(BundleEvent arg0) {
		// If any bunlde stop, start event, force update internal buffer.
		internalUpdateMark = true;
	}
}
