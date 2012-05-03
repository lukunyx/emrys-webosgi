/*******************************************************************************
 * Copyright (c) 2010 Hirisun Corporation. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Hirisun License v1.0 which accompanies this
 * distribution, and is available at http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.RegistryFactory;
import org.emrys.common.ComActivator;
import org.emrys.common.IComActivator;
import org.emrys.common.util.BundleServiceUtil;
import org.emrys.core.runtime.FwkActivator;
import org.emrys.core.runtime.IFwkConstants;
import org.emrys.core.runtime.IWebComActivator;
import org.emrys.core.runtime.WebComActivator;
import org.emrys.core.runtime.extension.DefaultWebContentIndentifier;
import org.emrys.core.runtime.extension.IWebContentPathProvider;
import org.emrys.core.runtime.internal.FwkRuntime;
import org.osgi.framework.Bundle;


/**
 * 
 * @author Leo Chang
 * @version 2010-10-13
 */
public class WebBundleUtil implements IFwkConstants {
	private static final String WEBCONTENT_EXT_POINT_ID = FwkActivator.getInstance()
			.getBundleSymbleName()
			+ ".webContentPaths";
	private static Map<String, DefaultWebContentIndentifier> webContentPathMaps;
	private static Bundle webHostBundle;

	public static boolean isHostWebBundle(String bundleId) {
		Bundle bundle = BundleServiceUtil.findBundleBySymbolName(bundleId);
		return bundle != null && bundle == findHostWebBundle();
	}

	public static boolean isHostWebBundle(Bundle bundle) {
		return bundle == findHostWebBundle();
	}

	/**
	 * Find Host wen bundle
	 * 
	 * @return
	 */
	public static Bundle findHostWebBundle() {
		if (webHostBundle != null) {
			return webHostBundle;
		}

		Collection<ComActivator> activators = FwkRuntime.getInstance().getAllComponentActivators();
		for (ComActivator activator : activators) {
			if (activator instanceof WebComActivator) {
				if (((WebComActivator) activator).isHostWebBundle())
					return webHostBundle = activator.getBundle();
			}
		}

		return null;
	}

