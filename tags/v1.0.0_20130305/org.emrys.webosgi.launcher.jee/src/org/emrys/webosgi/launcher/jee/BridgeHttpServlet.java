package org.emrys.webosgi.launcher.jee;

import org.emrys.webosgi.launcher.internal.DefaultBridgeHttpServlet;
import org.emrys.webosgi.launcher.internal.IFwkLauncher;

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
