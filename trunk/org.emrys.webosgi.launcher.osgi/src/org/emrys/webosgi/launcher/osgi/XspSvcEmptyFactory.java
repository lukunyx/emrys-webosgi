package org.emrys.webosgi.launcher.osgi;

import java.net.HttpURLConnection;
import java.net.URL;

import com.ibm.designer.runtime.domino.adapter.HttpService;
import com.ibm.designer.runtime.domino.adapter.IServiceFactory;
import com.ibm.designer.runtime.domino.adapter.LCDEnvironment;

/**
 * Define a empty Xpages Service Factory just to active this bundle for IBM
 * Domino server will load this factory when it start. Do it so, the WebOSGi web
 * container can be started automatically if need.
 * 
 * @author LeoChang
 * 
 */
public class XspSvcEmptyFactory implements IServiceFactory {
	public HttpService[] getServices(LCDEnvironment lcdEnv) {
		// Send a asyn http url to this OSGi WebContainer to active all web
		// bundles if fwk web context path configured.
		try {
			final String host = Activator.BUNDLE_CTX
					.getProperty("webosgi.host");
			String ctxPathConfigured = Activator.BUNDLE_CTX
					.getProperty("webosgi.contextPath");
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
		return new HttpService[0];
	}
}