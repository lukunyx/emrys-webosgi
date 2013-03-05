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
import org.emrys.webosgi.common.ComActivator;
import org.emrys.webosgi.core.FwkActivator;
import org.emrys.webosgi.core.WebComActivator;
import org.emrys.webosgi.core.internal.FwkRuntime;
import org.emrys.webosgi.core.service.IOSGiWebContainer;
import org.ops4j.pax.swissbox.extender.BundleObserver;
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
public class Watcher4ActivatorWAB extends BundleWatcher<Bundle> {

	private SynchronousBundleListener bundleInstallListener;

	@SuppressWarnings("unchecked")
	public Watcher4ActivatorWAB(BundleContext context, FwkRuntime fwkRuntime) {
		super(context, new WebSvcExtScanner(), new ActivatorWabObserver(
				fwkRuntime));
	}

	@Override
	protected void onStart() {
		m_mappings = new HashMap<Bundle, List<Bundle>>();
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

	private static class WebSvcExtScanner implements BundleScanner<Bundle> {

		public List scan(Bundle bundle) {
			List<Bundle> result = new ArrayList<Bundle>();
			if (isJeeSvcContribution(bundle))
				result.add(bundle);
			return result;
		}

		private boolean isJeeSvcContribution(Bundle bundle) {
			FwkActivator fwcActivator = FwkActivator.getInstance();
			IExtensionPoint extPoint = Platform.getExtensionRegistry()
					.getExtensionPoint(
							fwcActivator.getBundleSymbleName()
									+ ".jeeSvcContribution");
			IConfigurationElement[] ces = extPoint.getConfigurationElements();
			for (IConfigurationElement ce : ces) {
				String bundleId = ((RegistryContributor) (ce.getContributor()))
						.getId();
				if (bundle.getBundleId() != Long.parseLong(bundleId))
					continue;
				return true;
			}
			return false;
		}
	}

	private static class ActivatorWabObserver implements BundleObserver<Bundle> {

		private final FwkRuntime fwkRuntime;

		ActivatorWabObserver(FwkRuntime fwkRuntime) {
			this.fwkRuntime = fwkRuntime;
		}

		public void addingEntries(Bundle bundle, List<Bundle> entries) {
			if (!fwkRuntime.isFwkInited())
				return;
			if (bundle.getState() == Bundle.ACTIVE) {
				ComActivator activator = FwkRuntime.getInstance()
						.getBundleActivator(bundle.getBundleId());
				if (activator instanceof WebComActivator) {
					WebComActivator webActivator = (WebComActivator) activator;
					if (!webActivator.isWebServiceStarted()) {
						try {
							webActivator.startApplication();
							fwkRuntime.getWebContainer().refresh();
						} catch (Exception e) {
							// e.printStackTrace();
							FwkActivator.getInstance().log(e);
						}
					}
				}
			}
		}

		public void removingEntries(Bundle bundle, List<Bundle> entries) {
			if (bundle.getState() == Bundle.INSTALLED) {
				ComActivator activator = FwkRuntime.getInstance()
						.getBundleActivator(bundle.getBundleId());
				if (activator instanceof WebComActivator) {
					IOSGiWebContainer webContainer = fwkRuntime
							.getWebContainer();
					WebComActivator webActivator = (WebComActivator) activator;
					webContainer.unregServletContext(webActivator
							.getBundleServletContext());
					try {
						webContainer.refresh();
						FwkActivator.getInstance().log(
								"Removed web bundle service: "
										+ webActivator.getBundleSymbleName(),
								0, false);
					} catch (Exception e) {
						// e.printStackTrace();
						FwkActivator.getInstance().log(e);
					}
				}
			}
		}
	}
}
