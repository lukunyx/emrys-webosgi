/**
 * 
 */
package org.emrys.webosgi.core;

import org.emrys.webosgi.launcher.internal.IFwkEnvConstants;

/**
 * @author Leo Chang
 */
public interface IFwkConstants {

	public static String HOST_WEB_XML_NAME = "web0.xml";

	public static final String PLATFORM_NAME = IFwkEnvConstants.PLATFORM_NAME;

	public static final String SERVLET_TYPE_HTTP = IFwkEnvConstants.SERVLET_TYPE_HTTP;
	public static final String SERVLET_TYPE_SIP = IFwkEnvConstants.SERVLET_TYPE_SIP;

	public static final String ATTR_FWK_OSGI_EMBEDDED = IFwkEnvConstants.ATTR_FWK_OSGI_EMBEDDED;
	public static final String ATTR_FWK_INSTALL_DIR = IFwkEnvConstants.ATTR_FWK_INSTALL_DIR;
	public static final String ATTR_JEE_WORK_DIR = IFwkEnvConstants.ATTR_JEE_WORK_DIR;
	public static final String ATTR_FWK_WEBAPP_NAME = IFwkEnvConstants.ATTR_FWK_WEBAPP_NAME;
	public static final String ATTR_FWK_WEBAPP_DEPLOY_PATH = IFwkEnvConstants.ATTR_FWK_WEBAPP_DEPLOY_PATH;
	public static final String ATTR_FWK_SERVLET_CTX_PATH = IFwkEnvConstants.ATTR_FWK_SERVLET_CTX_PATH;

	/**
	 * The host addresses(IP or host domain name) of the framework server. The
	 * recommended is the first one. The returned attribute type is
	 * List<String>.
	 */
	public static final String ATTR_WEB_APP_HOSTS = "web.app.hosts";// IFwkEnvConstants.ATTR_WEB_APP_HOST;
	public static final String ATTR_WEB_APP_PORT = IFwkEnvConstants.ATTR_WEB_APP_PORT;

	public static final String ATTR_FWK_WEB_APP_CTX = IFwkEnvConstants.ATTR_FWK_WEB_APP_CTX;

	public static final String SYS_PATH_PREFIX = "#";
}
