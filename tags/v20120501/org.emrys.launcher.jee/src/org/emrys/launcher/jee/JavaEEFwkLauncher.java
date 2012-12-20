/**
 * 
 */
package org.emrys.launcher.jee;

import javax.servlet.ServletConfig;

import org.emrys.core.launcher.internal.DefaultFwkLauncher;

/**
 * OSGi JavaEE Framework's launcher in JavaEE server mode.
 * 
 * @author Leo Chang
 * 
 */
public class JavaEEFwkLauncher extends DefaultFwkLauncher {

	private ServletConfig bridgeServletConfig;

	@Override
	public void init(ServletConfig servletConfig) {
		this.bridgeServletConfig = servletConfig;
		Object serverWorkDir = bridgeServletConfig.getServletContext()
				.getAttribute("javax.servlet.context.tempdir");
		fwkAgent.setFwkEvnAttribute(ATTR_JEE_WORK_DIR, serverWorkDir);
		fwkAgent.setFwkEvnAttribute(ATTR_FWK_WEBAPP_DEPLOY_PATH,
				bridgeServletConfig.getServletContext().getRealPath("/"));
		// servletConfig.getServletContext().getContextPath();
		// fwkAgent.setFwkEvnAttribute(ATTR_JEE_WORK_DIR, "");
		// fwkAgent.setFwkEvnAttribute(ATTR_FWK_WEB_APP_CTX, "");
		init();
	}

	@Override
	public void init() {
		super.init();
		osgiFwkLauncher.init(bridgeServletConfig);
	}

	@Override
	public void deploy() {
		// no more deploy need
		super.deploy();
	}

	@Override
	public void start() throws Exception {
		super.start();
	}

	@Override
	public void stop() {
		super.stop();
	}

	@Override
	public void destroy() {
		super.destroy();
	}

	@Override
	public String getEnviromentInfo() {
		return "Java EE Server";
	}
}
