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

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
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
public class ServletDelegate extends AbstMultiInstUrlMapObject implements
		Servlet {
	public Servlet servlet;

	public String name;
	public String className;
	public int loadOnSetupPriority = 0;
	public Hashtable<String, String> parameters;
	public boolean possible = false;

	public Servlet getServlet() throws ServletException {
		if (!isInitialized())
			init(null);
		return servlet;
	}

	public void destroy() {
		if (!isInitialized())
			return;
		BundleContextRunnable runnable = new BundleContextRunnable(
				getBundleContext()) {
			@Override
			protected IStatus execute() {
				try {
					servlet.destroy();
					return Status.OK_STATUS;
				} catch (Exception e) {
					// e.printStackTrace();
					return new Status(Status.ERROR, FwkActivator.getInstance()
							.getBundleSymbleName(), "Servlet destroy failed["
							+ className + "]", e);
				}
			}
		};
		runnable.run();
	}

	public ServletConfig getServletConfig() {
		if (!isInitialized())
			return null;
		return servlet.getServletConfig();
	}

	public String getServletInfo() {
		if (!isInitialized())
			return null;
		return servlet.getServletInfo();
	}

	public void service(final ServletRequest req, final ServletResponse res)
			throws ServletException, IOException {
		BundleContextRunnable runnable = new BundleContextRunnable(
				getBundleContext()) {
			@Override
			protected IStatus execute() {
				try {
					if (!isInitialized())
						init(null);

					servlet.service(req, res);
					return Status.OK_STATUS;
				} catch (Exception e) {
					// e.printStackTrace();
					return new Status(Status.ERROR, FwkActivator.getInstance()
							.getBundleSymbleName(), "Servlet service failed["
							+ className + "]", e);
				}
			}
		};

		runnable.run();
		IStatus status = runnable.getResult();
		if (!status.isOK()) {
			throw new ServletException(status.getException());
		}
	}

	/**
	 * @throws ServletException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * 
	 */
	public void init(ServletConfig nullConfig) throws ServletException {
		// Servlet Init method should also be executed in Bundle-ClassLoader
		// Context.
		BundleContextRunnable runnable = new BundleContextRunnable(
				getBundleContext()) {
			@Override
			protected IStatus execute() {
				try {
					// init the servlet with ServletConfig
					ServletConfig config = new ServletConfig() {
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

						public String getServletName() {
							return name;
						}
					};

					if (servlet == null) {
						try {
							Class clazz = getBundleContext().getBundle()
									.loadClass(className);
							servlet = (Servlet) clazz.newInstance();
						} catch (Exception e) {
							e.printStackTrace();
							throw new ServletException("Init servlet: "
									+ className
									+ "from bundle: "
									+ getBundleContext().getBundle()
											.getBundleId() + " failed.", e);
						}
					}

					if (servlet.getServletConfig() == null)
						servlet.init(config);
					setInitialized(true);
				} catch (Exception e) {
					e.printStackTrace();
					return new Status(Status.ERROR, FwkActivator.getInstance()
							.getBundleSymbleName(), "Servlet Init failed["
							+ className + "]", e);
				}
				return Status.OK_STATUS;
			}
		};

		runnable.run();
		IStatus status = runnable.getResult();
		if (!status.isOK()) {
			throw new ServletException(status.getException());
		}
	}
}
