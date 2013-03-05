package org.emrys.webosgi.core.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.emrys.webosgi.core.jeeres.FilterDelegate;
import org.emrys.webosgi.core.jeeres.ListenerInfo;
import org.emrys.webosgi.core.jeeres.ServletDelegate;
import org.emrys.webosgi.core.jeeres.TaglibInfo;
import org.osgi.framework.Bundle;


/**
 * Servlet context Of web application. This interface derived from standard
 * JavaEE ServletContext, and make the wab customize it.
 * 
 * @author Leo Chang
 */
public interface IWABServletContext extends ServletContext {
	/**
	 * ServletContext Attribute modification Event type: add
	 */
	public static final int ET_ATTR_ADDED = 0;
	/**
	 * ServletContext Attribute modification Event type: removed
	 */
	public static final int ET_ATTR_REMOVED = 1;
	/**
	 * ServletContext Attribute modification Event type: replaced
	 */
	public static final int ET_ATTR_REPLACED = 2;

	/**
	 * This Servlet Context's state mark: normal
	 */
	public static final int STATE_NORMAL = 0;
	/**
	 * This Servlet Context's state mark: use global context
	 */
	public static final int STATE_ONLY_USE_GLOBAL_CTX = 1;
	/**
	 * This Servlet Context's state mark: include global context
	 */
	public static final int STATE_INCLUE_GLOBAL_CTX = 2;

	void setInitParameter(String name, String value);

	/**
	 * Get global Servlet Context shared by all Web Server's direct servlet.
	 * 
	 * @return
	 */
	ServletContext getGlobalContext();

	/**
	 * @return
	 */
	Collection<ServletDelegate> getServletsInfo();

	/**
	 * @return
	 */
	Collection<FilterDelegate> getFilters();

	/**
	 * @return
	 */
	Collection<ListenerInfo> getListeners();

	/**
	 * Get this Servlet Context's Bundle.
	 * 
	 * @return
	 */
	Bundle getBundle();

	/**
	 * Switch the context state.
	 * 
	 * @param state
	 */
	void switchState(int state);

	/**
	 * @return taglibs
	 */
	List<TaglibInfo> getTaglibs();

	/**
	 * @return welcome pages
	 */
	List<String> getWelcomePages();

	/**
	 * @return error pages
	 */
	Map<Integer, String> getErrorPages();

	/**
	 * Http Session timeout interval.
	 * 
	 * @return session timeout in seconds.
	 */
	int getSessionTimeout();

	/**
	 * @param interval
	 *            in seconds.
	 */
	void setSessionTimeout(int interval);

	/**
	 * If this wab is a host bundle with can be visit without framework's
	 * context path prefix.
	 * 
	 * @return
	 */
	boolean isHostBundle();

	/**
	 * The context path of web without the framework's context path.
	 * 
	 * @return
	 */
	String getWABContextPath();

	/**
	 * Get the web application's context class loader.
	 * 
	 * @return
	 */
	ClassLoader getWabClassLoader();

	/**
	 * Judge this resource is a jsp file. If true, the framework will try to
	 * handle it as jsp.
	 * 
	 * @param path
	 * @return
	 */
	boolean isJspResource(String path);

	/**
	 * Judge this resourc is a static resource that framework can deliver it to
	 * client.
	 * 
	 * @param path
	 * @return
	 */
	boolean isStaticResource(String path);

	/**
	 * Whether this servlet context is actived.
	 * 
	 * @return
	 */
	boolean isActive();

	/**
	 * The framework actived this servelt context, some extra job can be done in
	 * this method.
	 */
	void actived();

	/**
	 * Set this servlet context actived. If this status is set true before this
	 * servlet context register to web container, the web container will do
	 * Servlet Context Create event dispatching once registered. Otherwise, if
	 * the status set to false, this servlet context will not be considered in
	 * request hanlding, eccept the static resource GET Request.
	 * 
	 * @param b
	 */
	void setActive(boolean b);

	/**
	 * Get the web application of this wab servlet context.
	 * 
	 * @return
	 */
	IWebApplication getWebApplication();
}
