package org.emrys.webosgi.core.resource;

import java.io.File;

import org.emrys.webosgi.core.ServiceInitException;
import org.emrys.webosgi.core.WebComActivator;
import org.emrys.webosgi.core.resource.extension.DefinesRoot;
import org.osgi.framework.BundleContext;

/**
 * This Activator inherit from WebComponentActivator and publish all resources
 * on Web Content path (see
 * {@link WebComActivator#getResolvedWebContentRoot(boolean)}).
 * 
 * @author Leo Chang
 */
public class WebResComActivator extends WebComActivator {
	/**
	 * The resource repository root of this web component..
	 */
	private DefinesRoot publishedResRoot = null;

	@Override
	public void stop(BundleContext context) throws Exception {
		if (publishedResRoot != null)
			WebResCore.getInstance().unregisterWebContextRoot(publishedResRoot);
		super.stop(context);
	}

	@Override
	public void initWebConfig() throws ServiceInitException {
		super.initWebConfig();
		// Publish this Web Bundle's Resource
		File webContent = this.findWebContentRoot(false);
		if (webContent != null) {
			publishedResRoot = WebResCore.getInstance().registerWebContextRoot(
					this.getBundle(), this.getWebContextPath(),
					webContent.getAbsolutePath());

			// the default resource visit controller will forbid the folder
			// browsing and file
			// modifying. If any component has to change this feature, do like
			// following.
			/*
			 * publishedResRoot.setVisitControler(new IResourceVisitController()
			 * { public boolean canRead(HttpServletRequest requestUrl) { return
			 * true; }
			 * 
			 * public boolean canModify(HttpServletRequest requestUrl) { return
			 * false; }
			 * 
			 * public boolean canBrowseFolder(HttpServletRequest req) { return
			 * false; } });
			 */
		}
	}
}
