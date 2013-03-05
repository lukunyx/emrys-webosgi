package org.emrys.webosgi.core.service;

import java.util.Map;
import java.util.Set;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;

import org.emrys.webosgi.core.IFwkConstants;
import org.emrys.webosgi.core.handlers.IFwkRequestHandler;
import org.emrys.webosgi.core.runtime.OSGiWebContainerHelper;
import org.osgi.framework.Bundle;

/**
 * OSGi Web Container of Framework.
 * 
 * @author Leo Chang
 * @version 2011-3-22
 */
public interface IOSGiWebContainer extends Servlet, ServletConfig,
		IFwkConstants {
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
	IWABServletContext findHostServletContext();

	/**
	 * Get all bundled ServletContext.
	 * 
	 * @return
	 */
	Set<IWABServletContext> getAllBundledServletContext();

	/**
	 * Refresh buffered data, servlet filters, serlvets, listeners. etc.
	 * 
	 * @throws Exception
	 */
	void refresh() throws Exception;

	/**
	 * Active the given servlet context of web bundle and then set the active
	 * status to true. If the servlet context is set actived, this method do
	 * nothing.
	 * 
	 * @param ctx
	 * @throws Exception
	 */
	void activeServletContext(IWABServletContext ctx) throws Exception;

	/**
	 * Register a {@link IWABServletContext} of web bundle, but now the servlet
	 * context has not been active yet. If it has been registered, do nothing.
	 * If this servelt context is set active before register, do context created
	 * event dispatch then.
	 * 
	 * @param ctx
	 * @throws Exception
	 * @return
	 */
	void regServletContext(IWABServletContext ctx) throws Exception;

	/**
	 * Remove a {@link IWABServletContext}
	 * 
	 * @param ctx
	 */
	void unregServletContext(IWABServletContext ctx);

	/**
	 * @param scab
	 * @param et
	 */
	void trigerContextAttrEvent(ServletContextAttributeEvent scab,
			IWABServletContext ctx, int et);

	/**
	 * The convenient method to find the bundled servlet context for a given
	 * context prefix string. Deprecated, use getWABServletContext(String
	 * wabCtxPath) for instead.
	 * 
	 * @param ctxPrefix
	 * @return the result context if found, otherwise null returned.
	 */
	@Deprecated
	IWABServletContext getBundledServletContext(String bundlePrefix);

	IWABServletContext getWABServletContext(Bundle bundle);

	IWABServletContext getWABServletContext(String wabCtxPath);

	void addFwkRequestHandler(IFwkRequestHandler handler);

	void unregisterFwkRequestHandler(IFwkRequestHandler handler);

	OSGiWebContainerHelper getHelper();
}
