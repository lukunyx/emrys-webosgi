/**
 * 
 */
package org.emrys.webosgi.core.handlers;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.emrys.webosgi.core.IFwkConstants;
import org.emrys.webosgi.core.jeeres.ClonedExecutableServletObject;
import org.emrys.webosgi.core.jeeres.FilterDelegate;
import org.emrys.webosgi.core.jeewrappers.BundledHttpServletRequestWrapper;
import org.emrys.webosgi.core.jeewrappers.HttpServletResponseWrapper;
import org.emrys.webosgi.core.runtime.OSGiWebContainer;
import org.emrys.webosgi.core.service.IWABServletContext;
import org.osgi.framework.Bundle;


/**
 * @author LeoChang
 * 
 */
public class CustomizeFiltersHandler extends AbstractFwkReqeustHandler
		implements IFwkRequestHandler {
	public static final int PRIORITY = 100;

	public CustomizeFiltersHandler(OSGiWebContainer fwkContainer) {
		super(fwkContainer);
	}

	public void handle(BundledHttpServletRequestWrapper request,
			HttpServletResponseWrapper response, IFwkHandlerChain handlerChain)
			throws IOException, ServletException {

		// Do standard servlet filters.
		doUrlMapFilter(request, response, handlerChain);

		// Not need invoke parent handler chain at last. the do Url map method
		// will do so at the end of its filter chain.
		// handlerChain.handle(request, response);
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
		// If the request is dispatched type, some filters maybe handled in last
		// request process, here need to clear the possible executed mark.
		if (req.isDispatched()) {
			for (ClonedExecutableServletObject<FilterDelegate> o : filters)
				o.refresh();
		}

		final Iterator<ClonedExecutableServletObject<FilterDelegate>> it = filters
				.iterator();
		// Construct the Framework FilterChain.
		final FilterChain filterChain = new FilterChain() {
			private String lastTopServletPath;
			private String lastFilterServletPath;
			private String lastWabCtxPath;

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

			private void switchReqPath(IWABServletContext ctx) {
				String wabCtxPath = ctx.getWABContextPath();
				lastTopServletPath = topReq.getServletPath();
				if (lastTopServletPath.startsWith(wabCtxPath)) {
					lastWabCtxPath = wabCtxPath;
					topReq.setServletPath(lastTopServletPath.replaceFirst(
							wabCtxPath, ""));
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
				// And if the target url not defined in the bundle of former
				// filter defined, the lastWabCtxPath is null. Here we
				// check it. FIXME: Here maybe need allow cross bundle url
				// change by request wrapper. We can check special prefix of
				// servlet path and skip this process. The prefix:
				// IFwkConstants.SYS_PATH_PREFIX before
				// the servlet path indicats that this is system root base path.
				// We have do this for all Url pattern of servlet or filter when
				// parsing web.xml.
				String thisFilterServletPath = httpReq.getServletPath();
				boolean isReqPathSysRootBased = thisFilterServletPath
						.startsWith(IFwkConstants.SYS_PATH_PREFIX);

				if (!isReqPathSysRootBased && lastFilterServletPath != null
						&& lastWabCtxPath != null
						&& !thisFilterServletPath.startsWith(lastWabCtxPath)) {
					// Use old bundle not change the bundle context.
					request = BundledHttpServletRequestWrapper
							.getHttpServletRequestWrapper(request, oldBundle);
					BundledHttpServletRequestWrapper bundledReqWrapper = ((BundledHttpServletRequestWrapper) request);
					thisFilterServletPath = lastWabCtxPath
							+ thisFilterServletPath;
					bundledReqWrapper.setServletPath(thisFilterServletPath);
				}

				if (isReqPathSysRootBased) {
					// If last filter switched the request path to system root
					// based, wrapper the req again and remove the system root
					// prefix.
					request = BundledHttpServletRequestWrapper
							.getHttpServletRequestWrapper(request, oldBundle);
					BundledHttpServletRequestWrapper bundledReqWrapper = ((BundledHttpServletRequestWrapper) request);
					thisFilterServletPath = thisFilterServletPath
							.substring(IFwkConstants.SYS_PATH_PREFIX.length());
					bundledReqWrapper.setServletPath(thisFilterServletPath);
				}

				lastWabCtxPath = null;
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

				// Notes: decide the url pattern according to the dispatchers
				// set if any. If dispatcher type check failed, a null url
				// pattern returned. So do the next while iterate.
				urlPattern = decideUrlPattern(req, urlPattern);

				while (clonedFilterInstance != null
						&& (clonedFilterInstance.isExecuted() || !(!StringUtils
								.isEmpty(urlPattern) && fwkContainerHelper
								.checkNeedFilter((HttpServletRequest) request,
										urlPattern)))) {
					if (it.hasNext()) {
						clonedFilterInstance = it.next();
						filterDelegate = clonedFilterInstance.getOriginalObj();
						urlPattern = filterDelegate.getURLPatterns()[clonedFilterInstance
								.getId()];
						urlPattern = decideUrlPattern(req, urlPattern);
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

			private String decideUrlPattern(
					BundledHttpServletRequestWrapper req, String urlPattern) {
				String dispatchers = null;
				boolean isDispatched = req.isDispatched();
				boolean isInclude = req.isInclude();
				// Check if this url pattern has prefix( Dipatchers type).
				int index = urlPattern
						.indexOf(FilterDelegate.DISPATCHERS_PARTTERN_SEPERATOR);
				if (index > -1) {
					dispatchers = urlPattern.substring(0, index);
					urlPattern = urlPattern.substring(index + 1);

					if (!isDispatched
							&& dispatchers
									.contains(FilterDelegate.DISPATCHERS.REQUEST
											.name()))
						return urlPattern;
					if (isDispatched
							&& isInclude
							&& dispatchers
									.contains(FilterDelegate.DISPATCHERS.INCLUDE
											.name()))
						return urlPattern;

					if (isDispatched
							&& !isInclude
							&& dispatchers
									.contains(FilterDelegate.DISPATCHERS.FORWARD
											.name()))
						return urlPattern;
				} else {
					if (!isDispatched)
						return urlPattern;
				}
				return null;
			}
		};

		// Trigger the filters chain.
		filterChain.doFilter(req, resp);
	}
}
