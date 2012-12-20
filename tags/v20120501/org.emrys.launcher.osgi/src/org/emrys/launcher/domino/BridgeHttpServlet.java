package org.emrys.launcher.domino;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.emrys.core.adapter.internal.ServletContextAdapter;
import org.emrys.core.launcher.internal.DefaultBridgeHttpServlet;
import org.emrys.core.launcher.internal.FwkExternalAgent;
import org.emrys.core.launcher.internal.IFwkLauncher;

/**
 * Bridge Servlet to launcher OSGi Java EE Framework embedded in OSGi runtime.
 * 
 * @author LeoChang
 * 
 */
public class BridgeHttpServlet extends DefaultBridgeHttpServlet {

	private static final long serialVersionUID = -8563761086306691762L;
	private static final String BRIDGE_SERVLET_MAP_NAME = "p";
	private IFwkLauncher fwkLauncher;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		// For servlets in the default (root) context, this method returns ""
		// Adjust ServletContext Path. Super class has done the job when the
		// first request arrives in service() method. The following code can be
		// deleted.
		ServletContextAdapter fwkServletCtx = fwkAgent
				.getFwkServletContext(FwkExternalAgent.SERVLET_TYPE_HTTP);
		String originalCtxPath = fwkServletCtx.getContextPath();
		if (originalCtxPath != null && originalCtxPath.length() > 0) {
			fwkServletCtx.setContextPath(originalCtxPath + "/"
					+ BRIDGE_SERVLET_MAP_NAME);
		} else
			fwkServletCtx.setContextPath("/" + BRIDGE_SERVLET_MAP_NAME);
	}

	@Override
	public IFwkLauncher getFwkLauncher() {
		if (fwkLauncher == null)
			fwkLauncher = new EmbeddedOSGiFwkLauncher();
		return fwkLauncher;
	}

	@Override
	public String getServletInfo() {
		return super.getServletInfo()
				+ "/Bridge Servlet for OSGi JavaEE Container 1.0.0";
	}

	@Override
	protected String getSpecifiedContextPath() {
		// Specify the framework servlet context path.
		return "/" + BRIDGE_SERVLET_MAP_NAME;
	}
}
