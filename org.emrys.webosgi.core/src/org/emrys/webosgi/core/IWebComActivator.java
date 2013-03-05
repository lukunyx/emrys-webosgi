package org.emrys.webosgi.core;

import java.io.File;

import org.emrys.webosgi.common.IComActivator;
import org.emrys.webosgi.core.service.IWABServletContext;

/**
 * Interface to Web Bundle's Activator
 * 
 * @author Leo Chang
 * @version 2010-11-5
 */
public interface IWebComActivator extends IComActivator {

	/**
	 * The prefix of the services' name space.
	 * 
	 * @return
	 */
	String getServiceNSPrefix();

	void setServiceNSPrefix(String nsPrefix);

	/**
	 * the default host web bundle shoule has the same install path with the
	 * framework. But the donwflow bundle can specify this feature bu override
	 * this method. If multiple host bundles specified, the first bundle will be
	 * applied. A host bundle perhaps containing and starting the WebOSGi
	 * runtime, or just has the default emplty context path attached.
	 * 
	 * @return if this bundle is host web bundle
	 */
	boolean isHostWebBundle();

	public void startApplication();

	/**
	 * @return if the universal web service be started successfully.
	 */
	boolean isWebServiceStarted();

	/**
	 * Get resolved Web Content Root directory in local file system. Maybe null
	 * if no WebContnet resource be specified in this bundle.
	 * 
	 * @param forceUpdate
	 * @return Web Content Root direcotry in local file system. May be null, if
	 *         this bundle has no WebContent specified.
	 */
	File findWebContentRoot(boolean forceUpdate);

	/**
	 * Get this web bundles {@link javax.servlet.ServletContext}
	 * 
	 * @return
	 */
	IWABServletContext getBundleServletContext();
}
