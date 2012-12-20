/*******************************************************************************
 * Copyright (c) 2011 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.jeecontainer;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponseWrapper;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.emrys.core.runtime.handlers.CustomizeFiltersHandler;
import org.emrys.core.runtime.handlers.CustomizeServletHandler;
import org.emrys.core.runtime.handlers.IFwkHandlerChain;
import org.emrys.core.runtime.handlers.IFwkRequestHandler;
import org.emrys.core.runtime.handlers.RequestHandlerChain;
import org.emrys.core.runtime.handlers.RequestPathAdjustHandler;
import org.emrys.core.runtime.jeeres.AbstMultiInstUrlMapObject;
import org.emrys.core.runtime.jeeres.ClonedExecutableServletObject;
import org.emrys.core.runtime.jeeres.FilterDelegate;
import org.emrys.core.runtime.jeeres.ListenerInfo;
import org.emrys.core.runtime.jeeres.ServletDelegate;
import org.emrys.core.runtime.jeewrappers.BundledHttpServletRequestWrapper;
import org.emrys.core.runtime.jeewrappers.HttpSessionWrapper;
import org.osgi.framework.Bundle;


/**
 * The {@link OSGiJEEContainer}'s helper class to provide some utilitary method
 * and make the {@link OSGiJEEContainer} clear and easy to read. This class has
 * been defined as packaged scope using only. Other package shoule not use it.
 * 
 * @author Leo Chang
 */
public class OSGiJEEContainerHelper {
	private final static String REQ_V_MAPPED_SERVLET_PREFIX = "REQ_V_MAPPED_SERVLET_PREFIX";
	private final OSGiJEEContainer jeeContainer;
	/**
	 * the sorter map for SerlvetDelegate and FilterDelegate.
	 */
	private final Map<Class<?>, URLMapObjectSorter<?>> sorterMap = new HashMap<Class<?>, URLMapObjectSorter<?>>();
	// Buffered global data.
	private Collection<ServletDelegate> normalServlets;
	private Collection<FilterDelegate> allFilters;
	private RequestHandlerChain requestHandlerChain;

	public OSGiJEEContainerHelper(OSGiJEEContainer jeeContainer) {
		this.jeeContainer = jeeContainer;
		this.normalServlets = new HashSet<ServletDelegate>();
		this.allFilters = new HashSet<FilterDelegate>();
	}

	/**
	 * Refresh the buffered Data.
	 */
	public void refresh() throws Exception {
		// Find all serlvets
		normalServlets = findURLParttensObjects(null, ServletDelegate.class,
				null);
		// collect all fitlers.
		getAllBufferedFilters(true);

		// sort normal serlvets and all fitlers and buffered them.S
		sortURLPatternsExeObjs(ServletDelegate.class, true);
		sortURLPatternsExeObjs(FilterDelegate.class, true);
	}

	/**
	 * Get all buffered {@link FilterDelegate}
	 * 
	 * @param update
	 * @return
	 */
	public Collection<FilterDelegate> getAllBufferedFilters(boolean update) {
		if (allFilters == null || update) {
			allFilters = findURLParttensObjects(null, FilterDelegate.class,
					new IServletFilter() {
						public boolean intrest(AbstMultiInstUrlMapObject info) {
							return true;
						}
					});
		}
		return allFilters;
	}

	/**
	 * Switch request's current bundle to the filter's bundle. Because
	 * {@link BundledHttpServletRequestWrapper#getSession()} should return
	 * different {@link HttpSessionWrapper} according to the current bundle in
	 * {@link BundledHttpServletRequestWrapper}
	 * 
	 * @param bundle
	 *            current bundle to be set into BundledHttpServletRequestWrapper
	 * @see <li>{@link BundledHttpServletRequestWrapper#getSession()}</li> <li>
	 *      {@link #doServletMapFilter(ServletDelegate, BundledHttpServletRequestWrapper, ServletResponse, List)}
	 *      </li> <li>
	 *      {@link #doUrlMapFilter(BundledHttpServletRequestWrapper, HttpServletResponseWrapper, List, boolean)}
	 *      </li> <li>
	 *      {@link #doServletService(BundledHttpServletRequestWrapper, HttpServletResponseWrapper)}
	 *      </li>
	 */
	public void switchReqBundleContext(Bundle bundle) {
		BundledHttpServletRequestWrapper req = (BundledHttpServletRequestWrapper) jeeContainer
				.getReqThreadVariants().get(OSGiJEEContainer.THREAD_V_REQUEST);
		req.setBundle(bundle);
	}

