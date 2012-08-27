/*******************************************************************************
 * Copyright (c) 2011 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.jeewrappers;

import java.util.Dictionary;
import java.util.Enumeration;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.emrys.core.runtime.FwkActivator;
import org.osgi.framework.Bundle;


/**
 * 
 * @author Leo Chang - EMRYS
 * @version 2011-3-24
 */
public class BundledServletConfig implements ServletConfig {

	private final ServletContext ctx;
	private final Dictionary<String, String> initParams;
	private final String servletName;

	public BundledServletConfig(Bundle bundle, Dictionary<String, String> initParams,
			String servletName) {
		ctx = FwkActivator.getInstance().getJeeContainer().findServletContext(bundle);
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
