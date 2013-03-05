/**
 * 
 */
package org.emrys.webosgi.core.extender;

import org.osgi.framework.Bundle;

/**
 * @author LeoChang
 * 
 */
public interface WABDeployer {

	void deploy(Bundle bundle) throws Exception;

	void undeploy(Bundle bundle) throws Exception;
}
