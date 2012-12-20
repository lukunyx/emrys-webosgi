package org.emrys.core.runtime.resources;

import org.emrys.core.runtime.ServiceInitException;
import org.emrys.core.runtime.WebComActivator;
import org.emrys.core.runtime.resources.servlet.ResGetSvcHandler;
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
	public void initWebConfig() throws ServiceInitException {
		super.initWebConfig();
		// Add the web resource request handler to Framework Container.
		jeeContainerSVC.addFwkRequestHandler(new ResGetSvcHandler(
				jeeContainerSVC));
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
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