	/**
	 * @param httpReq
	 * @param sortedSerlvetCopys
	 *            All sorted Serlvet Copys for all URL Pattern of all Serlvet.
	 * @return the serlvet copy matched the current request's path.
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public ClonedExecutableServletObject<ServletDelegate> chooseDelegateServlet(
			BundledHttpServletRequestWrapper httpReqWrapper,
			List<ClonedExecutableServletObject<ServletDelegate>> sortedSerlvetCopys)
			throws Exception {
		String originalServletPath = httpReqWrapper.getServletPath();
		jeeContainer.getReqThreadVariants().put(REQ_V_MAPPED_SERVLET_PREFIX,
				null);

		ClonedExecutableServletObject<ServletDelegate> servletDelegateCopy = calculateServlet(
				originalServletPath, sortedSerlvetCopys);
		if (servletDelegateCopy == null)
			return null;

		IBundledServletContext ctx = servletDelegateCopy.getOriginalObj()
				.getBundleContext();

		// switchReqBundleContext(ctx.getBundle());
		String servletPrefix = (String) jeeContainer.getReqThreadVariants()
				.get(REQ_V_MAPPED_SERVLET_PREFIX);

		String newServletPath = servletPrefix;
		if (servletPrefix.startsWith("/"
				+ ctx.getBundleActivator().getServiceNSPrefix()))
			newServletPath = new Path(servletPrefix).removeFirstSegments(1)
					.makeAbsolute().toPortableString();

		if (newServletPath.equals("/"))
			newServletPath = null;

		httpReqWrapper.setServletPath(newServletPath);
		String newPathInfo = originalServletPath.replace(servletPrefix, "");
		// Tomcat6's behavior is to take the empty pathInfo as null.
		if (newPathInfo.length() == 0)
			newPathInfo = null;
		httpReqWrapper.setPathInfo(newPathInfo);

		return servletDelegateCopy;
	}

	/**
	 * To match a givan servlet path with the all servlet copys list. If a copy
	 * of one Servlet has been executed, other copy will be skiped.
	 * 
	 * @param servletPath
	 *            the current http request's serlvet path to match.
	 * @param servletCopys
	 *            all servlet copys for each url pattern.
	 * @return
	 */
	private ClonedExecutableServletObject<ServletDelegate> calculateServlet(
			String servletPath,
			List<ClonedExecutableServletObject<ServletDelegate>> servletCopys) {
		for (ClonedExecutableServletObject<ServletDelegate> clonedUrlPatternInst : servletCopys) {
			// if one copy of a serlvet has been executed in this request
			// thread, skip it.
			// But this is not the fact for servlet, because serlvet will only
			// be called once in request thread, not like flter.
			/*
			 * if (clonedUrlPatternInst.isExecuted()) continue;
			 */

			String expr = clonedUrlPatternInst.getOriginalObj()
					.getURLPatterns()[clonedUrlPatternInst.getId()];
			StringBuffer sb = new StringBuffer();

			if (checkPathInfoWithExpr(servletPath, expr, sb, false)) {
				String servletPrefix = sb.toString();
				jeeContainer.getReqThreadVariants().put(
						REQ_V_MAPPED_SERVLET_PREFIX, servletPrefix);
				return clonedUrlPatternInst;
			}
		}
		return null;
	}

