package org.emrys.webosgi.core.runtime;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.emrys.webosgi.core.IFwkConstants;
import org.emrys.webosgi.core.internal.FwkRuntime;
import org.emrys.webosgi.core.jeewrappers.BundledHttpServletRequestWrapper;
import org.emrys.webosgi.core.jeewrappers.HttpServletResponseWrapper;
import org.emrys.webosgi.core.service.IOSGiWebContainer;

/**
 * The Bundled Request Dispatcher.
 * 
 * @author Leo Chang
 * @version 2011-3-31
 */
public class BundledRequestDispatcher implements RequestDispatcher {
	private final String webCtxPath;
	private final IOSGiWebContainer jeeContainerSVC;
	private String targetPath;
	private Object include_context_path;
	private Object include_servlet_path;
	private Object include_path_info;
	private Object include_request_uri;
	private Object include_query_string;
	private boolean doInternalFilter;

	public BundledRequestDispatcher(String wabCtxPath, String path) {
		this.webCtxPath = wabCtxPath;
		this.targetPath = path;
		jeeContainerSVC = FwkRuntime.getInstance().getWebContainer();
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
		 * javax.servlet.include.servlet_path = ServletPath
		 * javax.servlet.include.path_info = PathInfo
		 * javax.servlet.include.query_string = QueryString
		 * javax.servlet.include.request_uri = RequestURI
		 */
		service(request, response, true);
	}

	/**
	 * Serve the reqeust, wiring back to the method. Forward and include on this
	 * method, will not trigger the servlet filters.
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
				.getReqThreadVariants().get(OSGiWebContainer.THREAD_V_REQUEST);

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
			// Skip on system root path mark.
			if (targetPath.startsWith(IFwkConstants.SYS_PATH_PREFIX)) {
				targetPath = targetPath.substring(IFwkConstants.SYS_PATH_PREFIX
						.length());
			} else {
				if (!targetPath.startsWith("/")) {
					// Relative path not allowed, regard it as absolute.
					targetPath = "/" + targetPath;
				}

				if (this.webCtxPath != null && this.webCtxPath.length() != 0
						&& !targetPath.startsWith("/" + this.webCtxPath + "/")) {
					targetPath = this.webCtxPath + targetPath;
				}
			}

			// Take the URI and query string together as targetURL, and later
			// set into request.
			String targetURL = targetPath;

			int index = targetPath.indexOf('?');
			if (index != -1) {
				String queryStr = targetPath.substring(index + 1);
				targetPath = targetPath.substring(0, index);
				reqWrapper.parseNewParameters(queryStr);
			}

			// set new path to dispatch to.
			reqWrapper.setServletPath(targetPath);
			// According to the tomcat's behavior, if forward, the request's URI
			// should be reset.
			if (!isInclude) {
				String oReqURL = reqWrapper.getRequestURL().toString();
				String ctxPath = reqWrapper.getContextPath();
				// Get the external top context path.
				int i = ctxPath.lastIndexOf('/');
				if (i > 0)
					ctxPath = ctxPath.substring(0, i);
				String newReqURI = ctxPath + targetPath;
				reqWrapper.setRequestURI(newReqURI);
				i = oReqURL.indexOf(ctxPath);
				if (i > -1) {
					String urlPrefix = oReqURL.substring(0, i
							+ ctxPath.length());
					String newReqURL = urlPrefix + targetURL;
					reqWrapper.setRequestURL(newReqURL);
				}
			}

			reqWrapper.setPathInfo(null);

			// If need do filter, not set dispatching ad include mark.
			if (!doInternalFilter) {
				// If forward, we just reset the response's output buffer, not
				// reset the headers and status. So we invoke resetBuffer(), not
				// reset(); Notice, before dispatch this request, some filter or
				// servlet may add some cookie or set status. Tomcat remain them
				// after dispatch.
				if (!isInclude)
					respWrapper.resetBuffer(); // reset();
				// set the dispatching mark.
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
