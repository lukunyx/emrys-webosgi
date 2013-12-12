/**
 * 
 */
package org.emrys.webosgi.launcher.internal;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.emrys.webosgi.launcher.internal.adapter.ServletContextAdapter;

/**
 * @author LeoChang
 * 
 */
public final class FwkExternalAgent implements IFwkEnvConstants {

	private static FwkExternalAgent INSTANCE;

	private final Map<String, Object> fwkEnvAttrs;
	private final Map<String, Object> fwkDelegateServlets;
	private final Map<String, ServletContextAdapter> fwkServletCtxes;

	public static FwkExternalAgent getInstance() {
		if (INSTANCE == null)
			INSTANCE = new FwkExternalAgent();
		return INSTANCE;
	}

	private FwkExternalAgent() {
		fwkEnvAttrs = new HashMap<String, Object>();
		fwkDelegateServlets = new HashMap<String, Object>();
		fwkServletCtxes = new HashMap<String, ServletContextAdapter>();
	}

	public void regiesterFwkDelegateServlet(String type, Object servletObject) {
		if (servletObject == null)
			fwkDelegateServlets.remove(type);
		fwkDelegateServlets.put(type, servletObject);
	}

	public Object getFwkDelegateServlet(String type) {
		return fwkDelegateServlets.get(type);
	}

	public ServletContextAdapter getFwkServletContext(String servletType) {
		return fwkServletCtxes.get(servletType);
	}

	public void setFwkServletContext(String servletType,
			ServletContextAdapter servletCtx) {
		fwkServletCtxes.put(servletType, servletCtx);
		if (SERVLET_TYPE_HTTP.equals(servletType))
			setFwkEvnAttribute(ATTR_FWK_WEB_APP_CTX, servletCtx);
	}

	public Object getFwkEvnAttribute(String name) {
		return fwkEnvAttrs.get(name);
	}

	public void setFwkEvnAttribute(String name, Object value) {
		if (value == null && name != null)
			fwkEnvAttrs.remove(name);
		if (name != null)
			fwkEnvAttrs.put(name, value);
	}

	public String getOSGiSysConfigProperty(String name) throws Exception {
		Object sysBundleCtx = OSGiFwkLauncher.sysBundleCtx;
		if (sysBundleCtx == null)
			throw new IllegalStateException(
					"No system bundle context found. OSGi framework not launched.");

		Method runMethod = sysBundleCtx.getClass().getMethod("getProperty",
				new Class[] { String.class });
		return (String) runMethod.invoke(sysBundleCtx, new Object[] { name });
	}
}
