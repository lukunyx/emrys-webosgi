/*******************************************************************************
 * Copyright (c) 2011 Hirisun Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Hirisun License v1.0
 * which accompanies this distribution, and is available at
 * http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.jeecontainer;

import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;

import org.emrys.core.runtime.handlers.IFwkRequestHandler;
import org.osgi.framework.Bundle;


/**
 * OSGi Web Container of Framework.
 * 
 * @author Leo Chang - Hirisun
 * @version 2011-3-22
 */
public interface IOSGiWebContainer {
	/**
	 * Thread local variant name: current servlet request
	 */
	final static String THREAD_V_REQUEST = "current_req";
	/**
	 * Thread local variant name: current servlet response
	 */
	final static String THREAD_V_RESPONSE = "current_resp";

	/**
	 * Get the current request thread variants.
	 * 
	 * @return
	 */
	Map<String, Object> getReqThreadVariants();

	/**
	 * Find the ServletContext for a given bundle.
	 * 
	 * @param bundle
	 * @return
	 */
	ServletContext findServletContext(Bundle bundle);

	/**
	 * find the host web bundle's ServletContext
	 * 
	 * @return
	 */
	IBundledServletContext findHostServletContext();

	/**
	 * Get all bundled ServletContext.
	 * 
	 * @return
	 */
	Set<IBundledServletContext> getAllBundledServletContext();

	/**
	 * 
	 * @return
	 */
	public ServletContext getServletContext();

	/**
	 * Refresh buffered data, servlet filters, serlvets, listeners. etc.
	 * 
	 * @throws Exception
	 */
	void refresh() throws Exception;

	/**
	 * Add a {@link IBundledServletContext}
	 * 
	 * @param ctx
	 * @throws Exception
	 */
	void addBundledServletContext(IBundledServletContext ctx) throws Exception;

	/**
	 * Remove a {@link IBundledServletContext}
	 * 
	 * @param ctx
	 */
	void removeBundledServletContext(IBundledServletContext ctx);

	/**
	 * @param scab
	 * @param et
	 */
	void trigerContextAttrEvent(ServletContextAttributeEvent scab,
			IBundledServletContext ctx, int et);

	/**
	 * The convenient method to find the bundled servlet context for a given
	 * context prefix string.
	 * 
	 * @param ctxPrefix
	 * @return the result context if found, otherwise null returned.
	 */
	IBundledServletContext getBundledServletContext(String bundlePrefix);

	void addFwkRequestHandler(IFwkRequestHandler handler);

	OSGiJEEContainerHelper getHelper();
}
