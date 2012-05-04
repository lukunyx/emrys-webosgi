/*******************************************************************************
 * Copyright (c) 2011 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.jeecontainer;

import java.io.IOException;
import java.net.SocketException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.emrys.common.ComActivator;
import org.emrys.core.runtime.FwkActivator;
import org.emrys.core.runtime.IFwkConstants;
import org.emrys.core.runtime.WebComActivator;
import org.emrys.core.runtime.handlers.IFwkHandlerChain;
import org.emrys.core.runtime.handlers.IFwkRequestHandler;
import org.emrys.core.runtime.internal.FwkRuntime;
import org.emrys.core.runtime.jeeres.FilterDelegate;
import org.emrys.core.runtime.jeeres.ServletDelegate;
import org.emrys.core.runtime.jeewrappers.BundledHttpServletRequestWrapper;
import org.emrys.core.runtime.jeewrappers.HttpServletResponseWrapper;
import org.emrys.core.runtime.jeewrappers.ServletContextWrapper;
import org.emrys.core.runtime.util.NamedThreadLocal;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

import org.emrys.core.adapter.internal.HttpServletRequestAdapter;
import org.emrys.core.adapter.internal.HttpServletResponseAdapter;

/**
 * OSGi JEE runtime container. The original ServletRequest from server will be
 * transformed from BridgeServiet to this serlvet by java reflect method
 * invoking
 * 
 * <code>serviet()<code> method. And then, the original SerlvetRequest will be wrapered back to javax.servlet.SerlvetRequest, and be dispatched to mapping servlet provided from Web Bundles.
 * 
 * @author Leo Chang - EMRYS
 * @version 2011-3-22
 */
