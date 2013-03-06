/**
 * 
 */
package org.emrys.webosgi.launcher.osgi;

import javax.servlet.ServletConfig;

import org.emrys.webosgi.launcher.internal.DefaultFwkLauncher;


/**
 * Framework Launcher in embedded OSGi mode.
 * 
 * @author Leo Chang
 * 
 */
public class EmbeddedOSGiFwkLauncher extends DefaultFwkLauncher {

	private ServletConfig bridgeServletConfig;

	@Override
	public void init(ServletConfig servletConfig) {
		this.bridgeServletConfig = servletConfig;
		init();
	}

	@Override
	public void init() {
		super.init();
		// osgiFwkLauncher.init(bridgeServletConfig);
		Object serverWorkDir = bridgeServletConfig.getServletContext()
				.getAttribute("javax.servlet.context.tempdir");
		fwkAgent.setFwkEvnAttribute(ATTR_JEE_WORK_DIR, serverWorkDir);
		// fwkAgent.setFwkEvnAttribute(ATTR_FWK_WEB_APP_CTX,
		// servletConfig.getServletContext().getContextPath());
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
		return "OSGi Runtime/Domino Version 8.x.x";
	}
}
