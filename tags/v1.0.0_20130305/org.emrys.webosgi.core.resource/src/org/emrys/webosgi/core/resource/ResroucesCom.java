package org.emrys.webosgi.core.resource;

import org.emrys.webosgi.core.ServiceInitException;
import org.emrys.webosgi.core.WebComActivator;
import org.emrys.webosgi.core.internal.FwkRuntime;
import org.emrys.webosgi.core.resource.servlet.ResGetSvcHandler;
import org.osgi.framework.BundleContext;


/**
 * Initialize all web bundle, set their webContent as published resource.
 * 
 * @author Leo Chang
 * @version 2010-11-4
 */
public final class ResroucesCom extends WebComActivator {
	private static ResroucesCom instance;

	public static ResroucesCom getInstance() {
		return instance;
	}

	private WebResCore fsvcCore;
	private WebResourceDeployer webResDeployer;

	@Override
	protected void startComponent(BundleContext context) {
		instance = this;
		super.startComponent(context);
	}

	@Override
	public void startWebService(BundleContext context)
			throws ServiceInitException {
		instance = this;
		super.startWebService(context);
		fsvcCore = WebResCore.getInstance();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		FwkRuntime.getInstance().unregisterWABDeployer(webResDeployer);
		super.stop(context);
	}

	@Override
	public void initWebConfig() throws ServiceInitException {
		super.initWebConfig();
		// Add the web resource request handler to Framework Container.
		webContainer.addFwkRequestHandler(new ResGetSvcHandler(
				webContainer));
	}

	/**
	 * @return the {@link WebResCore} instance.
	 */
	public WebResCore getResPluginCore() {
		return fsvcCore;
	}

	@Override
	public String getServiceNSPrefix() {
		return "fsvc";
	}
}