public class OSGiJEEContainer extends GenericServlet implements
		IOSGiWebContainer, BundleListener, Servlet, ServletConfig,
		IFwkConstants {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8570607384054854413L;

	/**
	 * Thread local variants for each Request thread. See
	 * {@link #getReqThreadVariants()} to get or put variants.
	 */
	private static final NamedThreadLocal<Map<String, Object>> reqThreadVariants = new NamedThreadLocal<Map<String, Object>>(
			OSGiJEEContainer.class.getName()) {
		@Override
		protected Map<String, Object> initialValue() {
			// initialize variants.
			return new HashMap<String, Object>();
		}
	};

	/**
	 * helper including many convenient methods intend to make code clear.
	 */
	protected OSGiJEEContainerHelper helper;
	/**
	 * BundledServletContext buffer.
	 */
	protected Set<IBundledServletContext> bundleCtxs;
	protected final Set<IFwkRequestHandler> reqHandlers;
	/**
	 * Host web bundle's ServletContext
	 */
	private IBundledServletContext hostServletContext;
	private final FwkActivator activator;
	private boolean containerInited;

	public OSGiJEEContainer() {
		activator = FwkActivator.getInstance();
		bundleCtxs = new HashSet<IBundledServletContext>();
		reqHandlers = new HashSet<IFwkRequestHandler>();
	}

	public boolean isInitialized() {
		return containerInited;
	}

	@Override
	public synchronized void init() throws ServletException {
		// JavaEE container should be initialized here when the first request
		// comes by framework. In constructor method of this class there is risk
		// that some global resource not available, such as framework web
		// context path.
		if (containerInited)
			return;
		helper = new OSGiJEEContainerHelper(this);
		FwkActivator.getInstance().earlyInitOnFwkStarted();
		containerInited = true;
	}

	/**
	 * 
	 * Get thread local variants for each Request thread.
	 * 
	 * @return the reqthreadvariants
	 */
	public Map<String, Object> getReqThreadVariants() {
		return reqThreadVariants.get();
	}

	@Override
	public ServletContext getServletContext() {
		// Get the framework top servlet context.
		// NOTE: Some attribute of this Framework Container's context may not
		// available if this method invoked to early before the invoke of init()
		// of this class.
		return ServletContextWrapper.getServletContextWrapper(FwkRuntime
				.getInstance().getFrameworkAttribute(ATTR_FWK_WEB_APP_CTX));
	}

	public void refresh() throws Exception {
		helper.refresh();
	}

	public void addBundledServletContext(IBundledServletContext ctx)
			throws Exception {
		if (ctx != null) {
			synchronized (bundleCtxs) {
				bundleCtxs.add(ctx);
			}
			// not refresh here, in the 2end start period of web bundles.
			// refresh();
			// Notify servelt context listeners.
			trigerContextEvent(ctx, true);
		}
	}

	public ServletContext findServletContext(Bundle bundle) {
		if (bundleCtxs == null || bundle == null) {
			return this.getServletContext();
		}
		synchronized (bundleCtxs) {
			for (IBundledServletContext ctx : bundleCtxs) {
				if (ctx.getBundle().equals(bundle))
					return ctx;
			}
		}
		return null;
	}

	public void removeBundledServletContext(IBundledServletContext ctx) {
		if (bundleCtxs != null) {
			boolean ok = false;
			synchronized (bundleCtxs) {
				ok = bundleCtxs.remove(ctx);
			}

			if (ok) {
				Collection<ServletDelegate> servlets = ctx.getServletsInfo();
				for (ServletDelegate info : servlets)
					info.destroy();

				Collection<FilterDelegate> filters = ctx.getFilters();
				for (FilterDelegate info : filters)
					info.destroy();

				trigerContextEvent(ctx, false);
			}
		}
	}

	public void trigerContextAttrEvent(final ServletContextAttributeEvent scab,
			final IBundledServletContext ctx, final int et) {
		BundleContextRunnable runnable = new BundleContextRunnable(ctx) {
			@Override
			protected IStatus execute() {
				List<ServletContextAttributeListener> listeners = helper
						.findListeners(ctx,
								ServletContextAttributeListener.class);
				for (ServletContextAttributeListener l : listeners) {
					switch (et) {
					case IBundledServletContext.ET_ATTR_ADDED: {
						l.attributeAdded(scab);
						break;
					}
					case IBundledServletContext.ET_ATTR_REMOVED: {
						l.attributeRemoved(scab);
						break;
					}
					case IBundledServletContext.ET_ATTR_REPLACED: {
						l.attributeReplaced(scab);
						break;
					}
					default:
						break;
					}
				}
				return Status.OK_STATUS;
			}
		};
		runnable.run();
		// IStatus r = runnable.getResult();

	}

	/**
	 * Trigger the context listeners.
	 * 
	 * @param ctx
	 * @param b
	 */
	private void trigerContextEvent(final IBundledServletContext ctx,
			final boolean created) {
		BundleContextRunnable runnable = new BundleContextRunnable(ctx) {
			@Override
			protected IStatus execute() {
				// Collect all filters, servlets by a certain bundle order.
				Collection<ServletContextListener> listeners = helper
						.findListeners(ctx, ServletContextListener.class);
				for (ServletContextListener l : listeners) {
					// FIXME: here need to change the thread context
					// classloader???
					ServletContextEvent sce = new ServletContextEvent(ctx);
					if (created)
						l.contextInitialized(sce);
					else
						l.contextDestroyed(sce);
				}
				return Status.OK_STATUS;
			}
		};
		runnable.run();
		// IStatus r = runnable.getResult();
	}

	/**
	 * The forgedservice method for bridget servlet invoking outside. This
	 * method will wait for the framework started all web bundle serivce at
	 * first. And then wrapper the {@link HttpServletRequestAdapter} and
	 * {@link HttpServletResponseAdapter} as servlet object defined inside
	 * framework.
	 * 
	 * @param req
	 *            Request adatper from bridge servlet.
	 * @param res
	 *            Response adapter from bridge servlet.
	 * @throws Exception
	 *             any exception.
	 */
	public void service(HttpServletRequestAdapter req,
			HttpServletResponseAdapter res) throws Exception {
		// Wrapper the original HttpServletRequest and HttpServletResponse and
		// buffer them in Thread Local variants.
		BundledHttpServletRequestWrapper topReq = BundledHttpServletRequestWrapper
				.getHttpServletRequestWrapper(req, null);
		HttpServletResponseWrapper topResp = HttpServletResponseWrapper
				.getHttpServletResponseWrapper(res);

		getReqThreadVariants().put(OSGiJEEContainer.THREAD_V_REQUEST, topReq);
		getReqThreadVariants().put(OSGiJEEContainer.THREAD_V_RESPONSE, topResp);

		try {
			triggerReqListener(topReq, true);
			service(topReq, topResp);
		} finally {
			try {
				triggerReqListener(topReq, false);
			} finally {
				// Release wrapper buffer.
				// FIXME: It seems not need to buffer and use singleton factory.
				BundledHttpServletRequestWrapper.releaseRequestWrapper(topReq);
				HttpServletResponseWrapper.releaseResponseWrapper(topResp);
				// Clean the request thread linked variants at last.
				reqThreadVariants.remove();
			}
		}
	}

	@Override
	public void service(ServletRequest req, ServletResponse res)
			throws ServletException, IOException {
		if (!(req instanceof HttpServletRequest && res instanceof HttpServletResponse))
			throw new ServletException(
					"Not support none HttpServletRequest and HttpServletResponse.");

		boolean exceptionThrowed = false;
		try {
			// Construct a Handers Chain for the current request and trigger it.
			// The framework handler:
			// 1. RequestPathAdjustHandlder
			// 2. CustomizeFilterHandler
			// 3. CustomizeServletHandler
			IFwkHandlerChain handlerChain = helper.constructHandlerChain();
			handlerChain.start((BundledHttpServletRequestWrapper) req,
					(HttpServletResponseWrapper) res);
		} catch (Throwable t) {
			// t.printStackTrace();
			exceptionThrowed = true;
			String msg = t.getMessage();
			if (t instanceof SocketException
					|| (msg != null && msg.contains("java.net.SocketException"))) {
				// If SocketException, it will be emited by external server.
				// May be Socket be reset SocketException, skip, just log
				// message.
				FwkActivator.getInstance().log(t.getMessage(), 0, true);
			} else
				throw new ServletException("OSGiJEEContainer: Service error", t);
		} finally {
			HttpServletResponseWrapper topResp = (HttpServletResponseWrapper) this
					.getReqThreadVariants().get(THREAD_V_RESPONSE);
			BundledHttpServletRequestWrapper topRep = (BundledHttpServletRequestWrapper) this
					.getReqThreadVariants().get(THREAD_V_REQUEST);
			if (!exceptionThrowed) {
				try {
					topResp.flushBuffer();
				} catch (Exception e) {
					String msg = e.getMessage();
					// If http socket reset by client, a IOException will be
					// threw. Ignore it.
					if (e instanceof IOException
							|| (msg != null && msg
									.contains("java.net.SocketException"))) {
						// If SocketException, it will be emited by external
						// server.
						// May be Socket be reset SocketException, skip,
						// just log message.
						FwkActivator.getInstance().log(e.getMessage(), 0, true);
					}
				}
			}
		}
	}

	/**
	 * Trigger the ServletRequestListener event.
	 * 
	 * @param httpReq
	 * @param b
	 */
	private void triggerReqListener(
			final BundledHttpServletRequestWrapper httpReq,
			final boolean created) {
		IPath path = new Path(httpReq.getServletPath());
		IBundledServletContext curBundleServletCtx = null;
		if (path.segmentCount() > 1) {
			String nsPrefix = path.segment(0);
			curBundleServletCtx = getBundledServletContext(nsPrefix);
		}
		if (curBundleServletCtx == null)
			curBundleServletCtx = findHostServletContext();

		// Invoke Request listeners in current bundle context and modified
		// thread context class loader.
		if (curBundleServletCtx != null) {
			final IBundledServletContext ctx = curBundleServletCtx;
			httpReq.setBundle(ctx.getBundle());
			try {
				BundleContextRunnable runnable = new BundleContextRunnable(ctx) {
					@Override
					protected IStatus execute() {
						List<ServletRequestListener> listeners = helper
								.findListeners(ctx,
										ServletRequestListener.class);
						for (Iterator<ServletRequestListener> it = listeners
								.iterator(); it.hasNext();) {
							ServletRequestListener listener = it.next();
							if (created)
								listener
										.requestInitialized(new ServletRequestEvent(
												ctx, httpReq));
							else
								listener
										.requestDestroyed(new ServletRequestEvent(
												ctx, httpReq));
						}
						return Status.OK_STATUS;
					}
				};
				runnable.run();

			} finally {
				httpReq.setBundle(null);
			}
		}
	}

	public Set<IBundledServletContext> getAllBundledServletContext() {
		return bundleCtxs;
	}

	public IBundledServletContext findHostServletContext() {
		if (hostServletContext == null) {
			Set<IBundledServletContext> allCtxs = this
					.getAllBundledServletContext();
			for (IBundledServletContext ctx : allCtxs) {
				boolean isHostBundle = ctx.getBundleActivator()
						.isHostWebBundle();
				if (isHostBundle) {
					hostServletContext = ctx;
					break;
				}
			}
		}

		return hostServletContext;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.EMRYS.components.web.core.IOSGiWebContainer#getBundledServletContext
	 * (java.lang.String)
	 */
	public IBundledServletContext getBundledServletContext(String bundlePrefix) {
		Set<IBundledServletContext> list = this.getAllBundledServletContext();
		for (IBundledServletContext ctx : list) {
			if (ctx.getBundleActivator().getServiceNSPrefix().equals(
					bundlePrefix))
				return ctx;
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.osgi.framework.BundleListener#bundleChanged(org.osgi.framework.
	 * BundleEvent)
	 */
	public void bundleChanged(BundleEvent event) {
		// Only after the Fwk started, this listener will work.
		if (!activator.isFwkEarlyInitialized())
			return;

		int et = event.getType();
		Bundle bundle = event.getBundle();
		Object source = event.getSource();
		if (et == BundleEvent.STOPPED) {
			ComActivator activator = FwkRuntime.getInstance()
					.getBundleActivator(bundle.getBundleId());
			if (activator instanceof WebComActivator) {
				WebComActivator webActivator = (WebComActivator) activator;
				removeBundledServletContext(webActivator
						.getBundleServletContext());
				try {
					refresh();
					FwkActivator.getInstance().log(
							"Removed web bundle service: "
									+ webActivator.getServiceNS(), 0, false);
				} catch (Exception e) {
					// e.printStackTrace();
					FwkActivator.getInstance().log(e);
				}
			}
		}

		if (et == BundleEvent.STARTED) {
			ComActivator activator = FwkRuntime.getInstance()
					.getBundleActivator(bundle.getBundleId());
			if (activator instanceof WebComActivator) {
				WebComActivator webActivator = (WebComActivator) activator;
				if (!webActivator.isWebServiceStarted()) {
					try {
						webActivator.start2ndPeriod();
						refresh();
					} catch (Exception e) {
						// e.printStackTrace();
						FwkActivator.getInstance().log(e);
					}
				}
			}
		}
	}

	public void addFwkRequestHandler(IFwkRequestHandler handler) {
		reqHandlers.add(handler);
	}

	public OSGiJEEContainerHelper getHelper() {
		return helper;
	}
}
