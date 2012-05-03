/*******************************************************************************
 * Copyright (c) 2011 Hirisun Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Hirisun License v1.0
 * which accompanies this distribution, and is available at
 * http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.jeecontainer;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.emrys.core.runtime.internal.FwkRuntime;
import org.emrys.core.runtime.jeewrappers.BundledHttpServletRequestWrapper;
import org.emrys.core.runtime.jeewrappers.HttpServletResponseWrapper;


/**
 * The Bundled Request Dispatcher.
 * 
 * @see {@link com.hirisun.components.web.core.jeecontainer.BundledServletContext #getRequestDispatcher(String)}
 * @author Leo Chang - Hirisun
 * @version 2011-3-31
 */
public class BundledRequestDispatcher implements RequestDispatcher {
	private final String prefix;
	private final OSGiJEEContainer jeeContainerSVC;
	private String targetPath;
	private Object include_context_path;
	private Object include_servlet_path;
	private Object include_path_info;
	private Object include_request_uri;
	private Object include_query_string;
	private boolean doInternalFilter;

	public BundledRequestDispatcher(String bundlePrefix, String path) {
		this.prefix = bundlePrefix;
		this.targetPath = path;
		jeeContainerSVC = FwkRuntime.getInstance().getJeeContainer();
	}

	public void forward(ServletRequest request, ServletResponse response)
			throws ServletException, IOException {
		service(request, response, false);
	}

	public void include(ServletRequest request, ServletResponse response)
			throws ServletException, IOException {
		// Store these attribute in this request.
		/*
		 * javax.servlet.include.context_path = ContextPath
		 * 銆�銆�javax.servlet.include.servlet_path = ServletPath
		 * 銆�銆�javax.servlet.include.path_info = PathInfo
		 * 銆�銆�javax.servlet.include.query_string = QueryString
		 * 銆�銆�javax.servlet.include.request_uri = RequestURI
		 */
		service(request, response, true);
	}

	/**
	 * Serve the reqeust, wiring back to the
	 * {@link com.hirisun.components.web.core.jeecontainer.OSGiJEEContainer#service(ServletRequest, ServletResponse)}
	 * method. Forward and include on this method, will not trigger the servlet
	 * filters.
	 * 
	 * @param request
	 * @param response
	 * @param isInclude
	 * @throws IOException
	 * @throws ServletException
	 */
	private void service(ServletRequest request, ServletResponse response,
			boolean isInclude) throws ServletException, IOException {
		BundledHttpServletRequestWrapper topRequest = (BundledHttpServletRequestWrapper) jeeContainerSVC
				.getReqThreadVariants().get(OSGiJEEContainer.THREAD_V_REQUEST);

		// Try to wrapper the given request and response.
		BundledHttpServletRequestWrapper reqWrapper = BundledHttpServletRequestWrapper
				.getHttpServletRequestWrapper(request, topRequest.getBundle());
		HttpServletResponseWrapper respWrapper = HttpServletResponseWrapper
				.getHttpServletResponseWrapper(response);

		// Buffer the former state and revert them at last.
		boolean oIsDispatched = reqWrapper.isDispatched();
		boolean oIsInclude = reqWrapper.isInclude();
		String oServletPath = reqWrapper.getServletPath();
		String oPathInfo = reqWrapper.getPathInfo();

		try {
			if (targetPath.startsWith("#{system}")) {
				targetPath = targetPath.replace("#{system}", "");
			} else {
				if (!targetPath.startsWith("/")) {
					// Relative path not allowed, regard it as absolute.
					targetPath = "/" + targetPath;
				}

				if (this.prefix != null && this.prefix.length() != 0
						&& !targetPath.startsWith("/" + this.prefix + "/")) {
					targetPath = "/" + this.prefix + targetPath;
				}
			}

			// parse new parameters.
			int index = targetPath.indexOf('?');
			if (index != -1) {
				String paraStr = targetPath.substring(index + 1);
				targetPath = targetPath.substring(0, index);
				String[] tmps = paraStr.split("[=&]+");
				String name = null;
				String value = null;
				for (int i = 0; i < tmps.length; i++) {
					if (i % 2 == 0)
						name = tmps[i];
					else {
						value = tmps[i];
						reqWrapper.getNewParameters().put(name,
								new String[] { value });
						name = null;
						value = null;
					}
				}
			}

			// set new path to dispatch to.
			reqWrapper.setServletPath(targetPath);
			reqWrapper.setPathInfo(null);

			// If need do filter, not set dispatching ad include mark.
			if (!doInternalFilter) {
				// set the dispatching mark.
				if (!isInclude)
					respWrapper.reset();
				reqWrapper.setDispatched(true);
				reqWrapper.setInclude(isInclude);
				// set include in response wrapper as well.
				respWrapper.setInclude(isInclude);

				// FIXME: If the original Request paths should not be modified
				// and just set several include_xxx attributes???
				if (isInclude && request instanceof HttpServletRequest) {
					// Store original include attribute.
					include_context_path = request
							.getAttribute("javax.servlet.include.context_path");
					include_servlet_path = request
							.getAttribute("javax.servlet.include.servlet_path");
					include_path_info = request
							.getAttribute("javax.servlet.include.path_info");
					include_request_uri = request
							.getAttribute("javax.servlet.include.request_uri");
					include_query_string = request
							.getAttribute("javax.servlet.include.query_string");

					request.setAttribute("javax.servlet.include.context_path",
							((HttpServletRequest) request).getSession()
									.getServletContext().getContextPath());
					request.setAttribute("javax.servlet.include.servlet_path",
							((HttpServletRequest) request).getServletPath());
					request.setAttribute("javax.servlet.include.path_info",
							((HttpServletRequest) request).getPathInfo());

					request.setAttribute("javax.servlet.include.request_uri",
							((HttpServletRequest) request).getRequestURI());
					request.setAttribute("javax.servlet.include.query_string",
							((HttpServletRequest) request).getQueryString());

				}
			}

			// rewire to JEE Container.
			jeeContainerSVC.service(reqWrapper, respWrapper);
		} finally {
			// reset the include attributes at last.
			if (!doInternalFilter && isInclude
					&& request instanceof HttpServletRequest) {
				// Store original include attribute.
				request.setAttribute("javax.servlet.include.context_path",
						include_context_path);
				request.setAttribute("javax.servlet.include.servlet_path",
						include_servlet_path);
				request.setAttribute("javax.servlet.include.path_info",
						include_path_info);
				request.setAttribute("javax.servlet.include.request_uri",
						include_request_uri);
				request.setAttribute("javax.servlet.include.query_string",
						include_query_string);
			}

			// revert the buffered state.
			reqWrapper.setServletPath(oServletPath);
			reqWrapper.setPathInfo(oPathInfo);
			reqWrapper.setDispatched(oIsDispatched);
			reqWrapper.setInclude(oIsInclude);
			respWrapper.setInclude(oIsInclude);
		}
	}

	public void setDoFilterInternal(boolean b) {
		doInternalFilter = b;
	}
}
