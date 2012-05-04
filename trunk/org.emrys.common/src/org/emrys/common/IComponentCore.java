/*******************************************************************************
 * Copyright (c) 2011 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.common;

import java.util.Collection;

/**
 * 
 * @author Leo Chang - EMRYS
 * @version 2011-7-5
 */
public interface IComponentCore {
	public Collection<ComActivator> getAllComponentActivators();

	/**
	 * Register the bundle and its Activator instance.
	 * 
	 * @param bundleId
	 * @param componentActivator
	 */
	public void addBundleActivatorEntry(Long bundleId, ComActivator componentActivator);

	/**
	 * Get the actived bundle's Activator instance.
	 * 
	 * @param bundleSymbleName
	 * @return
	 */
	public ComActivator getBundleActivator(long bundleId);

	/**
	 * Get all invalid Components' activator which without any valid license attached.
	 * 
	 * @return
	 */
	public ComActivator[] getInvalidCompnents();
}
