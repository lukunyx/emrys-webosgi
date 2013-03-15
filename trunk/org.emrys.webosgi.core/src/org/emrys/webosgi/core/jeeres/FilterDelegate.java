package org.emrys.webosgi.core.jeeres;

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
import org.emrys.webosgi.core.FwkActivator;
import org.emrys.webosgi.core.internal.FwkRuntime;
import org.emrys.webosgi.core.runtime.BundleContextRunnable;
import org.emrys.webosgi.core.service.IWebApplication;

/**
 * 
 * @author Leo Chang
 * @version 2011-3-22
 */
public class FilterDelegate extends AbstMultiInstUrlMapObject implements Filter {
	public enum DISPATCHERS {
		REQUEST, FORWARD, INCLUDE, EXCEPTION, ERROR
	}

	public static final String DISPATCHERS_SEPERATOR = "|";
	public static final String DISPATCHERS_PARTTERN_SEPERATOR = ">";

	public Hashtable<String, String> urlOrServletMapWithDispatchers;
	public String targetServletNames;
	private final FilterDelegateBase filterBase;

	public FilterDelegate(FilterDelegateBase filterBase) {
		this.filterBase = filterBase;
	}

	public void destroy() {
		if (isInitialized()) {
			BundleContextRunnable runnable = new BundleContextRunnable(
					getBundleContext()) {
				@Override
				protected IStatus execute() {
					filterBase.filter.destroy();
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
					filterBase.filter.doFilter(request, response, chain);
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
								"Servlet Filter execute failed["
										+ filterBase.clazzName + "]", e);
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
				// Make sure active the web application dynamically.
				IWebApplication webApp = getBundleContext().getWebApplication();
				if (!FwkRuntime.getInstance().makeSureWabActive(webApp)) {
					return new Status(Status.ERROR, FwkActivator.getInstance()
							.getBundleSymbleName(),
							"Web appliction not actived and wait timeout to do servlet filter:"
									+ filterBase.name);
				}

				FilterConfig filterConfig = new FilterConfig() {
					public String getFilterName() {
						return filterBase.name;
					}

					public String getInitParameter(String name) {
						if (filterBase.parameters != null)
							return filterBase.parameters.get(name);
						return null;
					}

					public Enumeration getInitParameterNames() {
						if (filterBase.parameters != null)
							return filterBase.parameters.keys();
						return null;
					}

					public ServletContext getServletContext() {
						return ctx;
					}
				};

				// Lazy load filter class.
				if (filterBase.filter == null) {
					try {
						Class clazz = getBundleContext().getBundle().loadClass(
								filterBase.clazzName);
						filterBase.filter = (Filter) clazz.newInstance();
					} catch (Exception e) {
						// e.printStackTrace();
						return new Status(Status.ERROR, FwkActivator
								.getInstance().getBundleSymbleName(),
								"Filter Instance Create failed["
										+ filterBase.clazzName + "]", e);
					}
				}

				try {
					filterBase.filter.init(filterConfig);
				} catch (ServletException e) {
					// e.printStackTrace();
					return new Status(Status.ERROR, FwkActivator.getInstance()
							.getBundleSymbleName(), "Filter Init failed["
							+ filterBase.clazzName + "]", e);
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
				throw (ServletException) e;
			else if (e != null)
				throw new ServletException(e);
			else
				throw new ServletException(status.getMessage());
		}
	}

	@Override
	public int getIndentityHashCode() {
		return filterBase.hashCode();
	}

	public String getFilterName() {
		return filterBase.name;
	}
}
