/*******************************************************************************
 * Copyright (c) 2011 EMRYS Corporation. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the EMRYS License v1.0 which accompanies this
 * distribution, and is available at http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.jsp;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.emrys.core.runtime.jeewrappers.BundledServletConfig;
import org.osgi.framework.Bundle;


/**
 * Jsp Servlet pool.
 * 
 * @author Leo Chang - EMRYS
 * @version 2011-1-13
 */
public class JspServletPool {
	public static Map<Bundle, OSGIJspServlet> buffers = new Hashtable<Bundle, OSGIJspServlet>();

	public static OSGIJspServlet getInstance(Bundle bundle)
			throws ServletException {
		// String webContentPath = WebBundleUtil.findWebContentPath(bundle);
		OSGIJspServlet result = buffers.get(bundle);
		if (result != null)
			return result;

		result = new OSGIJspServlet(bundle);
		initJspServlet(bundle, result);
		buffers.put(bundle, result);
		return result;
	}

	private static void initJspServlet(Bundle bundle, OSGIJspServlet result)
			throws ServletException {
		ServletConfig newConfig = new BundledServletConfig(bundle,
				getJspServletConfig(), bundle.getSymbolicName()
						+ "_jasper_servlet");
		result.init(newConfig);
	}

	private static Dictionary<String, String> getJspServletConfig() {
		Dictionary<String, String> config = new Hashtable<String, String>();
		// validating Do you want to keep the generated Java files around?
		config.put("validating", "false");
		// trimSpaces
		config.put("trimSpaces", "false");
		// enablePooling Determines whether tag handler pooling is enabled.
		config.put("enablePooling", "true");
		// mappedfile = true;
		config.put("mappedfile", "false");
		// classdebuginfo = true;
		config.put("classdebuginfo", "false");
		// checkInterval
		config.put("checkInterval", "5");
		// modificationTestInterval
		config.put("modificationTestInterval", "3");
		// development
		config.put("development", "true");
		// suppressSmap
		// dumpSmap
		// genStrAsCharArray
		// errorOnUseBeanInvalidClassAttribute
		// classpath
		// scratchDir
		// compiler
		// compilerTargetVM
		// javaEncoding
		// compilerClassName
		// fork
		// xpoweredBy
		// displaySourceFragment
		config.put("displaySourceFragment", "false");
		return config;
	}
}
