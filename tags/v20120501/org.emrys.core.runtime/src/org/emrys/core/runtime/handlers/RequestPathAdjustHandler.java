/**
 * 
 */
package org.emrys.core.runtime.handlers;

import java.io.IOException;

import javax.servlet.ServletException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.emrys.core.runtime.jeecontainer.IBundledServletContext;
import org.emrys.core.runtime.jeecontainer.OSGiJEEContainer;
import org.emrys.core.runtime.jeewrappers.BundledHttpServletRequestWrapper;
import org.emrys.core.runtime.jeewrappers.HttpServletResponseWrapper;


/**
 * @author LeoChang
 * 
 */
public class RequestPathAdjustHandler extends AbstractFwkReqeustHandler
		implements IFwkRequestHandler {

	public static final int PRIORITY = 10;

	public RequestPathAdjustHandler(OSGiJEEContainer fwkContainer) {
		super(fwkContainer);
	}

	public void handle(BundledHttpServletRequestWrapper request,
			HttpServletResponseWrapper response, IFwkHandlerChain handlerChain)
			throws IOException, ServletException {
		if (!request.isDispatched() && !adjustRequestPath(request, response))
			return;
		else
			handlerChain.handle(request, response);
	}

	/**
	 * If found a resource directory matched path, append a "/" at the end of
	 * original request path and send redirect status back. If found a existing
	 * welcome page, modify the servelt path internally and continue the
	 * handlers chain.
	 * 
	 * @param request
	 * @param response
	 * @return whether the handlers chain need to invoked continually.
	 * @throws IOException
	 */
	private boolean adjustRequestPath(BundledHttpServletRequestWrapper request,
			HttpServletResponseWrapper response) throws IOException {

		IPath path = new Path(request.getServletPath());
		String welcomePagePath = null;
		IBundledServletContext hostServletCtx = getFwkContainer()
				.findHostServletContext();
		// Try to process under host bundle servlet context at first.
		if (hostServletCtx != null) {
			// Adjust a resource directory path suffixed with "/" if need.
			if (!request.getServletPath().endsWith("/")
					&& getFwkContainerHelper().checkIsResourceDir(
							hostServletCtx, path)) {
				response.sendRedirect(request.getRequestURI() + "/");
				return false;
			}

			welcomePagePath = getFwkContainerHelper().tryRedirectToWelcomPage(
					hostServletCtx, path);
			if (welcomePagePath != null)
				request.setServletPath(path.append(welcomePagePath)
						.toPortableString());
		}

		// Then find a target bundle context.
		if (welcomePagePath == null && path.segmentCount() > 0) {
			IBundledServletContext tarBundleServletCtx = getFwkContainer()
					.getBundledServletContext(path.segment(0));
			if (tarBundleServletCtx != null) {
				// Adjust a resource directory path suffixed with "/" if need.
				if (!request.getServletPath().endsWith("/")
						&& getFwkContainerHelper().checkIsResourceDir(
								tarBundleServletCtx,
								path.removeFirstSegments(1))) {
					response.sendRedirect(request.getRequestURI() + "/");
					return false;
				}
				// Remove the target bundle prefix from the path and then try to
				// redirect to welcome page if exists.
				welcomePagePath = getFwkContainerHelper()
						.tryRedirectToWelcomPage(tarBundleServletCtx,
								path.removeFirstSegments(1));
				if (welcomePagePath != null)
					request.setServletPath(path.append(welcomePagePath)
							.toPortableString());
			}
		}

		return true;
	}

	@Override
	public int getPriority() {
		return PRIORITY;
	}
}
