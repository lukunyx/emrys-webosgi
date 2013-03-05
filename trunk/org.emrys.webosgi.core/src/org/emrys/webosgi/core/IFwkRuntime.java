/**
 * 
 */
package org.emrys.webosgi.core;

import java.util.Map;

import org.emrys.webosgi.core.extender.WABDeployer;
import org.emrys.webosgi.core.service.IOSGiWebContainer;
import org.emrys.webosgi.core.service.IWebApplication;
import org.osgi.framework.Bundle;


/**
 * OSGi JavaEE Web Container framework interface.
 * 
 * @author Leo Chang
 * 
 */
public interface IFwkRuntime extends IFwkConstants {

	/**
	 * Init framework launcher with some global init attribute.
	 * 
	 * @param fwkAttr
	 */
	void init(Map<String, Object> fwkAttr);

	/**
	 * Start the framework.
	 */
	void start();

	/**
	 * Stop the framework.
	 */
	void stop();

	/**
	 * Find the Web Application for the given wab bundle.
	 * 
	 * @param wabundle
	 * @return
	 */
	IWebApplication getAppliction(Bundle wabundle);

	/**
	 * Get framework's global attribute.
	 * 
	 * @param name
	 * @return
	 */
	Object getFrameworkAttribute(String name);

	/**
	 * Put a new framework attribute. If the given value is null, remove this
	 * attribute if any.
	 * 
	 * @param name
	 * @param value
	 */
	void setFrameworkAttribute(String name, Object value);

	/**
	 * Get the global WebContainer instance.
	 * 
	 * @return
	 */
	IOSGiWebContainer getWebContainer();

	/**
	 * Get the host web application's symblic name.
	 * 
	 * @return
	 */
	String getHostWebBundleSymbleName();

	/**
	 * Get the host web bundle's activator.
	 * 
	 * @return
	 */
	@Deprecated
	WebComActivator getHostBundleActivator();

	/**
	 * Register a new Wab deployer.
	 * 
	 * @param deployer
	 */
	void registerWABDeployer(WABDeployer deployer);

	/**
	 * Unregister a Wab deployer.
	 * 
	 * @param deployer
	 */
	void unregisterWABDeployer(WABDeployer deployer);

	/**
	 * Get all registered Wab deployers.
	 * 
	 * @return
	 */
	WABDeployer[] getWABDeployers();

	/**
	 * Check if this framework is running embedded in a OSGi runtime, not
	 * bridged in a JavaEE server.
	 * 
	 * @return
	 */
	boolean isOSGiEmbedded();

	/**
	 * Check if the Web Application's dynamic services be started. If not, start
	 * them and wait if it's starting in synchronized mode.
	 * 
	 * @param webApp
	 * @return
	 */
	boolean makeSureWabActive(IWebApplication webApp);

	/**
	 * Register a Web Application. If the application is set to actived before,
	 * this method will dispatch JavaEE event: Servlet Context created.
	 * 
	 * @param app
	 * @throws ServiceInitException
	 */
	void regApplication(IWebApplication app) throws ServiceInitException;

	/**
	 * Unregistered the web applicaton. And its servlet context will be
	 * deactived by framework.
	 * 
	 * @param app
	 */
	void unregApplication(IWebApplication app);
}
