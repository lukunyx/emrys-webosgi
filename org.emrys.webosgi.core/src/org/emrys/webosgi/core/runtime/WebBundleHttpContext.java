package org.emrys.webosgi.core.runtime;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.http.HttpContext;

/**
 * Web Bundled HttpContext
 * 
 * @author Leo Chang
 * @version 2010-11-8
 */
public class WebBundleHttpContext implements HttpContext {
	/**
	 * Wrappered ServletContext
	 */
	private final ServletContext servletContext;

	public WebBundleHttpContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	public String getMimeType(String name) {
		int index = name.lastIndexOf('/');
		return servletContext.getMimeType(name.substring(index + 1));
	}

	public URL getResource(String name) {
		try {
			return servletContext.getResource(name);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		return null;
	}

	public boolean handleSecurity(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		return true;
	}
}
