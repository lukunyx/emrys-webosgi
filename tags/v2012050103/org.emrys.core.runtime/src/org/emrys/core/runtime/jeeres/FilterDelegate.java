/*******************************************************************************
 * Copyright (c) 2011 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.jeeres;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.emrys.core.runtime.FwkActivator;
import org.emrys.core.runtime.jeecontainer.BundleContextRunnable;


/**
 * 
 * @author Leo Chang - EMRYS
 * @version 2011-3-22
 */
public class FilterDelegate extends AbstMultiInstUrlMapObject implements Filter {
	private Filter filter;
	public Hashtable<String, String> parameters;
	public String name;
	public String targetServletNames;
	public String dispatcherNames;
	public String clazzName;

	public void destroy() {
		if (isInitialized()) {
			BundleContextRunnable runnable = new BundleContextRunnable(
					getBundleContext()) {
				@Override
				protected IStatus execute() {
					filter.destroy();
					return Status.OK_STATUS;
				}
			};
			runnable.run();
		}
	}

	public void doFilter(final ServletRequest request,
			final ServletResponse response, final FilterChain chain)
			throws IOException, ServletException {

		BundleContextRunnable runnable = new BundleContextRunnable(
				getBundleContext()) {
			@Override
			protected IStatus execute() {
				try {
					if (!isInitialized())
						init(null);
					filter.doFilter(request, response, chain);
					return Status.OK_STATUS;
				} catch (Exception e) {
					// e.printStackTrace();
					// If http socket reset by client, may threw IOException
					// like SocketException. Ignore it.
					if (e instanceof IOException) {
						return Status.OK_STATUS;
					} else {
						return new Status(Status.ERROR, FwkActivator
								.getInstance().getBundleSymbleName(),
								"Servlet Filter execute failed[" + clazzName
										+ "]", e);
					}
				}
			}
		};

		runnable.run();
		IStatus status = runnable.getResult();
		if (!status.isOK()) {
			throw new ServletException(status.getException());
		}
	}

	public void init(FilterConfig nullConfig) throws ServletException {
		BundleContextRunnable runnable = new BundleContextRunnable(
				getBundleContext()) {
			@Override
			protected IStatus execute() {
				FilterConfig filterConfig = new FilterConfig() {
					public String getFilterName() {
						return name;
					}

					public String getInitParameter(String name) {
						if (parameters != null)
							return parameters.get(name);
						return null;
					}

					public Enumeration getInitParameterNames() {
						if (parameters != null)
							return parameters.keys();
						return null;
					}

					public ServletContext getServletContext() {
						return ctx;
					}
				};

				// Lazy load filter class.
				if (filter == null) {
					try {
						Class clazz = getBundleContext().getBundle().loadClass(
								clazzName);
						filter = (Filter) clazz.newInstance();
					} catch (Exception e) {
						// e.printStackTrace();
						return new Status(Status.ERROR, FwkActivator
								.getInstance().getBundleSymbleName(),
								"Filter Instance Create failed[" + clazzName
										+ "]", e);
					}
				}

				try {
					filter.init(filterConfig);
				} catch (ServletException e) {
					// e.printStackTrace();
					return new Status(Status.ERROR, FwkActivator.getInstance()
							.getBundleSymbleName(), "Filter Init failed["
							+ clazzName + "]", e);
				}
				setInitialized(true);
				return Status.OK_STATUS;
			}
		};

		runnable.run();
		IStatus status = runnable.getResult();
		if (!status.isOK()) {
			Throwable e = status.getException();
			if (e instanceof ServletException)
				throw new ServletException(e);
			else
				throw new ServletException(status.getException());
		}
	}
}
