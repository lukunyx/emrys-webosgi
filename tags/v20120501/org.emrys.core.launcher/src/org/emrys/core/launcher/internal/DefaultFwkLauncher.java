/**
 * 
 */
package org.emrys.core.launcher.internal;

import javax.servlet.ServletConfig;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * @author Leo Chang
 */
public class DefaultFwkLauncher implements IFwkLauncher {

	private static final String FWK_RUNTIME_BUNDLE_ID = "org.emrys.core.runtime";

	protected FwkExternalAgent fwkAgent = FwkExternalAgent.getInstance();
	protected OSGiFwkLauncher osgiFwkLauncher;

	public void init() {
		// Check whether the Framework embedded in OSGi runtime and set the mark
		// attribute.
		fwkAgent.setFwkEvnAttribute(ATTR_FWK_OSGI_EMBEDDED, Boolean
				.valueOf(OSGiFwkLauncher.isFwkOSGiEmbedded()));

		// If not embeded in existing OSGi Framework, create a OSGi Launcher.
		if (!OSGiFwkLauncher.isFwkOSGiEmbedded()) {
			osgiFwkLauncher = new OSGiFwkLauncher();
			osgiFwkLauncher.init();
		}
	}

	public void deploy() {
		if (!OSGiFwkLauncher.isFwkOSGiEmbedded())
			osgiFwkLauncher.deploy();
	}

	public void start() throws Exception {
		if (OSGiFwkLauncher.isFwkOSGiEmbedded())
			startRuntime();
		else
			startOSGiFramework();
	}

	private void startRuntime() throws BundleException {
		PackageAdmin packageAdmin = getPackageAdmin();
		Bundle[] bundles = packageAdmin.getBundles(FWK_RUNTIME_BUNDLE_ID, null);
		if (bundles == null || bundles.length == 0)
			throw new IllegalStateException(
					"Framework Runtime Bundle not exists.");
		Bundle runtimeBundle = bundles[0];
		if (runtimeBundle.getState() != Bundle.ACTIVE)
			runtimeBundle.start();
	}

	private void startOSGiFramework() {
		osgiFwkLauncher.start();
	}

	private PackageAdmin getPackageAdmin() {
		ServiceTracker serviceTracker = new ServiceTracker(Activator
				.getBundleContext(), PackageAdmin.class.getName(), null);
		serviceTracker.open();

		return (PackageAdmin) serviceTracker.getService();
	}

	public void stop() {
		if (!OSGiFwkLauncher.isFwkOSGiEmbedded())
			osgiFwkLauncher.stop();
	}

	public void destroy() {
		if (!OSGiFwkLauncher.isFwkOSGiEmbedded())
			osgiFwkLauncher.destroy();
	}

	public String getEnviromentInfo() {
		return "No Enviroment Information available.";
	}

	public void init(ServletConfig servletConfig) {
		init();
	}
}
