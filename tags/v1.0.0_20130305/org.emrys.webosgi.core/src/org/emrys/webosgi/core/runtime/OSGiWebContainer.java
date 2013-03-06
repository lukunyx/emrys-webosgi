package org.emrys.webosgi.core.runtime;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.GenericServlet;
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

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.emrys.webosgi.core.FwkActivator;
import org.emrys.webosgi.core.handlers.IFwkHandlerChain;
import org.emrys.webosgi.core.handlers.IFwkRequestHandler;
import org.emrys.webosgi.core.internal.FwkRuntime;
import org.emrys.webosgi.core.jeeres.FilterDelegate;
import org.emrys.webosgi.core.jeeres.ServletDelegate;
import org.emrys.webosgi.core.jeewrappers.BundledHttpServletRequestWrapper;
import org.emrys.webosgi.core.jeewrappers.HttpServletResponseWrapper;
import org.emrys.webosgi.core.jeewrappers.ServletContextWrapper;
import org.emrys.webosgi.core.service.IOSGiWebContainer;
import org.emrys.webosgi.core.service.IWABServletContext;
import org.emrys.webosgi.core.util.NamedThreadLocal;
import org.emrys.webosgi.launcher.internal.IFwkEnvConstants;
import org.emrys.webosgi.launcher.internal.adapter.HttpServletRequestAdapter;
import org.emrys.webosgi.launcher.internal.adapter.HttpServletResponseAdapter;
import org.osgi.framework.Bundle;

/**
 * OSGi JEE runtime container. The original ServletRequest from server will be
 * transformed from BridgeServiet to this serlvet by java reflect method
 * invoking
 * 
 * <code>serviet()<code> method. And then, the original SerlvetRequest will be wrapered back to javax.servlet.SerlvetRequest, and be dispatched to mapping servlet provided from Web Bundles.
 * 
 * @author Leo Chang
 * @since 2011-3-22
 */
