/*******************************************************************************
 * Copyright (c) 2011 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.jeecontainer;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletException;

import org.apache.AnnotationProcessor;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.emrys.common.util.FileUtil;
import org.emrys.core.runtime.FwkActivator;
import org.emrys.core.runtime.WebComActivator;
import org.emrys.core.runtime.internal.FwkRuntime;
import org.emrys.core.runtime.jeeres.FilterDelegate;
import org.emrys.core.runtime.jeeres.ListenerInfo;
import org.emrys.core.runtime.jeeres.ServletDelegate;
import org.emrys.core.runtime.jeeres.TaglibInfo;
import org.osgi.framework.Bundle;


/**
 * Bundle Servlet Context for each web bundle. This ServletContext wrappered the
 * original ServletContext delivered from external Server, and modified its some
 * behaviors like resource loading, request disptcher getting. etc.
 * 
 * @author Leo Chang - EMRYS
 * @version 2011-3-22
 */
public class BundledServletContext implements IBundledServletContext {
	/**
	 * ServletContext Attributes buffer
	 */
	private final Hashtable<String, Object> attributes = new Hashtable<String, Object>();
	/**
	 * Later set parameters buffer
	 */
	private final Hashtable<String, String> laterInitParameters = new Hashtable<String, String>();
	/**
	 * context listeners buffer
	 */
	private final List<ServletContextAttributeListener> contextListeners = null;
	/**
	 * Original global ServletContext
	 */
	ServletContext globalContext = null;
	/**
	 * this bundle's Activator
	 */
	WebComActivator activator = null;
	/**
	 * Servlet Filters buffer
	 */
	private List<FilterDelegate> filters;
	/**
	 * Servlet Listeners buffer.
	 */
	private List<ListenerInfo> listeners;
	/**
	 * Servlets in this ServletContext
	 */
	private List<ServletDelegate> servlets;
	/**
	 * Taglibs
	 */
	private List<TaglibInfo> taglibs;
	/**
	 * OSGiJEEContainer instance
	 */
	private final IOSGiWebContainer jeeContainerSVC;
	/**
	 * WebContent Path of this bundle servlet context.
	 */
	private IPath webContentRootPath;

	/**
	 * indicating whether only use Global Servlet Context. If true, realating
	 * method will directly delegate to globale ServletContext.
	 */
	private boolean onlyUseGlobalContext = false;
	/**
	 * Include Global Context mark.
	 */
	private boolean includeGlobalContext = false;
	/**
	 * Wellcome pages of this bundled ServletContext
	 */
	private List<String> welcomPages;
	/**
	 * Error pages of this bundled ServletContext
	 */
	private Map<Integer, String> errPages;
	/**
	 * The activator instance of this ServletContext bundled in.
	 */
	private WebComActivator webHostActivator;
	private int timeout;

	public BundledServletContext(WebComActivator webComponentActivator) {
		jeeContainerSVC = FwkActivator.getInstance().getJeeContainer();
		globalContext = jeeContainerSVC.getServletContext();
		activator = webComponentActivator;
		initAttributes();
	}

	/**
	 * Initialize attributes of this Servlet Context.
	 */
	private void initAttributes() {
		// copy interesting attribute to this attributes.
		/*
		 * String names[] = { "org.apache.catalina.WELCOME_FILES",
		 * "javax.servlet.context.tempdir", "org.apache.catalina.jsp_classpath",
		 * "org.apache.catalina.resources" };
		 */
		Enumeration en = globalContext.getAttributeNames();
		while (en.hasMoreElements()) {
			String name = (String) en.nextElement();
			attributes.put(name, globalContext.getAttribute(name));
		}

		/*
		 * Attributes from tomcat6
		 * org.apache.AnnotationProcessor=org.apache.catalina
		 * .util.DefaultAnnotationProcessor@5443
		 * javax.servlet.context.tempdir=D:
		 * \btools_sdk1000505\tomcat\work\Catalina\localhost\web
		 * org.apache.catalina
		 * .resources=org.apache.naming.resources.ProxyDirContext@54643
		 * org.apache.catalina.WELCOME_FILES=[Ljava.lang.String;@481958
		 * org.apache
		 * .catalina.jsp_classpath=/D:/btools_sdk1000505/tomcat/webapps
		 * /web/WEB-INF/classes/,...
		 * 
		 * Attributes from weblogic10
		 * javax.servlet.context.tempdir=D:/bea/user_projects
		 * /domains/Leo_domain/
		 * servers/AdminServer/tmp/_WL_user/web/7f9y4c/public
		 * weblogic.servlet.WebAppComponentMBean
		 * =weblogic.management.configuration
		 * .WebAppComponentMBeanImpl@af1e71([Leo_domain
		 * ]/Applications[web]/WebAppComponents[web]
		 * weblogic.servlet.WebAppComponentRuntimeMBean
		 * =weblogic.servlet.internal.WebAppRuntimeMBeanImpl@24e11c
		 */

		// Clear server related attributes, this will discard some special
		// featrue of server of a
		// certain type.
		attributes.remove("weblogic.servlet.WebAppComponentMBean");
		attributes.remove("weblogic.servlet.WebAppComponentRuntimeMBean");
		// set AnnotationProcessor null, not support jsp servlet's annotation.
		// set org.apache.catalina.jsp_classpath as empty
		attributes.remove(AnnotationProcessor.class.getName());
		attributes.remove("org.apache.catalina.jsp_classpath");
	}

