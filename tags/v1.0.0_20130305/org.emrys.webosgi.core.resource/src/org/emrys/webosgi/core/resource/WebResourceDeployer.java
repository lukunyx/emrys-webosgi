package org.emrys.webosgi.core.resource;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.emrys.webosgi.core.extender.DefaultWabDeployer;
import org.emrys.webosgi.core.internal.FwkRuntime;
import org.emrys.webosgi.core.resource.extension.DefinesRoot;
import org.emrys.webosgi.core.runtime.WebApplication;
import org.emrys.webosgi.core.service.IWebApplication;
import org.emrys.webosgi.core.util.WebBundleUtil;
import org.osgi.framework.Bundle;


/**
 * @author LeoChang
 * 
 */
public class WebResourceDeployer extends DefaultWabDeployer {
	final Map<Bundle, DefinesRoot> buffers = new HashMap<Bundle, DefinesRoot>();

	@Override
	public synchronized void deploy(Bundle wabundle) throws Exception {
		if (wabundle.getState() == Bundle.RESOLVED
				&& WebBundleUtil.isWebAppBundle(wabundle)) {

			// Search application extension point.
			IWebApplication app = findApplication(wabundle);
			if (app != null && app.isDynaServicesStarted())
				return;

			if (app == null) {
				WebApplication tmpApp = new WebApplication(wabundle);
				FwkRuntime.getInstance().regApplication(tmpApp);
				app = tmpApp;
			}

			// Do default resources publish if any.
			app.pubStaticResources();

			// Do our internal static resource publish.
			File webContentRoot = app.findWebContentRoot(false);
			if (webContentRoot != null && webContentRoot.isDirectory()
					&& webContentRoot.exists()) {
				IPath webContentPath = new Path(webContentRoot
						.getAbsolutePath());

				if (webContentPath != null) {
					DefinesRoot publishedResRoot = WebResCore.getInstance()
							.registerWebContextRoot(
									wabundle,
									WebBundleUtil
											.getWabContextPathHeader(wabundle),
									webContentPath.toPortableString());
					buffers.put(wabundle, publishedResRoot);
				}
			}
		}
	}

	@Override
	public void undeploy(Bundle wabundle) {
		if (wabundle.getState() == Bundle.UNINSTALLED) {
			WebResCore.getInstance().unregisterWebContextRoot(
					buffers.get(wabundle));
		}
	}
}
