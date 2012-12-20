/*******************************************************************************
 * Copyright (c) 2010 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.adapter.internal;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import javax.servlet.*;
import org.osgi.framework.Bundle;

/**
 * Use ServletContextAdapter to modify some behaviors of the server's default ServletContext. This
 * wrapper let the init parameter can be modified in a servlet.
 * 
 * @author Leo Chang - EMRYS
 * @version 2010-11-4
 */
public class ServletContextAdapter implements IServletObjectWrapper {

	// Jsp Taglib Information
	public static class TagLibInfo {
		private String uri;
		private String prefix;
		private String location;

		/**
		 * @param uri
		 * @param prefix
		 * @param location
		 */
		public TagLibInfo(String uri, String prefix, String location) {
			this.uri = uri;
			this.prefix = prefix;
			this.location = location;
		}

		public String getUri() {
			return uri;
		}

		public String getPrefix() {
			return prefix;
		}

		public String getLocation() {
			return location;
		}
	}

	protected Bundle currentContextBundle = null;
	private static Set<ServletContextAdapter> wrappers = new HashSet<ServletContextAdapter>();
	private Map<String, String> laterInitParameters = new HashMap<String, String>();

	public static ServletContextAdapter getServletContextWrapper(ServletContext servlet) {
		if (servlet == null || servlet instanceof ServletContextAdapter)
			return (ServletContextAdapter) servlet;
		for (ServletContextAdapter w : wrappers) {
			if (w.getWrapperedServlet().equals(servlet))
				return w;
		}
		ServletContextAdapter newWrapper = new ServletContextAdapter(servlet);
		wrappers.add(newWrapper);
		return newWrapper;
	}

	private ServletContext servlet;
	private String contextPath;

	public ServletContextAdapter(ServletContext servlet) {
		this.servlet = servlet;
	}

	public Object getAttribute(String name) {
		return servlet.getAttribute(name);
	}

	public Enumeration getAttributeNames() {
		return servlet.getAttributeNames();
	}

	public ServletContext getContext(String uripath) {
		return servlet.getContext(uripath);
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public String getContextPath() {
		if (contextPath != null)
			return contextPath;

		try {
			Method method = servlet.getClass().getMethod("getContextPath");
			return (String) method.invoke(servlet);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

		return null;
	}

	public String getInitParameter(String name) {
		String value = laterInitParameters.get(name);
		if (value == null) {
			value = servlet.getInitParameter(name);
		}
		return value;
	}

	public void setInitParameter(String name, String value) {
		this.setAttribute(name, value);
		laterInitParameters.put(name, value);
	}

	public Enumeration getInitParameterNames() {
		return servlet.getInitParameterNames();
	}

	public int getMajorVersion() {
		return servlet.getMajorVersion();
	}

	public String getMimeType(String file) {
		return servlet.getMimeType(file);
	}

	public int getMinorVersion() {
		return servlet.getMinorVersion();
	}

	public RequestDispatcher getNamedDispatcher(String name) {
		return servlet.getNamedDispatcher(name);
	}

	public String getRealPath(String path) {
		return servlet.getRealPath(path);
	}

	public RequestDispatcher getRequestDispatcher(String path) {
		return servlet.getRequestDispatcher(path);
	}

	public URL getResource(String path) throws MalformedURLException {
		return servlet.getResource(path);
	}

	public InputStream getResourceAsStream(String path) {
		try {
			InputStream in = getResource(path).openStream();
			return in;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	public Set getResourcePaths(String path) {
		return servlet.getResourcePaths(path);
	}

	public String getServerInfo() {
		return servlet.getServerInfo();
	}

	public Servlet getServlet(String name) throws ServletException {
		return servlet.getServlet(name);
	}

	public String getServletContextName() {
		return servlet.getServletContextName();
	}

	public Enumeration getServletNames() {
		return servlet.getServletNames();
	}

	public Enumeration getServlets() {
		return servlet.getServlets();
	}

	public void log(Exception exception, String msg) {
		servlet.log(exception, msg);
	}

	public void log(String message, Throwable throwable) {
		servlet.log(message, throwable);
	}

	public void log(String msg) {
		servlet.log(msg);
	}

	public void removeAttribute(String name) {
		servlet.removeAttribute(name);
	}

	public void setAttribute(String name, Object object) {
		servlet.setAttribute(name, object);
	}

	public ServletContext getWrapperedServlet() {
		return servlet;
	}

	public Object getOriginalObject() {
		return servlet;
	}
}
