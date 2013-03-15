/**
 * 
 */
package org.emrys.webosgi.help;

import java.io.File;
import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspFactory;

import org.apache.jasper.compiler.JspRuntimeContext;
import org.apache.jasper.runtime.JspFactoryImpl;

/**
 * @author LeoChang
 * 
 */
public class ServletWrapper implements Servlet {

	private final Servlet servlet;
	private static JspFactory jspFactoryIns;

	public ServletWrapper(Servlet servlet) {
		this.servlet = servlet;
	}

	public void destroy() {
		servlet.destroy();
	}

	public ServletConfig getServletConfig() {
		return servlet.getServletConfig();
	}

	public String getServletInfo() {
		return servlet.getServletInfo();
	}

	public void init(ServletConfig arg0) throws ServletException {
		servlet.init(arg0);
	}

	public void service(ServletRequest req, ServletResponse resp)
			throws ServletException, IOException {
		// NOTE: If multiple org.apache.jasper packages exists in runtime,
		// org.apache.jasper.compiler.JspRuntimeContext may be initialized more
		// than one time. This will make the singleton JspFactory's default
		// instance changed and incompatible with the parent class JspFactory.
		// We do a extra check and change it back if need.
		Object jspFactory = JspFactory.getDefaultFactory();
		if (jspFactory != jspFactoryIns
				&& !JspRuntimeContext.class.getClassLoader().equals(
						jspFactory.getClass().getClassLoader())) {
			if (jspFactoryIns == null)
				jspFactoryIns = new JspFactoryImpl();
			JspFactory.setDefaultFactory(jspFactoryIns);
		}

		// Because the equinox help webapp will place jsp tmp class file at
		// $tmp$/org/apache/jsp/, this will override the Host Wab's. We the
		// reset the jsp tmp dir to a new one.
		File oldJspTmpDir = null;
		ServletContext servletCtx = null;
		if (req instanceof HttpServletRequest) {
			servletCtx = ((HttpServletRequest) req).getSession()
					.getServletContext();
			oldJspTmpDir = (File) servletCtx
					.getAttribute("javax.servlet.context.tempdir");
			File newTmpJspDir = null;
			if (oldJspTmpDir != null)
				newTmpJspDir = new File(oldJspTmpDir, "help_jsp_tmp");
			else
				newTmpJspDir = new File(HelpSupportActivator.getInstance()
						.getComponentWorkspaceRoot(), "help_jsp_tmp");

			servletCtx.setAttribute("javax.servlet.context.tempdir",
					newTmpJspDir);
		}
		try {
			servlet.service(req, resp);
		} finally {
			if (servletCtx != null)
				servletCtx.setAttribute("javax.servlet.context.tempdir",
						oldJspTmpDir);
		}
	}
}
