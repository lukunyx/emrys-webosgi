/**
 * 
 */
package org.emrys.webosgi.core.extender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.emrys.webosgi.core.FwkActivator;
import org.emrys.webosgi.core.internal.FwkRuntime;
import org.emrys.webosgi.core.service.IWebApplication;
import org.ops4j.pax.swissbox.extender.BundleScanner;
import org.ops4j.pax.swissbox.extender.BundleWatcher;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;


/**
 * @author LeoChang
 * 
 */
public class Watcher4ExtPointWAB extends BundleWatcher<IWebApplication> {

	private SynchronousBundleListener bundleInstallListener;

	@SuppressWarnings("unchecked")
	public Watcher4ExtPointWAB(BundleContext context, FwkRuntime fwkRuntime) {
		super(context, new WebSvcExtScanner(),
				new WABObeserver<IWebApplication>());
	}

	@Override
	protected void onStart() {
		m_mappings = new HashMap<Bundle, List<IWebApplication>>();
		// listen to bundles events
		m_context
				.addBundleListener(bundleInstallListener = new SynchronousBundleListener() {
					public void bundleChanged(final BundleEvent bundleEvent) {
						switch (bundleEvent.getType()) {
						case BundleEvent.INSTALLED:
						case BundleEvent.RESOLVED:
						case BundleEvent.STARTED:
							register(bundleEvent.getBundle());
							break;
						case BundleEvent.STOPPED:
						case BundleEvent.UNRESOLVED:
						case BundleEvent.UNINSTALLED:
							unregister(bundleEvent.getBundle());
							break;
						}
					}
				});

		// scan already installed bundles
		Bundle[] bundles = m_context.getBundles();
		if (bundles != null) {
			for (Bundle bundle : bundles) {
				if (bundle.getState() == Bundle.RESOLVED
						|| bundle.getState() == Bundle.ACTIVE) {
					register(bundle);
				}
			}
		}
	}

	/**
	 * Un-register the bundle listener, releases resources
	 */
	@Override
	protected void onStop() {
		m_context.removeBundleListener(bundleInstallListener);
		bundleInstallListener = null;
		super.onStop();
	}

	private static class WebSvcExtScanner implements BundleScanner {

		public List scan(Bundle bundle) {
			IWebApplication app = findWebSvcContribution(bundle);
			if (app != null) {
				List<IWebApplication> result = new ArrayList<IWebApplication>();
				result.add(app);
				return result;
			}
			return null;
		}

		private IWebApplication findWebSvcContribution(Bundle bundle) {
			FwkActivator fwcActivator = FwkActivator.getInstance();
			IExtensionPoint extPoint = Platform.getExtensionRegistry()
					.getExtensionPoint(
							fwcActivator.getBundleSymbleName()
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
					return (IWebApplication) ce
							.createExecutableExtension("class");
				} catch (Exception e) {
					fwcActivator.log(e);
				}
			}
			return null;
		}

	}
}