	/**
	 * Find web bundle's namespace urlPattern.
	 * 
	 * @param bundle
	 * @return
	 */
	public static String findNSPrefixOfWebBundle(Bundle bundle) {
		try {
			IComActivator activator = BundleServiceUtil.getBundleActivator(bundle);
			if (activator instanceof IWebComActivator) {
				return ((IWebComActivator) activator).getServiceNSPrefix();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Find web bundle's namespace.
	 * 
	 * @param bundle
	 * @return
	 */
	public static String findNSOfWebBundle(Bundle bundle) {
		try {
			IComActivator activator = BundleServiceUtil.getBundleActivator(bundle);
			if (activator instanceof IWebComActivator) {
				return ((IWebComActivator) activator).getServiceNS();
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Find a Web Component bundle by its namespace.
	 * 
	 * @param namespace
	 * @return
	 */
	public static Bundle findWebBundleByNS(String namespace) {
		List<Bundle> allWebBundles = findAllWebBundle();
		for (Bundle bundle : allWebBundles) {
			try {
				IComActivator activator = BundleServiceUtil.getBundleActivator(bundle);
				if (activator instanceof IWebComActivator) {
					String ns = ((IWebComActivator) activator).getServiceNS();
					if (namespace.equals(ns))
						return bundle;
				}
			} catch (Exception e) {
				// e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * Find a Web Component bundle by its Namespace Prefix.
	 * 
	 * @param nsPrefix
	 * @return
	 */
	public static Bundle findWebBundleByNSPrefix(String nsPrefix) {
		List<Bundle> allWebBundles = findAllWebBundle();
		for (Bundle bundle : allWebBundles) {
			try {
				IComActivator activator = BundleServiceUtil.getBundleActivator(bundle);
				if (activator instanceof IWebComActivator) {
					String prefix = ((IWebComActivator) activator).getServiceNSPrefix();
					if (nsPrefix.equals(prefix))
						return bundle;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public static List<Bundle> findAllWebBundle() {
		List<Bundle> result = new ArrayList<Bundle>();
		Bundle[] bundles = FwkActivator.getInstance().getBundle().getBundleContext().getBundles();
		for (Bundle bundle : bundles) {
			Iterator<Entry<String, DefaultWebContentIndentifier>> it = getWebContentRegInfo(false)
					.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, DefaultWebContentIndentifier> e = it.next();
				String path = e.getKey();
				DefaultWebContentIndentifier indentifier = e.getValue();
				if (indentifier.recgnize(bundle, path))
					result.add(bundle);
				else {
					// Search Host Web Bundle
					IPath bundleLocation = BundleServiceUtil.findBundleLocation(bundle);
					File bundleFile = bundleLocation.toFile();
					if (bundleFile.exists() && bundleFile.isDirectory()
							&& recgnizeWorkspaceWebContent(bundleLocation, "")) {
						result.add(bundle);
					}
				}
			}
		}

		return result;
	}

	public static InputStream findWebXmlOfBundle(Bundle bundle) throws Exception {
		if (webHostBundle == null) {
			webHostBundle = findHostWebBundle();
		}

		if (webHostBundle == bundle) {
			File hostBundleFile = FileLocator.getBundleFile(webHostBundle);
			IPath webXmlPath = new Path(hostBundleFile.getAbsolutePath()).append("WEB-INF/"
					+ HOST_WEB_XML_NAME);
			return new FileInputStream(webXmlPath.toFile());
		}

		String webContentPath = findWebContentPath(bundle);
		if (webContentPath != null) {
			IPath webXmlPath = new Path(webContentPath).append(new Path("WEB-INF/web.xml"));
			URL[] es = FileLocator.findEntries(bundle, webXmlPath);
			if (es.length > 0) {
				return FileLocator.toFileURL(es[0]).openStream();
			}
		}

		return null;
	}

	/**
	 * Get the file URL of the Web Content root of a bundle. NOTE: if this
	 * bundle is a jar file, the web content will be unzipped to a temporary
	 * folder and return this temporary folder's file url. If you want to get
	 * real web content folder in the local workspace, use
	 * getResolvedBundleWebContent() for instead.
	 * 
	 * @param bundle
	 * @return
	 */
	public static URL findWebContentURL(Bundle bundle) {
		String webContentPathStr = findWebContentPath(bundle);
		if (webContentPathStr != null) {
			IPath webContentPath = new Path(webContentPathStr);
			URL[] es = FileLocator.findEntries(bundle, webContentPath);
			if (es.length > 0) {
				try {
					return FileLocator.toFileURL(es[0]);
				} catch (IOException e) {
					// e.printStackTrace();
				}
			}
		}

		return null;
	}

	public static String findWebContentPath(IPath workspaceBundlePath) {
		Iterator<Entry<String, DefaultWebContentIndentifier>> it = getWebContentRegInfo(false)
				.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, DefaultWebContentIndentifier> en = it.next();
			String path = en.getKey();
			if (recgnizeWorkspaceWebContent(workspaceBundlePath, path)) {
				return new Path(path).toPortableString();
			} else {
				// Search Host Web Bundle
				File bundleFile = workspaceBundlePath.toFile();
				if (bundleFile.exists() && bundleFile.isDirectory()
						&& recgnizeWorkspaceWebContent(workspaceBundlePath, "")) {
					return workspaceBundlePath.toPortableString();
				}
			}
		}

		return null;
	}

	public static File getResolvedBundleWebContent(Bundle bundle, boolean forceUpdate) {
		try {
			IComActivator activator = BundleServiceUtil.getBundleActivator(bundle);
			if (activator instanceof IWebComActivator) {
				IWebComActivator webComponentActivator = (IWebComActivator) activator;
				return webComponentActivator.getResolvedWebContentRoot(forceUpdate);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Find the web content path relative to the bundle root.
	 * 
	 * @param bundle
	 * @return
	 */
	public static String findWebContentPath(Bundle bundle) {
		Iterator<Entry<String, DefaultWebContentIndentifier>> it = getWebContentRegInfo(false)
				.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, DefaultWebContentIndentifier> en = it.next();
			String path = en.getKey();
			DefaultWebContentIndentifier indentifier = en.getValue();
			if (indentifier.recgnize(bundle, path)) {
				return new Path(path).toPortableString();
			} else {
				// Search Host Web Bundle
				if (bundle == findHostWebBundle())
					return "/";// BundleServiceUtil.findBundleLocation(bundle).toPortableString();
			}
		}

		return null;
	}

	public static Map<String, DefaultWebContentIndentifier> getWebContentRegInfo(boolean forceUpdate) {
		if (forceUpdate || webContentPathMaps == null) {
			webContentPathMaps = new HashMap<String, DefaultWebContentIndentifier>();
			IExtensionPoint extPoint = RegistryFactory.getRegistry().getExtensionPoint(
					WEBCONTENT_EXT_POINT_ID);
			IConfigurationElement[] eles = extPoint.getConfigurationElements();
			for (IConfigurationElement ce : eles) {
				try {
					String path = ce.getAttribute("bundle-path");
					DefaultWebContentIndentifier indentifier = (DefaultWebContentIndentifier) ce
							.createExecutableExtension("indentifier-class");
					IWebContentPathProvider provider = null;

					try {
						provider = (IWebContentPathProvider) ce
								.createExecutableExtension("path-providers");
					} catch (Exception e) {
						// e.printStackTrace();
					}

					if (indentifier != null) {
						if (provider != null) {
							String[] paths = provider.getWebContentBundlePath();
							for (String p : paths) {
								webContentPathMaps.put(p, indentifier);
							}
						} else if (path != null)
							webContentPathMaps.put(path, indentifier);
					}
				} catch (InvalidRegistryObjectException e) {
					e.printStackTrace();
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}
		return webContentPathMaps;
	}

	public static boolean recgnizeWorkspaceWebContent(IPath bundlePath, String webContentBundlePath) {
		IPath path = bundlePath.append(webContentBundlePath).append(new Path("WEB-INF/web.xml"));
		if (!path.toFile().exists())
			return false;
		return true;
	}
}
