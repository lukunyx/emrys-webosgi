/**
 * 
 */
package org.emrys.core.runtime.handlers;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.emrys.core.runtime.jeecontainer.IBundledServletContext;
import org.emrys.core.runtime.jeecontainer.OSGiJEEContainer;
import org.emrys.core.runtime.jeeres.ClonedExecutableServletObject;
import org.emrys.core.runtime.jeeres.FilterDelegate;
import org.emrys.core.runtime.jeewrappers.BundledHttpServletRequestWrapper;
import org.emrys.core.runtime.jeewrappers.HttpServletResponseWrapper;
import org.osgi.framework.Bundle;


/**
 * @author LeoChang
 * 
 */
public class CustomizeFiltersHandler extends AbstractFwkReqeustHandler
		implements IFwkRequestHandler {
	public static final int PRIORITY = 100;

	public CustomizeFiltersHandler(OSGiJEEContainer fwkContainer) {
		super(fwkContainer);
	}

	public void handle(BundledHttpServletRequestWrapper request,
			HttpServletResponseWrapper response, IFwkHandlerChain handlerChain)
			throws IOException, ServletException {
		boolean isDispatched = request.isDispatched();
		// If the request is dispatched, skip customize servlet filters
		// according to Java EE standard.
		if (!isDispatched) {
			// Do standard servlet filters.
			doUrlMapFilter(request, response, handlerChain);
		} else
			handlerChain.handle(request, response);
	}

	@Override
	public int getPriority() {
		return PRIORITY;
	}

	private void doUrlMapFilter(final BundledHttpServletRequestWrapper req,
			HttpServletResponseWrapper resp, final IFwkHandlerChain handlerChain)
			throws IOException, ServletException {
		// Get splited Filters by their multiple URL mapping patterns.
		List<ClonedExecutableServletObject<FilterDelegate>> filters = fwkContainerHelper
				.sortURLPatternsExeObjs(FilterDelegate.class, false);
		final Iterator<ClonedExecutableServletObject<FilterDelegate>> it = filters
				.iterator();
		// Construct the Framework FilterChain.
		final FilterChain filterChain = new FilterChain() {
			private String lastTopServletPath;
			private String lastFilterServletPath;
			private String lastFilterBundlePrefix;

			private final BundledHttpServletRequestWrapper topReq = req
					.getTopWrapper();

			public void doFilter(final ServletRequest request,
					final ServletResponse response) throws IOException,
					ServletException {
				// Revert former request servlet path if buffered in last filter
				// invoke.
				revertReqPath();
				doFilterInternal(request, response);
			}

			private void revertReqPath() {
				if (lastTopServletPath != null) {
					topReq.setServletPath(lastTopServletPath);
					lastTopServletPath = null;
				}
			}

			private void switchReqPath(IBundledServletContext ctx) {
				String bundlePrefix = ctx.getBundleActivator()
						.getServiceNSPrefix();
				lastTopServletPath = topReq.getServletPath();
				if (lastTopServletPath.startsWith("/" + bundlePrefix)) {
					lastFilterBundlePrefix = bundlePrefix;
					topReq.setServletPath(lastTopServletPath.replaceFirst("/"
							+ bundlePrefix, ""));
				}
			}

			private void doFilterInternal(ServletRequest request,
					ServletResponse response) throws IOException,
					ServletException {

				if (!(request instanceof HttpServletRequest)
						|| !(response instanceof HttpServletResponse))
					return;

				Bundle oldBundle = topReq.getBundle();
				HttpServletRequest httpReq = (HttpServletRequest) request;

				// The former filter may wrapped th req and modified the servlet
				// path, and the former bundle prefix maybe removed by mistake.
				// Here, we need to rewrap it to insert its bundle prefix
				// before the path.
				String thisFilterServletPath = httpReq.getServletPath();
				if (lastFilterServletPath != null
						&& !thisFilterServletPath.startsWith("/"
								+ lastFilterBundlePrefix)) {
					// Use old bundle not change the bundle context.
					request = BundledHttpServletRequestWrapper
							.getHttpServletRequestWrapper(request, oldBundle);
					BundledHttpServletRequestWrapper bundledReqWrapper = ((BundledHttpServletRequestWrapper) request);
					thisFilterServletPath = "/" + lastFilterBundlePrefix
							+ thisFilterServletPath;
					bundledReqWrapper.setServletPath(thisFilterServletPath);
					lastFilterBundlePrefix = null;
				}

				// Record this filter's servlet path for next use.
				lastFilterServletPath = thisFilterServletPath;

				// If filter chain empty, do servlet invoke if needed.
				if (!it.hasNext()) {
					// The customize's Filter may wrapper our Req or Response
					// again, here wrapper back if any need.
					BundledHttpServletRequestWrapper httpReqWrapper = BundledHttpServletRequestWrapper
							.getHttpServletRequestWrapper(request, oldBundle);
					HttpServletResponseWrapper httpRespWrapper = HttpServletResponseWrapper
							.getHttpServletResponseWrapper(response);
					handlerChain.handle(httpReqWrapper, httpRespWrapper);
					return;
				}

				// Iterate to do filter matching the current request servlet
				// path. Remove bundle's web prefix from servlet path
				// temporarily before filter invoke.
				ClonedExecutableServletObject<FilterDelegate> clonedFilterInstance = it
						.next();
				FilterDelegate filterDelegate = clonedFilterInstance
						.getOriginalObj();
				String urlPattern = filterDelegate.getURLPatterns()[clonedFilterInstance
						.getId()];

				while (clonedFilterInstance != null
						&& (clonedFilterInstance.isExecuted() || !(urlPattern != null
								&& urlPattern.length() > 0 && fwkContainerHelper
								.checkNeedFilter((HttpServletRequest) request,
										urlPattern)))) {
					if (it.hasNext()) {
						clonedFilterInstance = it.next();
						filterDelegate = clonedFilterInstance.getOriginalObj();
						urlPattern = filterDelegate.getURLPatterns()[clonedFilterInstance
								.getId()];
					} else {
						clonedFilterInstance = null;
					}
				}

				// If any filter matched in the chain, do it.
				if (clonedFilterInstance != null) {

					try {
						// Switch request's current bundle to the filter's
						// bundle. And modify the servlet path of the request
						// delivering to the filter.
						fwkContainerHelper
								.switchReqBundleContext(filterDelegate
										.getBundleContext().getBundle());
						switchReqPath(filterDelegate.getBundleContext());
						// set the executed mark early.
						clonedFilterInstance.setExecuted();
						filterDelegate.doFilter(request, response, this);
					} finally {
						// revert to the former bundle.
						clonedFilterInstance.refresh();
						// revertReqPath(); // no need to revert req path at
						// last for it is not be used later.
						fwkContainerHelper.switchReqBundleContext(oldBundle);
					}
				} else if (!it.hasNext()) {
					BundledHttpServletRequestWrapper httpReqWrapper = BundledHttpServletRequestWrapper
							.getHttpServletRequestWrapper(request, oldBundle);
					HttpServletResponseWrapper httpRespWrapper = HttpServletResponseWrapper
							.getHttpServletResponseWrapper(response);
					handlerChain.handle(httpReqWrapper, httpRespWrapper);
				}
			}
		};

		// Trigger the filters chain.
		filterChain.doFilter(req, resp);
	}
}
