/**
 * 
 */
package org.emrys.webosgi.core.extender;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.emrys.webosgi.core.FwkActivator;
import org.emrys.webosgi.core.internal.FwkRuntime;
import org.emrys.webosgi.core.runtime.WebApplication;
import org.emrys.webosgi.core.service.IOSGiWebContainer;
import org.emrys.webosgi.core.service.IWebApplication;
import org.emrys.webosgi.core.util.WebBundleUtil;
import org.osgi.framework.Bundle;


/**
 * @author LeoChang
 * 
 */
public class DefaultWabDeployer implements WABDeployer {
	protected final IOSGiWebContainer webContainer;

	public DefaultWabDeployer() {
		this.webContainer = FwkActivator.getInstance().getJeeContainer();
	}

	public void deploy(Bundle wabundle) throws Exception {
		if (wabundle.getState() != Bundle.ACTIVE
				|| !WebBundleUtil.isWebAppBundle(wabundle))
			return;

		// Search application extensipn point.
		IWebApplication app = findApplication(wabundle);
		if (app != null && app.isDynaServicesStarted())
			return;

		if (app == null) {
			WebApplication tmpApp = new WebApplication(wabundle);
			FwkRuntime.getInstance().regApplication(tmpApp);
			app = tmpApp;
		}

		app.startDynamicServices();
	}

	public void undeploy(Bundle wabundle) throws Exception {
		// Search application extension point.
		IWebApplication app = findApplication(wabundle);
		if (app != null && app.isDynaServicesStarted())
			app.stopDynamicServices();

		return;
	}

	protected IWebApplication findApplication(Bundle bundle) {
		FwkActivator fwkActivator = FwkActivator.getInstance();
		IWebApplication app = FwkRuntime.getInstance().getAppliction(bundle);
		if (app != null)
			return app;
		IExtensionPoint extPoint = Platform.getExtensionRegistry()
				.getExtensionPoint(
						fwkActivator.getBundleSymbleName()
								+ ".JeeWebApplication");
		IConfigurationElement[] ces = extPoint.getConfigurationElements();
		for (IConfigurationElement ce : ces) {
			String bundleId = ((RegistryContributor) (ce.getContributor()))
					.getId();
			if (bundle.getBundleId() != Long.parseLong(bundleId))
				continue;
			if (!ce.getName().equals("application"))
				continue;
			try {
				return (IWebApplication) ce.createExecutableExtension("class");
			} catch (Exception e) {
				fwkActivator.log(e);
			}
		}

		return null;
	}
}
