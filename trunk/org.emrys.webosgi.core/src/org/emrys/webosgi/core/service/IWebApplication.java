package org.emrys.webosgi.core.service;

import java.io.File;

import org.emrys.webosgi.core.ServiceInitException;
import org.osgi.framework.Bundle;

/**
 * Web application descriptor for its information and customized behaviors.
 * 
 * @author Leo Chang
 * 
 */
public interface IWebApplication {
	void init() throws ServiceInitException;

	IOSGiWebContainer getWebContainer();

	/**
	 * The bundle of this web application bundle.
	 * 
	 * @return
	 */
	Bundle getWebBundle();

	/**
	 * If web context path specified with "/", this wab is a host web
	 * application then. This context path not prefix with the framework context
	 * path.
	 * 
	 * @return
	 */
	String getWebContextPath();

	/**
	 * Find the web resource content root directory of this wab. If not any,
	 * return null.
	 * 
	 * @param forceUpdate
	 * @return
	 */
	File findWebContentRoot(boolean forceUpdate);

	/**
	 * Get the singleton servlet context of this wab. The wab can given its
	 * customized context implementation.
	 * 
	 * @return
	 */
	IWABServletContext getBundleServletContext();

	/**
	 * Any static resources pub can be here.
	 * 
	 * @throws ServiceInitException
	 */
	void pubStaticResources() throws ServiceInitException;

	/**
	 * If the static resource published.
	 * 
	 * @return
	 */
	boolean isStaticResPublished();

	/**
	 * Do extra dynamic service start according JavaEE: Register any servlets,
	 * filters, listeners, etc to by API in Wab Servlet Context of this wab.
	 * 
	 * @throws ServiceInitException
	 */
	void startDynamicServices() throws ServiceInitException;

	/**
	 * Stop dynamic JavaEE servlets, filters or listeners.
	 */
	void stopDynamicServices();

	/**
	 * If the dynamic JavaEE service started.
	 * 
	 * @return
	 */
	boolean isDynaServicesStarted();
}
