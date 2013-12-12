/**
 * 
 */
package org.emrys.webosgi.launcher.osgi;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * @author LeoChang
 * 
 */
public class Activator implements BundleActivator {

	static BundleContext BUNDLE_CTX;

	public void start(BundleContext bundleCtx) throws Exception {
		this.BUNDLE_CTX = bundleCtx;
	}

	public void stop(BundleContext bundleCtx) throws Exception {
	}
}
