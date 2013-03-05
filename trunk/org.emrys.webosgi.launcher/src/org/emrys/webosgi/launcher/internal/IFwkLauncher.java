/**
 * 
 */
package org.emrys.webosgi.launcher.internal;

import javax.servlet.ServletConfig;

/**
 * @author LeoChang
 * 
 */
public interface IFwkLauncher extends IFwkEnvConstants {

	void init();

	void init(ServletConfig servletConfig);

	void deploy();

	void start() throws Exception;

	void stop();

	void destroy();

	String getEnviromentInfo();
}
