package org.emrys.webosgi.launcher.osgi;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.emrys.webosgi.launcher.internal.DefaultBridgeHttpServlet;
import org.emrys.webosgi.launcher.internal.FwkExternalAgent;
import org.emrys.webosgi.launcher.internal.IFwkLauncher;
import org.emrys.webosgi.launcher.internal.adapter.ServletContextAdapter;


/**
 * Bridge Servlet to launcher OSGi Java EE Framework embedded in OSGi runtime.
 * 
 * @author LeoChang
 * 
 */
public class BridgeHttpServlet extends DefaultBridgeHttpServlet {

	/**
	 * This name should be the same with servlet parames in the extension
	 * points. The sub plugins may use this name to recognize this servlet in
	 * HttpService. For example, a Help doc publish http service need to
	 * recognize this servlet and not register this servlet again.
	 */
	public static final String SERVLET_NAME = "EmbeddedBridgeServlet";

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
