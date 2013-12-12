package org.emrys.webosgi.launcher.jee;

import java.net.HttpURLConnection;
import java.net.URL;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.emrys.webosgi.launcher.internal.DefaultBridgeHttpServlet;
import org.emrys.webosgi.launcher.internal.FwkExternalAgent;
import org.emrys.webosgi.launcher.internal.IFwkLauncher;
import org.emrys.webosgi.launcher.internal.adapter.ServletContextAdapter;

/**
 * JavaEE Server Bridge Servlet to launcher framework.
 * 
 * @author LeoChang
 * 
 */
public class BridgeHttpServlet extends DefaultBridgeHttpServlet {

	private static final long serialVersionUID = 42548275286726287L;
	private JavaEEFwkLauncher fwkLauncher;

	@Override
	public void init(final ServletConfig config) throws ServletException {
		super.init(config);
		// Send a asyn http url to this OSGi WebContainer to active all web
		// bundles if fwk web context path configured.
		try {
			final String host = fwkAgent
					.getOSGiSysConfigProperty("webosgi.host");
			String ctxPathConfigured = fwkAgent
					.getOSGiSysConfigProperty("webosgi.contextPath");
			if (host != null && host.trim().length() > 0) {
				// Check if servletContext.getContextPath() is available if
				// Servlet 2.5. It true, override the configured context path.
				ServletContextAdapter fwkServletCtxAdapter = fwkAgent
						.getFwkServletContext(FwkExternalAgent.SERVLET_TYPE_HTTP);
				String ctxPathFromServlet = fwkServletCtxAdapter
						.getContextPath();
				final String ctxPath = ctxPathFromServlet != null ? ctxPathFromServlet
						: (ctxPathConfigured != null ? ctxPathConfigured.trim()
								: "");
				new Thread(new Runnable() {
					public void run() {
						try {
							HttpURLConnection urlConn = (HttpURLConnection) (new URL(
									host + ctxPath).openConnection());
							urlConn.setConnectTimeout(600000);
							// We do not care the response.
							int httpStatus = urlConn.getResponseCode();
							config.getServletContext().log(
									"WebOSGi Auto-Start OK.");
						} catch (Exception e) {
							config.getServletContext().log(
									"WebOSGi Auto-Start Request failed.", e);
						}
					}
				}).start();
			}
		} catch (Exception e) {
			if (!(e instanceof ServletException))
				e = new ServletException(e);
			throw (ServletException) e;
		}
	}

	@Override
	public IFwkLauncher getFwkLauncher() {
		if (fwkLauncher == null)
			fwkLauncher = new JavaEEFwkLauncher();
		return fwkLauncher;
	}

	@Override
	public String getServletInfo() {
		return super.getServletInfo()
				+ "/Bridge Servlet for OSGi JavaEE Container 1.0.0";
	}

	@Override
	protected String getSpecifiedContextPath() {
		// return null path, let the super class to decide.
		return null;
	}
}
