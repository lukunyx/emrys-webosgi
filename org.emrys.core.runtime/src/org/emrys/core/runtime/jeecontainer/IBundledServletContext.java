package org.emrys.core.runtime.jeecontainer;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.eclipse.core.runtime.IPath;
import org.emrys.core.runtime.WebComActivator;
import org.emrys.core.runtime.jeeres.FilterDelegate;
import org.emrys.core.runtime.jeeres.ListenerInfo;
import org.emrys.core.runtime.jeeres.ServletDelegate;
import org.emrys.core.runtime.jeeres.TaglibInfo;
import org.osgi.framework.Bundle;


/**
 * 
 * @author Leo Chang
 */
public interface IBundledServletContext extends ServletContext {
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

	WebComActivator getBundleActivator();

	/**
	 * @param resolvedWebContentRoot
	 */
	void setWebRootFolder(IPath webContentRootPath);

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
}