public class OSGiWebContainer extends GenericServlet implements
		IOSGiWebContainer {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8570607384054854413L;

	/**
	 * Thread local variants for each Request thread. See
	 * {@link #getReqThreadVariants()} to get or put variants.
	 */
	private static final NamedThreadLocal<Map<String, Object>> reqThreadVariants = new NamedThreadLocal<Map<String, Object>>(
			OSGiWebContainer.class.getName()) {
		@Override
		protected Map<String, Object> initialValue() {
			// initialize variants.
			return new HashMap<String, Object>();
		}
	};

	/**
	 * helper including many convenient methods intend to make code clear.
	 */
	protected OSGiWebContainerHelper helper;
	/**
	 * BundledServletContext buffer.
	 */
	protected Set<IWABServletContext> bundleCtxs;
	protected final Set<IFwkRequestHandler> reqHandlers;
	/**
	 * Host web bundle's ServletContext
	 */
	private IWABServletContext hostServletContext;
	private boolean containerInited;

	private final AtomicBoolean needRefreshHandlerChain = new AtomicBoolean();

	public OSGiWebContainer() {
		bundleCtxs = new HashSet<IWABServletContext>();
		reqHandlers = new HashSet<IFwkRequestHandler>();
	}

	@Override
	public String getServletInfo() {
		return "OSGi Web Container[version: 1.0.0 by Leo Chang]";
	}

	@Override
	public String getServletName() {
		return "OSGiWebContainer";
	}

	@Override
	public ServletConfig getServletConfig() {
		return this;
	}

	@Override
	public String getInitParameter(String name) {
		// FIXME: Some global parameter maybe better.
		return null;
	}

	@Override
	public Enumeration getInitParameterNames() {
		return null;
	}

	public boolean isInitialized() {
		return containerInited;
	}

	@Override
	public synchronized void init() throws ServletException {
		// Init web container in lazy mode.
		// JavaEE container should be initialized here when the first request
		// comes by framework. In constructor method of this class there is risk
		// that some global resource not available, such as framework web
		// context path.
		if (containerInited)
			return;
		try {
			// Fetch the host addresses(IP or host names) of the framework
			// server, and the set the recommended one as first.
			collectHostAddresses();
			helper = new OSGiWebContainerHelper(this);
			// Some wabs maybe configured early start once framework started. Do
			// initialize, deploy and started for them.
			FwkRuntime.getInstance().initEarlyStartWabs();
			helper.refresh();
			containerInited = true;
		} catch (Exception e) {
			if (e instanceof ServletException)
				throw (ServletException) e;
			throw new ServletException(e);
		}
	}

	private void collectHostAddresses() throws Exception {
		// Fetch the host addresses(IPs) of the framework server, and the set
		// the recommended one as first.
		FwkRuntime fwkRuntime = FwkRuntime.getInstance();
		List<String> hostAddrs = new ArrayList<String>();
		// This default host addr should be a String type, but we do a type
		// check.
		Object defaultHostAddr = fwkRuntime
				.getFrameworkAttribute(IFwkEnvConstants.ATTR_WEB_APP_HOST);
		if (defaultHostAddr instanceof String
				&& !"localhost".equals(defaultHostAddr)
				&& !"127.0.0.1".equals(defaultHostAddr)) {
			// Set the default host address to the first recommened one.
			hostAddrs.add((String) defaultHostAddr);
		}

		// Recursive for all Internet Protocol address(IP).
		String hostName = InetAddress.getLocalHost().getHostName();
		InetAddress[] addressese = InetAddress.getAllByName(hostName);
		for (InetAddress ad : addressese) {
			String addr = ad.getHostAddress();
			if (!hostAddrs.contains(addr))
				hostAddrs.add(addr);
		}

		fwkRuntime.setFrameworkAttribute(ATTR_WEB_APP_HOSTS, hostAddrs);
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
		if (isInitialized())
			helper.refresh();
	}

	public void activeServletContext(IWABServletContext ctx) throws Exception {
		if (ctx.isActive())
			return;
		ctx.setActive(true);
		trigerContextEvent(ctx, true);
	}

	public void regServletContext(IWABServletContext ctx) throws Exception {
		if (ctx != null) {
			synchronized (bundleCtxs) {
				bundleCtxs.add(ctx);
			}
			// If this servelt context is set active before register, do context
			// created event dispatch then.
			if (ctx.isActive())
				trigerContextEvent(ctx, true);
		}
	}

	public void unregServletContext(IWABServletContext ctx) {
		if (bundleCtxs != null) {
			boolean ok = false;
			synchronized (bundleCtxs) {
				ctx.setActive(false);
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
			final IWABServletContext ctx, final int et) {
		BundleContextRunnable runnable = new BundleContextRunnable(ctx) {
			@Override
			protected IStatus execute() {
				List<ServletContextAttributeListener> listeners = helper
						.findListeners(ctx,
								ServletContextAttributeListener.class);
				for (ServletContextAttributeListener l : listeners) {
					switch (et) {
					case IWABServletContext.ET_ATTR_ADDED: {
						l.attributeAdded(scab);
						break;
					}
					case IWABServletContext.ET_ATTR_REMOVED: {
						l.attributeRemoved(scab);
						break;
					}
					case IWABServletContext.ET_ATTR_REPLACED: {
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
	private void trigerContextEvent(final IWABServletContext ctx,
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

		getReqThreadVariants().put(OSGiWebContainer.THREAD_V_REQUEST, topReq);
		getReqThreadVariants().put(OSGiWebContainer.THREAD_V_RESPONSE, topResp);

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
			IFwkHandlerChain handlerChain = helper
					.constructHandlerChain(needRefreshHandlerChain
							.getAndSet(false));
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
			} else {
				FwkActivator.getInstance().log(t);
				if (!helper.forwardToErrPage(req, res, t))
					throw new ServletException(
							"OSGiWebContainer: Service error", t);
			}
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
		IWABServletContext curBundleServletCtx = null;
		// NOTE: In case /p?abc, the servlet path is null.
		String servletPath = httpReq.getServletPath();
		if (!StringUtils.isEmpty(servletPath)) {
			IPath path = new Path(servletPath);
			if (path.segmentCount() > 1) {
				String nsPrefix = path.segment(0);
				curBundleServletCtx = getBundledServletContext(nsPrefix);
			}
		}
		if (curBundleServletCtx == null)
			curBundleServletCtx = findHostServletContext();

		// Invoke Request listeners in current bundle context and modified
		// thread context class loader.
		if (curBundleServletCtx != null) {
			final IWABServletContext ctx = curBundleServletCtx;
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

	public Set<IWABServletContext> getAllBundledServletContext() {
		return bundleCtxs;
	}

	public IWABServletContext findHostServletContext() {
		if (hostServletContext == null) {
			Set<IWABServletContext> allCtxs = this
					.getAllBundledServletContext();
			for (IWABServletContext ctx : allCtxs) {
				boolean isHostBundle = ctx.isHostBundle();
				if (isHostBundle) {
					hostServletContext = ctx;
					break;
				}
			}
		}

		return hostServletContext;
	}

	public ServletContext findServletContext(Bundle bundle) {
		if (bundleCtxs == null || bundle == null) {
			return this.getServletContext();
		}
		synchronized (bundleCtxs) {
			for (IWABServletContext ctx : bundleCtxs) {
				if (ctx.getBundle().equals(bundle))
					return ctx;
			}
		}
		return null;
	}

	public IWABServletContext getWABServletContext(Bundle bundle) {
		Set<IWABServletContext> list = this.getAllBundledServletContext();
		for (IWABServletContext ctx : list) {
			if (ctx.getBundle() == bundle)
				return ctx;
		}

		return null;
	}

	public IWABServletContext getWABServletContext(String wabCtxPath) {
		Set<IWABServletContext> list = this.getAllBundledServletContext();
		for (IWABServletContext ctx : list) {
			if (ctx.getWABContextPath().equals(wabCtxPath))
				return ctx;
		}

		return null;
	}

	public IWABServletContext getBundledServletContext(String bundlePrefix) {
		// Note: In some old web bundle, the wabCtxPath argument not started
		// with a slash "/", to support this situation, we add a check here.
		Set<IWABServletContext> list = this.getAllBundledServletContext();
		for (IWABServletContext ctx : list) {
			if (ctx.getWABContextPath().equals(
					bundlePrefix.startsWith("/") ? bundlePrefix
							: ("/" + bundlePrefix)))
				return ctx;
		}

		return null;
	}

	public void addFwkRequestHandler(IFwkRequestHandler handler) {
		needRefreshHandlerChain.set(reqHandlers.add(handler));
	}

	public void unregisterFwkRequestHandler(IFwkRequestHandler handler) {
		reqHandlers.remove(handler);
	}

	public OSGiWebContainerHelper getHelper() {
		return helper;
	}
}
