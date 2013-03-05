package org.emrys.webosgi.common;

import java.util.Collection;

/**
 * 
 * @author Leo Chang
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
	public void addBundleActivatorEntry(Long bundleId,
			ComActivator componentActivator);

	/**
	 * Get the actived bundle's Activator instance.
	 * 
	 * @param bundleSymbleName
	 * @return
	 */
	public ComActivator getBundleActivator(long bundleId);

	/**
	 * Get all invalid Components' activator which without any valid license
	 * attached.
	 * 
	 * @return
	 */
	public ComActivator[] getInvalidCompnents();
}