	/**
	 * For Servlet and Filter, they may has mutiple URL pattern mapping. To sort
	 * the execute order by their url patterns, we cloned many object of the
	 * servlet or filter according their url patterns. These cloned object will
	 * be sort into a list to be execute, but a servlet or filter can only be
	 * executed once. This method will sort all cloned object for servlet or
	 * filter into a list. The result will be buffered only update need.
	 * 
	 * @param <T>
	 *            the type to sort.
	 * @param type
	 *            the type to sort.
	 * @param forceUpdate
	 *            whether to update the buffer.
	 * @return the sorted cloned object for servlet or filter.
	 */
	public <T extends AbstMultiInstUrlMapObject> List<ClonedExecutableServletObject<T>> sortURLPatternsExeObjs(
			Class<T> type, boolean forceUpdate) {
		URLMapObjectSorter<?> sorter = sorterMap.get(type);
		if (!sorterMap.containsKey(type)) {
			sorter = new URLMapObjectSorter<T>();
			sorterMap.put(type, sorter);
		}

		Collection<? extends AbstMultiInstUrlMapObject> multiInstObjs = null;
		if (type.equals(ServletDelegate.class))
			multiInstObjs = normalServlets;
		if (type.equals(FilterDelegate.class))
			multiInstObjs = allFilters;

		List<?> result = sorter.sort(multiInstObjs, forceUpdate);

		// Leo hates the java template class and method programming!!!
		List<ClonedExecutableServletObject<T>> tmpList = new ArrayList<ClonedExecutableServletObject<T>>();
		tmpList
				.addAll((Collection<? extends ClonedExecutableServletObject<T>>) result);
		return tmpList;
	}

	interface IServletFilter {
		boolean intrest(AbstMultiInstUrlMapObject info);
	}

	/**
	 * Find all ServletDelegate or FilterDelegate from given Serlvet Context, if
	 * no context given, all contexts will be searched.
	 * 
	 * @param ctx
	 *            the servlet context to search from.
	 * @param filter
	 *            the filter to decide which serlvet is interesting.
	 * @return the desiring servlet.
	 */
	public <T extends AbstMultiInstUrlMapObject> List<T> findURLParttensObjects(
			ServletContext ctx, Class<T> type, IServletFilter filter) {
		List<T> result = new ArrayList<T>();
		if (ctx instanceof IBundledServletContext) {
			IBundledServletContext octx = (IBundledServletContext) ctx;
			Collection<? extends AbstMultiInstUrlMapObject> servlets = null;
			if (type.equals(ServletDelegate.class))
				servlets = octx.getServletsInfo();
			if (type.equals(FilterDelegate.class))
				servlets = octx.getFilters();
			for (AbstMultiInstUrlMapObject info : servlets) {
				if (filter == null || filter.intrest(info))
					result.add((T) info);
			}
		} else {
			// Collect all ServletContextListener here.
			Set<IBundledServletContext> set = jeeContainer
					.getAllBundledServletContext();
			synchronized (set) {
				for (IBundledServletContext c : set) {
					result.addAll(findURLParttensObjects(c, type, filter));
				}
			}
		}
		return result;
	}

