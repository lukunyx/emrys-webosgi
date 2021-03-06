package org.emrys.webosgi.common;

import org.emrys.webosgi.common.license.DefaultLicenseProvider;
import org.emrys.webosgi.common.license.LicenseManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;

/**
 * The Activator of Common Component.
 * 
 * @author Leo Chang
 * @version 2010-9-24
 */
public class CommonActivator extends ComActivator {
	public static final String ID = "org.emrys.webosgi.common";

	private static CommonActivator instance;

	/**
	 * return singleton instance of this Activator.
	 * 
	 * @return
	 */
	public static CommonActivator getInstance() {
		return instance;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		instance = this;
		setDelayCheckBundleState(context);
		// Register the default license provider.
		LicenseManager.getInstance().registerLicenseProvider(
				new DefaultLicenseProvider());
		super.start(context);
	}

	/**
	 * Delay util all bundles except system bundle loaded to execute cleaning of
	 * registered activator instance and component bundle id.
	 * 
	 * @param context
	 */
	private void setDelayCheckBundleState(BundleContext context) {
		context.addFrameworkListener(new FrameworkListener() {
			public void frameworkEvent(FrameworkEvent event) {
				if (event.getType() == FrameworkEvent.STARTED) {
					ComponentCore.getInstance().checkBundleActivtorState();
				}
				// In OSGi 3.4, FrameworkEvent.STOPPED not exists, here do
				// nothing when the system bundle stopped.
				/*
				 * if (event.getType() == FrameworkEvent.STOPPED) {
				 * ComponentCore.getInstance().clearBundleActivtorState(); }
				 */
			}
		});
	}
}
