/*******************************************************************************
 * Copyright (c) 2010 Hirisun Corporation. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Hirisun License v1.0 which accompanies this
 * distribution, and is available at http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.common.util;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.resolver.BaseDescription;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.osgi.service.resolver.PlatformAdmin;
import org.eclipse.osgi.service.resolver.State;
import org.emrys.common.CommonActivator;
import org.emrys.common.ComponentCore;
import org.emrys.common.IComActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;


/**
 * 
 * @author Leo Chang - Hirisun
 * @version 2010-10-14
 */
public class BundleServiceUtil {
	private static PlatformAdmin platformAdmin;
	private static PackageAdmin packageAdmin;

	public static IPath findBundleLocation(Bundle bundle) {
		File bundleFile;
		try {
			bundleFile = FileLocator.getBundleFile(bundle);
			return new Path(bundleFile.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param bundleId
	 * @return
	 */
	public static Bundle findBundleBySymbolName(String symbolicName) {
		return Platform.getBundle(symbolicName);
		/*
		 * Bundle[] bundles =
		 * CommonActivator.getDefault().getBundle().getBundleContext().getBundles(); for (Bundle
		 * bundle : bundles) { if (bundleId.equals(bundle.getBundleId())) return bundle; } return
		 * null;
		 */
	}

	/**
	 * Get Component bundle's activator instance by reflect invoke the singleton getInstance()
	 * mehotd of the Acitvator class.
	 * 
	 * @param bundle
	 * @return
	 * @throws CoreException
	 */
	public static IComActivator getBundleActivator(Bundle bundle) throws CoreException {
		return ComponentCore.getInstance().getBundleActivator(bundle.getBundleId());
	}

	/**
	 * Get bundle's Activator Class
	 * 
	 * @param bundle
	 * @return
	 */
	public static Class getBundleActivatorClass(Bundle bundle) {
		try {
			String activator = (String) bundle.getHeaders().get(Constants.BUNDLE_ACTIVATOR);
			if (activator != null) {
				Class activatorClass = bundle.loadClass(activator);
				return activatorClass;
			}
		} catch (Throwable t) {
			// t.printStackTrace();
		}
		return null;
	}

	/**
	 * The convenient static method to get ServiceTracker
	 * 
	 * @param service
	 * @return
	 */
	public static Object getServiceTracker(Class<?> service) {
		BundleContext context = CommonActivator.getInstance().getBundle().getBundleContext();
		ServiceTracker serviceTracker = new ServiceTracker(context, service.getName(), null);
		serviceTracker.open();

		return serviceTracker.getService();
	}

	/**
	 * Get resolved bundles in the current platform state.
	 * 
	 * @param bundle
	 *            the bundle for which to find all resolved required bundles.
	 * @param includeOptional
	 *            whether to care about optional required bundle.
	 * @param onlyReexported
	 *            whether only to care about reexport bundle.
	 * @param onlyDirectRequired
	 *            whether only to find the direct required bundle(include whose be reexported from
	 *            required bundle)
	 * @return
	 */
	public static Collection<BundleDescription> getRequiredBoudles(Bundle bundle,
			boolean includeOptional, boolean onlyReexported, boolean onlyDirectRequired) {
		// Need resolver.
		State platformState = getPlatformAdmin().getState(false);
		BundleDescription bundleDes = platformState.getBundle(bundle.getBundleId());
		return getResolvedRequiredBundles(bundleDes, includeOptional, onlyReexported,
				onlyDirectRequired);
	}

	/**
	 * Get resolved bundles in the current platform state.
	 * 
	 * @param bundleDes
	 *            the {@link BundleDescription} for which to find all resolved required bundles.
	 * @param includeOptional
	 *            whether to care about optional required bundle.
	 * @param onlyReexported
	 *            whether only to care about reexport bundle.
	 * @param onlyDirectRequired
	 *            whether only to find the direct required bundle(include whose be reexported from
	 *            required bundle)
	 * @return
	 */
	public static Collection<BundleDescription> getResolvedRequiredBundles(
			BundleDescription bundleDes, boolean includeOptional, boolean onlyReexported,
			boolean onlyDirectRequired) {
		if (bundleDes == null)
			return Collections.EMPTY_LIST;
		Set<BundleDescription> result = new HashSet<BundleDescription>();

		if (!bundleDes.isResolved()) {
			bundleDes.getContainingState().updateBundle(bundleDes);
			// bundleDes.getContainingState().resolveBundle(bundleDes, bundleDes.isResolved(), null,
			// null, null, null, null);
		}
		BundleSpecification[] requiredBundleSpeces = bundleDes.getRequiredBundles();
		for (BundleSpecification bds : requiredBundleSpeces) {
			if (!includeOptional && bds.isOptional())
				continue;

			if (onlyReexported && !bds.isExported())
				continue;

			if (bds.isResolved() && bds.getSupplier() instanceof BundleDescription) {
				BundleDescription bd = (BundleDescription) bds.getSupplier();
				result.add(bd);
				if (!onlyDirectRequired || bds.isExported())
					result.addAll(getResolvedRequiredBundles(bd, includeOptional,
							onlyDirectRequired, onlyDirectRequired));
			}
		}

		if (!onlyReexported) {
			ImportPackageSpecification[] importPkSpeces = bundleDes.getImportPackages();
			for (ImportPackageSpecification ipks : importPkSpeces) {
				if (!includeOptional
						&& ImportPackageSpecification.RESOLUTION_OPTIONAL.equals(ipks
								.getDirective(Constants.RESOLUTION_DIRECTIVE)))
					continue;

				if (ipks.isResolved()) {
					BaseDescription supplier = ipks.getSupplier();
					if (supplier instanceof ExportPackageDescription) {
						result.add(((ExportPackageDescription) supplier).getExporter());
					}
					if (supplier instanceof BundleDescription) {
						result.add((BundleDescription) supplier);
					}
				}
			}
		}

		return result;
	}

	/**
	 * @return PackageAdmin
	 */
	public static PackageAdmin getPackageAdmin() {
		if (packageAdmin == null)
			packageAdmin = (PackageAdmin) getServiceTracker(PackageAdmin.class);
		return packageAdmin;
	}

	/**
	 * @return PlatformAdmin
	 */
	public static PlatformAdmin getPlatformAdmin() {
		if (platformAdmin == null) {
			BundleContext bundleContext = CommonActivator.getInstance().getBundle()
					.getBundleContext();
			ServiceReference platformAdminReference = bundleContext
					.getServiceReference(PlatformAdmin.class.getName());
			if (platformAdminReference == null)
				return null;
			platformAdmin = (PlatformAdmin) bundleContext.getService(platformAdminReference);
		}
		return platformAdmin;
	}

	/**
	 * @param pluginID
	 * @return
	 */
	public static Bundle getBundle(long pluginID) {
		return CommonActivator.getInstance().getBundle().getBundleContext().getBundle(pluginID);
	}
}
