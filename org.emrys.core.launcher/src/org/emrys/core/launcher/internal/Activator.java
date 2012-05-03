package org.emrys.core.launcher.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	private static BundleContext bundleCtx;

	public void start(BundleContext context) throws Exception {
		this.bundleCtx = context;
		// Set the mark indicating whether the Framework embedded in OSGi
		// runtime.
		OSGiFwkLauncher.osgiEmbedded = true;
	}

	public void stop(BundleContext context) throws Exception {
		// Do nothing...
	}

	public static BundleContext getBundleContext() {
		return bundleCtx;
	}
}
