/**
 * 
 */
package org.emrys.webosgi.core.handlers;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.emrys.webosgi.core.jeeres.ClonedExecutableServletObject;
import org.emrys.webosgi.core.jeeres.FilterDelegate;
import org.emrys.webosgi.core.jeeres.ServletDelegate;
import org.emrys.webosgi.core.jeewrappers.BundledHttpServletRequestWrapper;
import org.emrys.webosgi.core.jeewrappers.HttpServletResponseWrapper;
import org.emrys.webosgi.core.runtime.OSGiWebContainer;
import org.osgi.framework.Bundle;


/**
 * @author LeoChang
 * 
 */
public class CustomizeServletHandler extends AbstractFwkReqeustHandler
		implements IFwkRequestHandler {

	public static final int PRIORITY = 200;

	public CustomizeServletHandler(OSGiWebContainer fwkContainer) {
		super(fwkContainer);
	}

	public void handle(BundledHttpServletRequestWrapper request,
			HttpServletResponseWrapper response, IFwkHandlerChain handlerChain)
			throws IOException, ServletException {
		// Record the old path before servlet invoke, and reset to the req at
		// last. Otherwise, the filter will not be matched to the modified path.
		BundledHttpServletRequestWrapper topRequest = request.getTopWrapper();
		// HttpServletResponseWrapper topResponse = response.getTopWrapper();
		String oldPathInfo = topRequest.getPathInfo();
		String oldServletPath = topRequest.getServletPath();

		try {
			// Because a servlet may have mutiple url pattern map. Here
			// create mutiple cloned copy of a servlet for each url
			// parttern. And then, sort these copys of many servlets
			// according to some regular into a wait queue. If one copy be
			// executed, others of the same servlet or filter won't execute.
			Collection<FilterDelegate> filters = getFwkContainerHelper()
					.getAllBufferedFilters(false);
			List<ClonedExecutableServletObject<ServletDelegate>> servletCopys = getFwkContainerHelper()
					.sortURLPatternsExeObjs(ServletDelegate.class, false);

			// Try to find the url matched servlet copy below.
			ClonedExecutableServletObject<ServletDelegate> servletDelegateCopy = null;
			try {
				servletDelegateCopy = getFwkContainerHelper()
						.chooseDelegateServlet(request, servletCopys);
			} catch (Exception e) {
				throw new ServletException(e);
			}

			if (servletDelegateCopy != null) {
				ServletDelegate servletDelegate = servletDelegateCopy
						.getOriginalObj();
				Object oldBundle = topRequest.getBundle();
				// Only need to set once, because the filter of same bundle
				// with the servlet can be called.
				getFwkContainerHelper().switchReqBundleContext(
						servletDelegate.getBundleContext().getBundle());
				try {
					doServletMapFilter(servletDelegate, request, response,
							filters);
				} finally {
					// Revert bundle of Top Req
					getFwkContainerHelper().switchReqBundleContext(
							(Bundle) oldBundle);
				}
			} else {
				// No matched servlet found, set NOT_FOUND status rather than
				// throw ServletException.
				/*throw new ServletException("No Service found for Request URI:"
						+ request.getRequestURI());*/
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
			}
		} finally {
			// Reset the pathinfo and servlet path to the buffered request.
			// Because serlvet mapping will modify these two variants, and then
			// the web bundle's flters will not be maped after the servlet
			// invoked.
			topRequest.setPathInfo(oldPathInfo);
			topRequest.setServletPath(oldServletPath);
		}
	}

	/**
	 * Do servlet mapped filter before the target servlet be invoked.
	 * 
	 * @param delegate
	 * @param request
	 * @param response
	 * @param filters
	 * @throws IOException
	 * @throws ServletException
	 */
	private void doServletMapFilter(final ServletDelegate delegate,
			final BundledHttpServletRequestWrapper request,
			HttpServletResponseWrapper response,
			Collection<FilterDelegate> filters) throws IOException,
			ServletException {

		final String servletName = delegate.name;
		final Iterator<FilterDelegate> it = filters.iterator();

		final FilterChain chain = new FilterChain() {

			public void doFilter(final ServletRequest request,
					final ServletResponse response) throws IOException,
					ServletException {
				if (!it.hasNext()) {
					delegate.service(request, response);
					return;
				}

				FilterDelegate i = it.next();
				while (i != null
						&& !(i.isSameBundled(delegate)
								&& i.targetServletNames != null && (checkMatched(
								i, servletName) || checkMatched(i,
								delegate.className)))) {
					if (it.hasNext())
						i = it.next();
					else {
						i = null;
					}
				}
				if (i != null) {
					i.doFilter(request, response, this);
				} else if (!it.hasNext()) {
					delegate.service(request, response);
				}
			}

			private boolean checkMatched(FilterDelegate i, String tarServletName) {
				if (StringUtils.isEmpty(tarServletName))
					return false;

				boolean isDispatched = request.isDispatched();
				String tarServletMapStr = i.targetServletNames;
				// If servlet map not contains dispatchers config, do simple
				// check. Only when REQUEST req allowed.
				if (tarServletMapStr
						.indexOf(FilterDelegate.DISPATCHERS_PARTTERN_SEPERATOR) == -1)
					return !isDispatched
							&& tarServletMapStr.contains(tarServletName);

				boolean isIncluded = request.isInclude();
				String[] tarServletMapSegs = tarServletMapStr
						.split(FilterDelegate.MULTI_MAP_SEG_SEPERATOR);
				for (String seg : tarServletMapSegs) {
					int index = seg
							.indexOf(FilterDelegate.DISPATCHERS_PARTTERN_SEPERATOR);
					if (index == -1 && !isDispatched
							&& seg.contains(tarServletName))
						return true;

					if (index > -1) {
						if (seg.contains(tarServletName)) {
							String dispatchers = seg.substring(0, index);
							if (!isDispatched
									&& dispatchers
											.contains(FilterDelegate.DISPATCHERS.REQUEST
													.name()))
								return true;
							if (isDispatched
									&& !isIncluded
									&& dispatchers
											.contains(FilterDelegate.DISPATCHERS.FORWARD
													.name()))
								return true;
							if (isDispatched
									&& isIncluded
									&& dispatchers
											.contains(FilterDelegate.DISPATCHERS.INCLUDE
													.name()))
								return true;
						}
					}
				}
				return false;
			}
		};

		chain.doFilter(request, response);
	}

	@Override
	public int getPriority() {
		return PRIORITY;
	}
}
