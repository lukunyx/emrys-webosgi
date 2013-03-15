package org.emrys.support.docpub;

import java.util.Hashtable;

import org.eclipse.help.internal.HelpPlugin;
import org.osgi.framework.BundleContext;

import org.emrys.support.site.FeatureSiteReciever;
import org.emrys.support.site.IUpdateSiteEventListener;
import org.emrys.webosgi.core.ServiceInitException;
import org.emrys.webosgi.core.WebComActivator;
import org.emrys.webosgi.core.jeeres.ServletDelegate;
import org.emrys.webosgi.help.HelpSupportActivator;


public class Activator extends WebComActivator implements
		IUpdateSiteEventListener {
	private static Activator instance;

	public static Activator getInstance() {
		return instance;
	}

	@Override
	protected void startComponent(BundleContext context) {
		instance = this;
		super.startComponent(context);
	}

	@Override
	public String getServiceNSPrefix() {
		return "docsupport";
	}

	@Override
	protected void startWebService(BundleContext context)
			throws ServiceInitException {
		ServletDelegate delegate = new ServletDelegate();
		delegate.servlet = new DocResServlet();
		delegate.setRawURLPatterns("/" + this.getServiceNSPrefix() + "/*");
		delegate.parameters = new Hashtable<String, String>();
		delegate.setBundleContext(getBundleServletContext());
		this.getBundleServletContext().getServletsInfo().add(delegate);

		// Register the Update Site Listener to synchronize the Feature's
		// Documents when Update site
		// updated.
		// FeatureSiteReciever feaSiteReceiever =
		// FeatureSiteReciever.getInstance();
		// if (feaSiteReceiever != null)
		FeatureSiteReciever.addUpdateSiteListener(this);

		// HelpBase's remote help content seems not work sometime.We register a
		// toc resource provider to load resource form feature repository.
		HelpSupportActivator.getInstance().regTocResProvider(
				new TocResProvider());
		super.startWebService(context);
	}

	public void siteUpdated(String siteName, String version) {
		// Ignore the update site name and version, update Feature Documents.
		try {
			// Clear the Toc contribution caches and refresh.
			HelpPlugin.getTocManager().clearCache();
		} catch (Exception e) {
			e.printStackTrace();
			log(e);
		}
	}
}