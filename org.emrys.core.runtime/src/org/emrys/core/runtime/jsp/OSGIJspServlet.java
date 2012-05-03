/*******************************************************************************
 * Copyright (c) 2011 Hirisun Corporation. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Hirisun License v1.0 which accompanies this
 * distribution, and is available at http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.jsp;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.emrys.core.runtime.classloader.BundledClassLoaderFactory;
import org.osgi.framework.Bundle;


/**
 * OSGi Jsp Serlvet to compile a .jsp file and execute the compiled .class. This
 * servlet benefite from {@link org.apache.jasper.servlet.JspServlet}.
 * 
 * @author Leo Chang - Hirisun
 * @version 2011-1-12
 */
public class OSGIJspServlet extends HttpServlet {
	private static final long serialVersionUID = -3110476909139807652L;
	/**
	 * Wrappred jasper servlet.
	 */
	private final Servlet jspServlet = new org.apache.jasper.servlet.JspServlet();
	/**
	 * current bundle of the .jsp file.
	 */
	Bundle bundle;
	/**
	 * URl ClassLoader for jsp Serlvet
	 */
	private final URLClassLoader jspLoader;

	public OSGIJspServlet(Bundle bundle) {
		this.bundle = bundle;
		jspLoader = BundledClassLoaderFactory
				.getBundledJspUrlClassLoader(bundle);
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		ClassLoader original = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(jspLoader);
			// ClassLoader c = jspServlet.getClass().getClassLoader();
			jspServlet.init(new ServletConfigAdaptor(config));
		} finally {
			Thread.currentThread().setContextClassLoader(original);
		}
	}

	@Override
	public void destroy() {
		ClassLoader original = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(jspLoader);
			jspServlet.destroy();
		} finally {
			Thread.currentThread().setContextClassLoader(original);
		}
	}

	@Override
	public void service(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String pathInfo = request.getPathInfo();
		if (pathInfo != null && pathInfo.startsWith("/WEB-INF/")) { //$NON-NLS-1$
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		ClassLoader original = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(jspLoader);
			jspServlet.service(request, response);
		} finally {
			Thread.currentThread().setContextClassLoader(original);
		}
	}

	@Override
	public ServletConfig getServletConfig() {
		return jspServlet.getServletConfig();
	}

	@Override
	public String getServletInfo() {
		return jspServlet.getServletInfo();
	}

	private class ServletConfigAdaptor implements ServletConfig {
		private final ServletConfig config;
		private final ServletContext context;

		public ServletConfigAdaptor(ServletConfig config) {
			this.config = config;
			this.context = new ServletContextAdaptor(config.getServletContext());
		}

		public String getInitParameter(String arg0) {
			return config.getInitParameter(arg0);
		}

		public Enumeration getInitParameterNames() {
			return config.getInitParameterNames();
		}

		public ServletContext getServletContext() {
			return context;
		}

		public String getServletName() {
			return config.getServletName();
		}
	}

	/**
	 * ServletContext's Adapter, this class delegate a given ServletContext.
	 * 
	 * @author Leo Chang - Hirisun
	 * @version 2011-7-26
	 */
	private class ServletContextAdaptor implements ServletContext {
		private final ServletContext delegate;

		public ServletContextAdaptor(ServletContext delegate) {
			this.delegate = delegate;
		}

		public URL getResource(String name) throws MalformedURLException {
			return delegate.getResource(name);
		}

		public InputStream getResourceAsStream(String name) {
			try {
				URL resourceURL = getResource(name);
				if (resourceURL != null)
					return resourceURL.openStream();
			} catch (IOException e) {
				log("Error opening stream for resource '" + name + "'", e); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return null;
		}

		public Set getResourcePaths(String name) {
			return delegate.getResourcePaths(name);
		}

		public RequestDispatcher getRequestDispatcher(String arg0) {
			return delegate.getRequestDispatcher(arg0);
		}

		public Object getAttribute(String arg0) {
			return delegate.getAttribute(arg0);
		}

		public Enumeration getAttributeNames() {
			return delegate.getAttributeNames();
		}

		public ServletContext getContext(String arg0) {
			return delegate.getContext(arg0);
		}

		public String getInitParameter(String arg0) {
			return delegate.getInitParameter(arg0);
		}

		public Enumeration getInitParameterNames() {
			return delegate.getInitParameterNames();
		}

		public int getMajorVersion() {
			return delegate.getMajorVersion();
		}

		public String getMimeType(String arg0) {
			return delegate.getMimeType(arg0);
		}

		public int getMinorVersion() {
			return delegate.getMinorVersion();
		}

		public RequestDispatcher getNamedDispatcher(String arg0) {
			return delegate.getNamedDispatcher(arg0);
		}

		public String getRealPath(String arg0) {
			return delegate.getRealPath(arg0);
		}

		public String getServerInfo() {
			return delegate.getServerInfo();
		}

		/** @deprecated **/
		@Deprecated
		public Servlet getServlet(String arg0) throws ServletException {
			return delegate.getServlet(arg0);
		}

		public String getServletContextName() {
			return delegate.getServletContextName();
		}

		/** @deprecated **/
		@Deprecated
		public Enumeration getServletNames() {
			return delegate.getServletNames();
		}

		/** @deprecated **/
		@Deprecated
		public Enumeration getServlets() {
			return delegate.getServlets();
		}

		/** @deprecated **/
		@Deprecated
		public void log(Exception arg0, String arg1) {
			delegate.log(arg0, arg1);
		}

		public void log(String arg0, Throwable arg1) {
			delegate.log(arg0, arg1);
		}

		public void log(String arg0) {
			delegate.log(arg0);
		}

		public void removeAttribute(String arg0) {
			delegate.removeAttribute(arg0);
		}

		public void setAttribute(String arg0, Object arg1) {
			delegate.setAttribute(arg0, arg1);
		}

		// Added in Servlet 2.5
		public String getContextPath() {
			try {
				Method getContextPathMethod = delegate.getClass().getMethod(
						"getContextPath", null); //$NON-NLS-1$
				return (String) getContextPathMethod.invoke(delegate, null);
			} catch (Exception e) {
				// ignore
			}
			return null;
		}
	}
}
