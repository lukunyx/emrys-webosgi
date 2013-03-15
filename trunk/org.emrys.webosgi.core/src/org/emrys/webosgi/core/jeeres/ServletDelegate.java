package org.emrys.webosgi.core.jeeres;

import java.io.IOException;
import java.net.URL;
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
import org.emrys.webosgi.core.FwkActivator;
import org.emrys.webosgi.core.internal.FwkRuntime;
import org.emrys.webosgi.core.jeewrappers.BundledHttpServletRequestWrapper;
import org.emrys.webosgi.core.jsp.JspServletPool;
import org.emrys.webosgi.core.jsp.OSGIJspServlet;
import org.emrys.webosgi.core.runtime.BundleContextRunnable;
import org.emrys.webosgi.core.service.IWebApplication;
import org.osgi.framework.Bundle;

/**
 * 
 * @author Leo Chang
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

	public String jspFile;

	private ServletConfig config;

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

	public void service(final ServletRequest req, final ServletResponse resp)
			throws ServletException, IOException {
		BundleContextRunnable runnable = new BundleContextRunnable(
				getBundleContext()) {
			@Override
			protected IStatus execute() {
				try {
					if (!isInitialized())
						init(null);

					if (servlet != null)
						servlet.service(req, resp);
					else {
						// Process JSP file.
						Bundle bundle = ctx.getBundle();
						OSGIJspServlet jspServlet = JspServletPool
								.getInstance(ctx);
						BundledHttpServletRequestWrapper wrapper = BundledHttpServletRequestWrapper
								.getHttpServletRequestWrapper(req, bundle);
						wrapper.setServletPath(jspFile);
						wrapper.setPathInfo(null);
						jspServlet.service(wrapper, resp);
					}
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
		// init the servlet with ServletConfig
		config = createServletConfig();
		// Servlet Init method should also be executed in Bundle-ClassLoader
		// Context.
		BundleContextRunnable runnable = new BundleContextRunnable(
				getBundleContext()) {
			@Override
			protected IStatus execute() {
				try {
					// Make sure active the web application dynamically.
					IWebApplication webApp = getBundleContext()
							.getWebApplication();
					if (!FwkRuntime.getInstance().makeSureWabActive(webApp)) {
						return new Status(Status.ERROR, FwkActivator
								.getInstance().getBundleSymbleName(),
								"Web appliction not actived and wait timeout to init servlet:"
										+ config.getServletName());
					}

					// If servlet instance be set, not try to load class.
					if (servlet != null
							|| (className != null && className.length() > 0)) {
						if (servlet == null) {
							Class clazz = getBundleContext().getBundle()
									.loadClass(className);
							servlet = (Servlet) clazz.newInstance();
						}
						if (servlet.getServletConfig() == null)
							servlet.init(config);
						setInitialized(true);
					} else if (jspFile != null) {
						URL url = ctx.getResource(jspFile);
						if (url == null)
							throw new ServletException(
									"Jsp file assigneed for servlet: " + name
											+ "not found.");
					} else
						throw new ServletException(
								"Servlet class or jsp file not assigneed for servlet: "
										+ name + ".");
				} catch (Exception e) {
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
			Throwable e = status.getException();
			if (e instanceof ServletException)
				throw (ServletException) e;
			else if (e != null)
				throw new ServletException(e);
			else
				throw new ServletException(status.getMessage());
		}
	}

	protected ServletConfig createServletConfig() {
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
		return config;
	}
}