	public void setInitParameter(String name, String value) {
		// this.setAttribute(name, value);
		laterInitParameters.put(name, value);
	}

	public ServletContext getGlobalContext() {
		return globalContext;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.EMRYS.components.web.core.IBundledServletContext#getBundle()
	 */
	public Bundle getBundle() {
		return activator.getBundle();
	}

	public WebComActivator getBundleActivator() {
		return activator;
	}

	public Object getAttribute(String name) {
		if (includeGlobalContext) {
			Object value = attributes.get(name);
			if (value == null)
				value = globalContext.getAttribute(name);
			return value;
		}
		if (onlyUseGlobalContext)
			return globalContext.getAttribute(name);
		return attributes.get(name);
	}

	public Enumeration getAttributeNames() {
		if (includeGlobalContext) {
			MergeEnumeration en = new MergeEnumeration(attributes.keys(),
					globalContext.getAttributeNames());
			return en;
		}
		if (onlyUseGlobalContext)
			return globalContext.getAttributeNames();

		return attributes.keys();
	}

	public ServletContext getContext(String uripath) {
		// Allow to switch to another bundled servlet context.
		IPath path = new Path(uripath);
		String seg1 = path.segment(0);
		IBundledServletContext ctx = jeeContainerSVC
				.getBundledServletContext(seg1);
		if (ctx != null)
			return ctx;

		return this;// globalContext.getContext(uripath);
	}

	public String getContextPath() {
		if (onlyUseGlobalContext || getBundleActivator().isHostWebBundle())
			return globalContext.getContextPath();
		else {
			return new Path(globalContext.getContextPath()).append(
					getBundleActivator().getServiceNSPrefix())
					.toPortableString();
		}
	}

	public String getInitParameter(String name) {
		if (includeGlobalContext) {
			String value = laterInitParameters.get(name);
			if (value == null)
				value = globalContext.getInitParameter(name);
			return value;
		}

		if (onlyUseGlobalContext)
			return globalContext.getInitParameter(name);

		return laterInitParameters.get(name);
	}

	public Enumeration getInitParameterNames() {
		if (includeGlobalContext) {
			MergeEnumeration en = new MergeEnumeration(laterInitParameters
					.keys(), globalContext.getInitParameterNames());
			return en;
		}
		if (onlyUseGlobalContext)
			return globalContext.getInitParameterNames();
		return laterInitParameters.keys();
	}

	public int getMajorVersion() {
		return globalContext.getMajorVersion();
	}

	public String getMimeType(String file) {
		return globalContext.getMimeType(file);
	}

	public int getMinorVersion() {
		return globalContext.getMinorVersion();
	}

	public RequestDispatcher getNamedDispatcher(String name) {
		// FIXME: Named Dispatcher not implemented.
		return null;// globalContext.getNamedDispatcher(name);
	}

	public String getRealPath(String path) {
		File root = activator.getResolvedWebContentRoot(false);
		if (root != null && root.exists()) {
			IPath tmpPath = new Path(root.getAbsolutePath());
			tmpPath = tmpPath.append(path);
			if (tmpPath.toFile().exists())
				return tmpPath.toPortableString();

			// if not any file exists, just append the path to the root path and
			// return it.
			return new Path(root.getAbsolutePath()).append(path)
					.toPortableString();
		} else {
			// if this servlet context has not real root in local file system.
			return null;
		}
	}

	public RequestDispatcher getRequestDispatcher(String path) {
		// Dispathcer get from ServletContext, the given path is relative
		// to the context root, and if get from ServletRequest, it's relative to
		// the current servlet path of that request.
		// If is host web bundle ,not use prefix.
		return new BundledRequestDispatcher((getBundleActivator()
				.isHostWebBundle() ? null : getBundleActivator()
				.getServiceNSPrefix()), path);
		// return globalContext.getRequestDispatcher(path);
	}

	public URL getResource(String path) throws MalformedURLException {
		// NOTE: Any need to check that the first and sencond segement are host
		// bundle or this bundle's prefix. YES! Because jasper will reuse
		// compiled .class file if there servlet path is the same, we cann't
		// remove the bundel prefix before call japsr to process jsp file. This
		// job is done below. Check the first segement and remove this bundle's
		// prefix at first.
		IPath p = new Path(path);
		String firstSeg = p.segment(0);
		// Case /hostbundleid/thisbundleid/ format.
		WebComActivator hostWebActivator = getHostWebActivator();
		if (hostWebActivator != null
				&& hostWebActivator.getServiceNSPrefix().equals(firstSeg)) {
			p = p.removeFirstSegments(1);
			path = p.toPortableString();
			firstSeg = p.segment(0);
		}

		if (activator.getServiceNSPrefix().equals(firstSeg)) {
			p = p.removeFirstSegments(1);
			path = p.toPortableString();
		}

		if (path.equals("/WEB-INF/web.xml")
				&& getBundleActivator().isHostWebBundle())
			path = "/WEB-INF/web0.xml";

		File root = activator.getResolvedWebContentRoot(false);
		if (root == null || !root.exists())
			return null;

		File f = null;
		IPath tmpPath = new Path(root.getAbsolutePath());
		tmpPath = tmpPath.append(path);
		f = tmpPath.toFile();
		if (f.exists())
			return f.toURI().toURL();
		else {
			// Search WebContext/WEB-INF/classes for resource.
			tmpPath = new Path(root.getAbsolutePath());
			tmpPath = tmpPath.append("/WEB-INF/classes").append(path);
			f = tmpPath.toFile();
			if (f.exists())
				return f.toURI().toURL();
		}

		// NOTE: Proxy GetResource() to other bundled ServletContext may be
		// tolerant to some exception, but new defect created. Suppress this
		// feature.
		/*
		 * IBundledServletContext ctx = jeeContainerSVC
		 * .getBundledServletContext(firstSeg); if (ctx != null) return
		 * ctx.getResource(path);
		 */
		return null;
	}

	/**
	 * @return
	 */
	private WebComActivator getHostWebActivator() {
		if (webHostActivator == null)
			webHostActivator = FwkRuntime.getInstance()
					.getHostBundleActivator();
		return webHostActivator;
	}

	public InputStream getResourceAsStream(String path) {
		try {
			URL url = getResource(path);
			if (url != null)
				return url.openStream();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Set getResourcePaths(String path) {
		if (path == null || path.length() == 0)
			throw new IllegalArgumentException(
					"The path cann't be null or empty for resource paths.");

		final Set<String> pathStrs = new HashSet<String>();
		if (webContentRootPath == null)
			webContentRootPath = new Path(getBundleActivator()
					.getResolvedWebContentRoot(false).getAbsolutePath());

		if (webContentRootPath == null)
			return pathStrs;

		File root = getBundleActivator().getResolvedWebContentRoot(false);
		IPath p = new Path(root.getAbsolutePath());
		p = p.append(path);
		File f = p.toFile();
		if (f.exists() && f.isDirectory()) {
			File[] fileList = f.listFiles();
			for (int i = 0; i < fileList.length; i++) {
				String fileName = fileList[i].getName();
				// Make sure the path is start with a "/" and end with "/" if
				// this path is a
				// directory.
				IPath currentPath = FileUtil.makeRelativeTo(p.append(fileName),
						webContentRootPath).makeAbsolute();
				String resPathStr = currentPath.toPortableString();
				if (fileList[i].isDirectory() && !resPathStr.endsWith("/"))
					resPathStr = resPathStr + "/";

				pathStrs.add(resPathStr);
			}
		}

		return pathStrs;
	}

	public String getServerInfo() {
		if (includeGlobalContext) {
			return globalContext.getServerInfo()
					+ "; WebOSGi JavaEE Container/1.0.0(Servlet2.5, Equinox OSGi[3.5.2.R35])";
		} else if (onlyUseGlobalContext)
			return globalContext.getServerInfo();
		else
			return "WebOSGi JavaEE Container/1.0.0(Servlet2.5, Equinox OSGi[3.5.2.R35]) & "
					+ globalContext.getServerInfo();
	}

	public Servlet getServlet(String name) throws ServletException {
		if (name == null)
			return null;

		Collection<ServletDelegate> list = this.getServletsInfo();
		for (ServletDelegate info : list) {
			if (name.equals(info.name))
				return info.getServlet();
		}
		return null;
	}

	public String getServletContextName() {
		if (onlyUseGlobalContext)
			return globalContext.getServletContextName();
		else
			return activator.getServiceNSPrefix();
	}

	public Enumeration getServletNames() {
		final Iterator<ServletDelegate> it = getServletsInfo().iterator();
		return new Enumeration() {
			public boolean hasMoreElements() {
				return it.hasNext();
			}

			public Object nextElement() {
				if (it.hasNext())
					return it.next().name;
				return null;
			}
		};
	}

	public Enumeration getServlets() {
		final Iterator<ServletDelegate> it = getServletsInfo().iterator();
		return new Enumeration() {
			public boolean hasMoreElements() {
				return it.hasNext();
			}

			public Object nextElement() {
				if (it.hasNext()) {
					try {
						return it.next().getServlet();
					} catch (ServletException e) {
						e.printStackTrace();
					}
				}
				return null;
			}
		};
	}

	public void log(Exception exception, String msg) {
		globalContext.log(exception, msg);
	}

	public void log(String message, Throwable throwable) {
		globalContext.log(message, throwable);
	}

	public void log(String msg) {
		globalContext.log(msg);
	}

	public void removeAttribute(String name) {
		if (onlyUseGlobalContext)
			globalContext.removeAttribute(name);
		else if (includeGlobalContext) {
			setAttribute(name, null);
			globalContext.removeAttribute(name);
		} else {
			setAttribute(name, null);
		}
	}

	public void setAttribute(String name, Object object) {
		if (onlyUseGlobalContext)
			globalContext.setAttribute(name, object);
		else if (includeGlobalContext) {
			setAttribute0(name, object);
			globalContext.setAttribute(name, object);
		} else {
			setAttribute0(name, object);
		}

	}

	/**
	 * @param name
	 * @param object
	 */
	private void setAttribute0(String name, Object object) {
		Object original = attributes.get(name);
		if (object == null)
			attributes.remove(name);
		else
			attributes.put(name, object);

		ServletContextAttributeEvent scab = new ServletContextAttributeEvent(
				this, name, object);
		int et = -1;
		if (object == null && original != null)
			et = ET_ATTR_REMOVED;
		else if (original != null && !object.equals(original))
			et = ET_ATTR_REPLACED;
		else if (original == null && object != null)
			et = ET_ATTR_ADDED;

		if (et != -1)
			jeeContainerSVC.trigerContextAttrEvent(scab, this, et);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.EMRYS.components.web.core.IBundledServletContext#getFilters()
	 */
	public Collection<FilterDelegate> getFilters() {
		if (filters == null)
			filters = new ArrayList<FilterDelegate>();
		return filters;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.EMRYS.components.web.core.IBundledServletContext#getTaglibs()
	 */
	public List<TaglibInfo> getTaglibs() {
		if (taglibs == null)
			taglibs = new ArrayList<TaglibInfo>();
		return taglibs;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.EMRYS.components.web.core.IBundledServletContext#getListeners()
	 */
	public Collection<ListenerInfo> getListeners() {
		if (listeners == null)
			listeners = new ArrayList<ListenerInfo>();
		return listeners;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.EMRYS.components.web.core.IBundledServletContext#getServletsInfo()
	 */
	public Collection<ServletDelegate> getServletsInfo() {
		if (servlets == null)
			servlets = new ArrayList<ServletDelegate>();
		return servlets;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.EMRYS.components.web.core.IBundledServletContext#setWebRootFolder
	 * (java.io.File)
	 */
	public void setWebRootFolder(IPath webContentRootPath) {
		this.webContentRootPath = webContentRootPath;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.EMRYS.components.web.core.IBundledServletContext#switchState(int)
	 */
	public void switchState(int state) {
		switch (state) {
		case STATE_NORMAL: {
			onlyUseGlobalContext = false;
			includeGlobalContext = false;
			break;
		}
		case STATE_INCLUE_GLOBAL_CTX: {
			onlyUseGlobalContext = false;
			includeGlobalContext = true;
			break;
		}
		case STATE_ONLY_USE_GLOBAL_CTX: {
			onlyUseGlobalContext = true;
			includeGlobalContext = false;
			break;
		}
		default: {
			onlyUseGlobalContext = false;
			includeGlobalContext = false;
		}
		}
	}

	class MergeEnumeration implements Enumeration {
		private final Enumeration e1;
		private final Enumeration e2;

		public MergeEnumeration(Enumeration e1, Enumeration e2) {
			this.e1 = e1;
			this.e2 = e2;
		}

		public boolean hasMoreElements() {
			return e1.hasMoreElements() || e2.hasMoreElements();
		}

		public Object nextElement() {
			if (e1.hasMoreElements())
				return e1.nextElement();
			if (e2.hasMoreElements())
				return e2.nextElement();
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.EMRYS.components.web.core.IBundledServletContext#getErrorPages()
	 */
	public Map<Integer, String> getErrorPages() {
		if (errPages == null)
			errPages = new HashMap<Integer, String>();
		return errPages;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.EMRYS.components.web.core.IBundledServletContext#getWelcomePages()
	 */
	public List<String> getWelcomePages() {
		if (welcomPages == null)
			welcomPages = new ArrayList<String>();
		return welcomPages;
	}

	public int getSessionTimeout() {
		return timeout;
	}

	public void setSessionTimeout(int interval) {
		timeout = interval;
	}
}
