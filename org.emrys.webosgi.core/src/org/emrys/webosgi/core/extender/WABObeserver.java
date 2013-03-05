/**
 * 
 */
package org.emrys.webosgi.core.extender;

import java.util.List;

import org.emrys.webosgi.core.FwkActivator;
import org.emrys.webosgi.core.internal.FwkRuntime;
import org.ops4j.pax.swissbox.extender.BundleObserver;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author LeoChang
 * 
 */
public class WABObeserver<T> implements BundleObserver<T> {
	private static final Logger LOG = LoggerFactory
			.getLogger(WABObeserver.class);

	static {
		// Register the dynaimcal resource deployer to framework.
		FwkRuntime.getInstance().registerWABDeployer(new DefaultWabDeployer());
	}

	public void addingEntries(final Bundle bundle, final List<T> entries) {
		WABDeployer[] deployers = FwkRuntime.getInstance().getWABDeployers();
		for (WABDeployer deployer : deployers) {
			try {
				deployer.deploy(bundle);
			} catch (Exception e) {
				FwkActivator.getInstance().log(e);
			}
		}
	}

	// log found entries when bundle containing expected headers stops
	public void removingEntries(final Bundle bundle, final List<T> entries) {
		WABDeployer[] deployers = FwkRuntime.getInstance().getWABDeployers();
		for (WABDeployer deployer : deployers) {
			try {
				deployer.undeploy(bundle);
			} catch (Exception e) {
				FwkActivator.getInstance().log(e);
			}
		}
	}
}
