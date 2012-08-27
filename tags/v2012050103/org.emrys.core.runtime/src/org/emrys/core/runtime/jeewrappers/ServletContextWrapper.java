/*******************************************************************************
 * Copyright (c) 2010 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.jeewrappers;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.osgi.framework.Bundle;

import org.emrys.core.adapter.internal.IServletObjectWrapper;
import org.emrys.core.adapter.internal.ServletContextAdapter;

/**
 * Use ServletContextAdapter to modify some behaviors of the server's default
 * ServletContext. This wrapper let the init parameter can be modified in a
 * servletCtxAdapter.
 * 
 * @author Leo Chang - EMRYS
 * @version 2010-11-4
 */
public class ServletContextWrapper implements ServletContext, IServletObjectWrapper {

	// Jsp Taglib Information
	public static class TagLibInfo {
		private final String uri;
		private final String prefix;
		private final String location;

		/**
		 * @param uri
		 * @param urlPattern
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

	private final List<TagLibInfo> tagLibs = new ArrayList<TagLibInfo>();

	protected Bundle currentContextBundle = null;
	private static Set<ServletContextWrapper> wrappers = new HashSet<ServletContextWrapper>();
	private final Map<String, String> laterInitParameters = new HashMap<String, String>();

	public static ServletContextWrapper getServletContextWrapper(Object servletContext) {
		if (servletContext == null || servletContext instanceof ServletContextWrapper)
			return (ServletContextWrapper) servletContext;

		if (!(servletContext instanceof ServletContextAdapter))
			return null;

		for (ServletContextWrapper w : wrappers) {
			if (w.getOriginalObject().equals(servletContext))
				return w;
		}
		ServletContextWrapper newWrapper = new ServletContextWrapper(
				(ServletContextAdapter) servletContext);
		wrappers.add(newWrapper);
		return newWrapper;
	}

	private final ServletContextAdapter servletCtxAdapter;

	public ServletContextWrapper(ServletContextAdapter servletContext) {
		this.servletCtxAdapter = servletContext;
	}

	public Object getAttribute(String name) {
		return servletCtxAdapter.getAttribute(name);
	}

	public Enumeration getAttributeNames() {
		return servletCtxAdapter.getAttributeNames();
	}

	public ServletContext getContext(String uripath) {
		return servletCtxAdapter.getContext(uripath);
	}

	public String getContextPath() {
		return servletCtxAdapter.getContextPath();
	}

	public String getInitParameter(String name) {
		String value = laterInitParameters.get(name);
		if (value == null) {
			value = servletCtxAdapter.getInitParameter(name);
		}
		return value;
	}

	public void setInitParameter(String name, String value) {
		this.setAttribute(name, value);
		laterInitParameters.put(name, value);
	}

	public Enumeration getInitParameterNames() {
		return servletCtxAdapter.getInitParameterNames();
	}

	public int getMajorVersion() {
		return servletCtxAdapter.getMajorVersion();
	}

	public String getMimeType(String file) {
		return servletCtxAdapter.getMimeType(file);
	}

	public int getMinorVersion() {
		return servletCtxAdapter.getMinorVersion();
	}

	public RequestDispatcher getNamedDispatcher(String name) {
		return servletCtxAdapter.getNamedDispatcher(name);
	}

	public String getRealPath(String path) {
		return servletCtxAdapter.getRealPath(path);
	}

	public RequestDispatcher getRequestDispatcher(String path) {
		return servletCtxAdapter.getRequestDispatcher(path);
	}

	public URL getResource(String path) throws MalformedURLException {
		return servletCtxAdapter.getResource(path);
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
		return servletCtxAdapter.getResourcePaths(path);
	}

	public String getServerInfo() {
		return servletCtxAdapter.getServerInfo();
	}

	public Servlet getServlet(String name) throws ServletException {
		return servletCtxAdapter.getServlet(name);
	}

	public String getServletContextName() {
		return servletCtxAdapter.getServletContextName();
	}

	public Enumeration getServletNames() {
		return servletCtxAdapter.getServletNames();
	}

	public Enumeration getServlets() {
		return servletCtxAdapter.getServlets();
	}

	public void log(Exception exception, String msg) {
		servletCtxAdapter.log(exception, msg);
	}

	public void log(String message, Throwable throwable) {
		servletCtxAdapter.log(message, throwable);
	}

	public void log(String msg) {
		servletCtxAdapter.log(msg);
	}

	public void removeAttribute(String name) {
		servletCtxAdapter.removeAttribute(name);
	}

	public void setAttribute(String name, Object object) {
		servletCtxAdapter.setAttribute(name, object);
	}

	public Bundle getCurrentContextBundle() {
		return currentContextBundle;
	}

	public void setCurrentContextBundle(Bundle currentContextBundle) {
		this.currentContextBundle = currentContextBundle;
	}

	/**
	 * Get all registered jsp tag lib information from web.xml or other
	 * contributor.
	 * 
	 * @return List<TagLibInfo>
	 */
	public List<TagLibInfo> getTagLibs() {
		return tagLibs;
	}

	/**
	 * Add new tag lib to singleton servletCtxAdapter context.
	 * 
	 * @param taglibInfo
	 */
	public void addTagLib(String uri, String location, String prefix) {
		tagLibs.add(new TagLibInfo(uri, prefix, location));
	}

	/**
	 * Add new tag lib to singleton servletCtxAdapter context.
	 * 
	 * @param taglibInfo
	 */
	public void removeTagLib(String taglibUri) {
		Iterator<TagLibInfo> it = tagLibs.iterator();
		while (it.hasNext()) {
			TagLibInfo ti = it.next();
			if (ti.getUri().equals(taglibUri))
				it.remove();
		}
	}

	public Object getOriginalObject() {
		return servletCtxAdapter;
	}
}
