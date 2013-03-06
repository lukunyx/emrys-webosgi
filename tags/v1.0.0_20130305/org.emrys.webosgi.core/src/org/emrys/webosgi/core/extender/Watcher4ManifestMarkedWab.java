/**
 * 
 */
package org.emrys.webosgi.core.extender;

import java.util.HashMap;
import java.util.List;

import org.emrys.webosgi.core.internal.FwkRuntime;
import org.ops4j.pax.swissbox.extender.BundleManifestScanner;
import org.ops4j.pax.swissbox.extender.BundleWatcher;
import org.ops4j.pax.swissbox.extender.ManifestEntry;
import org.ops4j.pax.swissbox.extender.RegexKeyManifestFilter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author LeoChang
 * 
 */
public class Watcher4ManifestMarkedWab extends BundleWatcher<ManifestEntry> {

	private static final Logger LOG = LoggerFactory
			.getLogger(Watcher4ManifestMarkedWab.class);
	private SynchronousBundleListener bundleInstallListener;

	@SuppressWarnings("unchecked")
	public Watcher4ManifestMarkedWab(BundleContext context,
			FwkRuntime fwkRuntime) {
		super(context, new BundleManifestScanner(new RegexKeyManifestFilter(
				"Web-ContextPath")), new WABObeserver<ManifestEntry>());
	}

	@Override
	protected void onStart() {
		m_mappings = new HashMap<Bundle, List<ManifestEntry>>();
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
}