	/**
	 * Find all servlet listener of give type from the
	 * {@link BundledServletContext}. If null servlet context given, all servlet
	 * contexts' will be find and returned.
	 * 
	 * @param ctx
	 *            the servlet context from which to search the listeners.
	 * @return the listeners of the given type
	 */
	public <T extends EventListener> List<T> findListeners(ServletContext ctx,
			Class<T> type) {
		List<T> result = new ArrayList<T>();
		if (ctx instanceof IBundledServletContext) {
			IBundledServletContext octx = (IBundledServletContext) ctx;
			Collection<ListenerInfo> listeners = octx.getListeners();
			ListenerInfo[] listenerArray = listeners
					.toArray(new ListenerInfo[listeners.size()]);
			for (ListenerInfo info : listenerArray) {
				try {
					EventListener l = info.getListener();
					if (type.isInstance(l)) {
						result.add((T) l);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else {
			// Collect all ServletContextListener here.
			Set<IBundledServletContext> set = jeeContainer
					.getAllBundledServletContext();
			for (IBundledServletContext c : set) {
				result.addAll(findListeners(c, type));
			}
		}
		return result;
	}

	/**
	 * Check if the given filter need to be invoked according the given servlet
	 * request. This method will match the current path of request to the map
	 * url pattern of the filter.
	 * 
	 * @param request
	 *            the current servlet request.
	 * @param String
	 *            the filter's URL Pattern.
	 * @return the matching result.
	 */
	public boolean checkNeedFilter(HttpServletRequest request,
			String filterURLPattern) {
		String originalServletPath = request.getServletPath();
		if (checkPathInfoWithExpr(originalServletPath, filterURLPattern, null,
				false)) {
			return true;
		}
		return false;
	}

	/**
	 * Check if the given request path matched the RegExp pattern, and the
	 * matched content will be returned in matchResult argument if it isn't
	 * null.
	 * 
	 * @param pathInfo
	 *            the path info to check with the regular express.
	 * @param exprs
	 *            the regular express string used to match the path info .
	 * @param matchResult
	 *            can be null. To get the matched path content. this content may
	 *            be regard as the servet path.
	 * @param mutipleExpr
	 *            whether the exprs string is mutiple expreses splited by
	 *            ';'(defined in {@link FilterDelegate.MULTI_MAP_SEG_SEPERATOR}
	 *            ).
	 * @return whether the matched result is true.
	 */
	public boolean checkPathInfoWithExpr(String pathInfo, String exprs,
			StringBuffer matchResult, boolean mutipleExpr) {
		String[] exprsArray = null;
		if (mutipleExpr)
			exprsArray = exprs.split(FilterDelegate.MULTI_MAP_SEG_SEPERATOR);
		else
			exprsArray = new String[] { exprs };

		for (String expr : exprsArray) {
			// Need to check out if expr has some invalid chars like # @ ^,etc?
			// No, it's not affect any thing and not our responsibility.

			// if "/*" map, return true immediately, and let the whole path info
			// as matched servlet path.
			if (expr.equals("/*")) {
				/*
				 * if (matchResult != null) matchResult.append(pathInfo);
				 */
				return true;
			}

			// Developer may append a unnecessary '/' at the end of exact path
			// map, remove it.
			if (expr.endsWith("/"))
				expr = expr.substring(0, expr.length() - 1);

			int lastSegIndex = expr.lastIndexOf('/');
			String preSegs = lastSegIndex > 0 ? expr.substring(0, lastSegIndex)
					: "";
			// If the prefix segments contains '*' or ".", then the express is
			// invalid, skip it.
			if (preSegs.indexOf('*') != -1 || preSegs.indexOf('.') != -1)
				continue;

			String lastSeg = lastSegIndex > -1 ? expr.substring(lastSegIndex)
					: "/" + expr;

			// 3 types of servlet or filter url parttern.

			// Type 1: extension map like *.jsp, *.do
			// Regard the /xxx/*.jsp or /*.jsp as extension map, not wildcard
			// path map.
			boolean isExtMap = lastSeg.startsWith("/*.")
					&& lastSeg.length() > 3;

			// Type 2: exact path map like /abc, /abc/ef, /abc/ef.g (without
			// containing '*').
			boolean isExactPathMap = lastSeg.indexOf('*') == -1;

			// Type 3: wildcard path map like /xxx/*, /*
			boolean isWildcardPathMap = lastSeg.equals("/*");

			// Remove / at last index.
			if (pathInfo.endsWith("/"))
				pathInfo = pathInfo.substring(0, pathInfo.length() - 1);

			// Build the new pattern express according to different map type.
			String newExpr = preSegs;
			if (isExtMap) {
				// Replace RegExp in last segment if any.
				lastSeg = lastSeg.replaceAll("\\.", "\\\\.");
				lastSeg = lastSeg.replaceAll("\\*", "\\\\S*");
				// match the start of given path and group an empty servlet
				// path.
				newExpr = "^(" + newExpr + lastSeg + ")" + "$";
			} else if (isExactPathMap) {
				// match the start and the end of given path
				newExpr = "^(" + newExpr + lastSeg + ")$";
			} else if (isWildcardPathMap) {
				lastSeg = lastSeg.replaceAll("\\*", "\\\\S*");
				newExpr = "^(" + newExpr + ")" + lastSeg;
			} else
				continue;

			Pattern p = Pattern.compile(newExpr);
			Matcher m = p.matcher(pathInfo);
			if (m.find()) {
				String s = m.group(1);
				// Get the grouped matched content as servlet path if the
				// invoking method need(then matchResult!=null).
				if (matchResult != null)
					matchResult.append(s);
				return true;
			}
		}

		return false;
	}

	/**
	 * Check out whether a given web resource path is a extant directory. This
	 * method mainly be used to redirect the client request to a new path
	 * appended with "/".
	 * 
	 * @param bundleServletCtx
	 *            the current servlet context root.
	 * @param path
	 *            resource path relative to the current servlet context root.
	 * @return true if this path indicating a extant resource directory.
	 */
	public boolean checkIsResourceDir(IBundledServletContext bundleServletCtx,
			IPath path) {
		// FIXME: need to buffer result to optimize performance.
		try {
			URL url = bundleServletCtx.getResource(path.toPortableString());
			// Judge the url is local directory of web resource.
			if (url != null && url.toExternalForm().endsWith("/"))
				return true;
		} catch (MalformedURLException e) {
			// e.printStackTrace();
		}
		return false;
	}

	/**
	 * Try to find a welcome page for a given directory path if configured in
	 * web.xml according to the Java EE standard.
	 * 
	 * @param bundleServletCtx
	 * @param path
	 *            the current resource direcory path.
	 * @return the welcome page name.
	 */
	public String tryRedirectToWelcomPage(
			IBundledServletContext bundleServletCtx, IPath path) {
		// Check if this request path if a directory need to try to redirect to
		// welcome page.
		if (!checkIsResourceDir(bundleServletCtx, path))
			return null;

		// FIXME: need to buffer result to optimize performance.
		List<String> welcomePages = bundleServletCtx.getWelcomePages();
		if (welcomePages == null || welcomePages.size() == 0)
			return null;
		String resPath = path.toPortableString();
		if (resPath.length() == 0)
			resPath = "/";
		Set subResPaths = bundleServletCtx.getResourcePaths(resPath);
		if (subResPaths != null) {
			for (String welcomePage : welcomePages) {
				for (Iterator it = subResPaths.iterator(); it.hasNext();) {
					String p = (String) it.next();
					// remove the prefix "/" form path name to match welcome
					// page
					// setting.
					if (welcomePage.equals(new Path(p).removeFirstSegments(
							path.segmentCount()).makeRelative()
							.toPortableString()))
						return welcomePage;
				}
			}
		}
		// If not found any existant welcome page resource, modify the current
		// servlet path as the first specified welcome page.
		return welcomePages.get(0);
	}

	public IFwkHandlerChain constructHandlerChain() {
		// FIXME: any need to refresh.
		if (requestHandlerChain == null) {
			requestHandlerChain = new RequestHandlerChain();
			requestHandlerChain.addHandler(new RequestPathAdjustHandler(
					jeeContainer));
			requestHandlerChain.addHandler(new CustomizeFiltersHandler(
					jeeContainer));
			requestHandlerChain.addHandler(new CustomizeServletHandler(
					jeeContainer));

			Set<IFwkRequestHandler> regiteredReqHandlers = jeeContainer.reqHandlers;
			if (regiteredReqHandlers != null)
				requestHandlerChain.addHandlers(regiteredReqHandlers);
		}
		return requestHandlerChain;
	}
}
