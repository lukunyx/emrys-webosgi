/**
 * 
 */
package org.emrys.webosgi.launcher.osgi;

import java.net.HttpURLConnection;
import java.net.URL;

import org.emrys.webosgi.launcher.internal.OSGiFwkLauncher;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * @author LeoChang
 * 
 */
public class Activator implements BundleActivator {

	public void start(BundleContext bundleCtx) throws Exception {
		// Send a http request to active all web bundles when this launcher
		// plugin actived. We cann't do it in bridge servlet's init() method,
		// for IBM Domino server will not start the servlet when launching even
		// we set load-on-startup with true in extension point.
		if (OSGiFwkLauncher.isFwkOSGiEmbedded())
			sendWebOSGiStartRequest(bundleCtx);
	}

	public void sendWebOSGiStartRequest(BundleContext bundleCtx) {
		// Send a asyn http url to this OSGi WebContainer to active all web
		// bundles if fwk web context path configured.
		try {
			final String host = bundleCtx.getProperty("webosgi.host");
			// String ctxPathConfigured =
			// bundleCtx.getProperty("webosgi.contextPath");
			if (host != null && host.trim().length() > 0) {
				new Thread(new Runnable() {
					public void run() {
						try {
							// Domino not use global servlet context path, so
							// just use the path in extension point of
							// HttpService.
							HttpURLConnection urlConn = (HttpURLConnection) (new URL(
									host
											+ BridgeHttpServlet.BRIDGE_SERVLET_MAP_PATH)
									.openConnection());
							urlConn.setConnectTimeout(600000);
							// We do not care the response.
							int httpStatus = urlConn.getResponseCode();
							System.out.println("WebOSGi Auto-Start OK.");
						} catch (Exception e) {
							new Exception("WebOSGi Auto-Start Request failed.",
									e).printStackTrace();
						}
					}
				}).start();
			}
		} catch (Exception e) {
			new Exception("WebOSGi Auto-Start Request failed.", e)
					.printStackTrace();
		}
	}

	public void stop(BundleContext bundleCtx) throws Exception {
	}
}
