/*******************************************************************************
 * Copyright (c) 2011 Hirisun Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Hirisun License v1.0
 * which accompanies this distribution, and is available at
 * http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.emrys.common.license.License;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;


/**
 * Provide common method to manipulate the components current in the framework.
 * 
 * @author Leo Chang - Hirisun
 * @version 2011-6-8
 */
public class ComponentCore implements IComponentCore {
	private static ComponentCore instance;
	private final static Map<Long, ComActivator> bundleActivatorMap = new HashMap<Long, ComActivator>();

	/**
	 * @return the singleton instance of this ComponentCore
	 */
	public static ComponentCore getInstance() {
		if (instance == null)
			instance = new ComponentCore();
		return instance;
	}

	protected ComponentCore() {
	}

	/**
	 * Get all registered activator instance of Components.
	 * 
	 * @return
	 */
	public Collection<ComActivator> getAllComponentActivators() {
		return bundleActivatorMap.values();
	}

	/**
	 * Register the bundle and its Activator instance.
	 * 
	 * @param bundleId
	 * @param componentActivator
	 */
	public void addBundleActivatorEntry(Long bundleId, ComActivator componentActivator) {
		if (bundleId == null || componentActivator == null)
			throw new IllegalArgumentException("bundle id or activator cann't be null or empty.");
		synchronized (bundleActivatorMap) {
			bundleActivatorMap.put(bundleId, componentActivator);
		}
	}

	/**
	 * Get the actived bundle's Activator instance.
	 * 
	 * @param bundleSymbleName
	 * @return
	 */
	public ComActivator getBundleActivator(long bundleId) {
		return bundleActivatorMap.get(bundleId);
	}

	/**
	 * Remove all not active bundles
	 */
	public void checkBundleActivtorState() {
		synchronized (bundleActivatorMap) {
			BundleContext bundleContext = CommonActivator.getInstance().getBundle()
					.getBundleContext();
			Iterator<Long> eit = bundleActivatorMap.keySet().iterator();
			while (eit.hasNext()) {
				long bundleId = eit.next();
				Bundle bundle = bundleContext.getBundle(bundleId);
				if (bundle.getState() != Bundle.ACTIVE) {
					eit.remove();
				}
			}
		}

	}

	/**
	 * Clean the registered components.
	 */
	public void clearBundleActivtorState() {
		synchronized (bundleActivatorMap) {
			bundleActivatorMap.clear();
		}
	}

	/**
	 * Get all invalid Components' activator which without any valid license attached.
	 * 
	 * @return
	 */
	public ComActivator[] getInvalidCompnents() {
		Iterator<ComActivator> vit = bundleActivatorMap.values().iterator();
		List<ComActivator> result = new ArrayList<ComActivator>();
		while (vit.hasNext()) {
			ComActivator activator = vit.next();
			License license = activator.getValidLicense();
			if (license == null) {
				result.add(activator);
			}
		}
		return result.toArray(new ComActivator[result.size()]);
	}
}
