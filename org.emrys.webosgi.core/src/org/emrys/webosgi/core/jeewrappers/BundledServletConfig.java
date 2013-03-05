package org.emrys.webosgi.core.jeewrappers;

import java.util.Dictionary;
import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.emrys.webosgi.core.service.IWABServletContext;

/**
 * 
 * @author Leo Chang
 * @version 2011-3-24
 */
public class BundledServletConfig implements ServletConfig {

	private final ServletContext ctx;
	private final Dictionary<String, String> initParams;
	private final String servletName;

	public BundledServletConfig(IWABServletContext wabCtx,
			Dictionary<String, String> initParams, String servletName) {
		ctx = wabCtx;
		this.initParams = initParams;
		this.servletName = servletName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletConfig#getInitParameter(java.lang.String)
	 */
	public String getInitParameter(String name) {
		if (initParams == null)
			return null;
		return initParams.get(name);
	}

	public Enumeration getInitParameterNames() {
		if (initParams == null) {
			return new Enumeration() {
				public boolean hasMoreElements() {
					return false;
				}

				public Object nextElement() {
					return null;
				}
			};
		}
		return initParams.keys();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletConfig#getServletContext()
	 */
	public ServletContext getServletContext() {
		return ctx;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletConfig#getServletName()
	 */
	public String getServletName() {
		if (servletName == null)
			return Integer.toString(this.hashCode());
		return servletName;
	}

}
