/*******************************************************************************
 * Copyright (c) 2010 Hirisun Corporation. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Hirisun License v1.0 which accompanies this
 * distribution, and is available at http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime;

import java.io.File;

import org.emrys.common.IComActivator;
import org.emrys.core.runtime.jeecontainer.IBundledServletContext;


/**
 * Interface to Web Bundle's Activator
 * 
 * @author Leo Chang - Hirisun
 * @version 2010-11-5
 */
public interface IWebComActivator extends IComActivator {
	/**
	 * Get the services' name space id of this web bundle. By default, bundle's
	 * id is used.
	 * 
	 * @return
	 */
	String getServiceNS();

	/**
	 * The prefix of the services' name space.
	 * 
	 * @return
	 */
	String getServiceNSPrefix();

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

	/**
	 * @return if the universal web service be started successfully.
	 */
	boolean isWebServiceStarted();

	/**
	 * <p>
	 * This method allow sub class of this Activator to specify its WebContent Path, instead of
	 * provided by default extension point of WebOSGi framework.
	 * <p>
	 * <p>
	 * 鍏佽瀛愮被鎻愪緵棣栭�夌殑WebContent鐩綍锛屼笉鎻愪緵鍒欎粠鎵╁睍鐐逛腑璇诲彇鍊欓�夎矾寰勩��
	 * <p>
	 * 
	 * @return
	 */
	String getWebContentPath();

	/**
	 * Get resolved Web Content Root directory in local file system. Maybe null
	 * if no WebContnet resource be specified in this bundle.
	 * 
	 * @param forceUpdate
	 * @return Web Content Root direcotry in local file system. May be null, if
	 *         this bundle has no WebContent specified.
	 */
	File getResolvedWebContentRoot(boolean forceUpdate);

	/**
	 * Get web resource by path relative to the web content root.
	 * 
	 * @param path
	 * @return
	 */
	File getWebResource(String path);

	/**
	 * Get this web bundles {@link javax.servlet.ServletContext}
	 * 
	 * @return
	 */
	IBundledServletContext getBundleServletContext();
}
