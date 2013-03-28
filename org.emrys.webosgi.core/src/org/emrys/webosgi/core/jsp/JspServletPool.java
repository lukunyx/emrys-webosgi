package org.emrys.webosgi.core.jsp;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.emrys.webosgi.core.jeewrappers.BundledServletConfig;
import org.emrys.webosgi.core.service.IWABServletContext;
import org.osgi.framework.Bundle;

/**
 * Jsp Servlet pool.
 * 
 * @author Leo Chang
 * @version 2011-1-13
 */
public class JspServletPool {
	public static Map<Bundle, JasperServletWrapper> buffers = new Hashtable<Bundle, JasperServletWrapper>();

	public static JasperServletWrapper getInstance(IWABServletContext wabCtx)
			throws ServletException {
		Bundle bundle = wabCtx.getBundle();
		// String webContentPath = WebBundleUtil.findWebContentPath(bundle);
		JasperServletWrapper result = buffers.get(bundle);
		if (result != null)
			return result;

		result = new JasperServletWrapper(wabCtx);
		initJspServlet(wabCtx, result);
		buffers.put(bundle, result);
		return result;
	}

	public static void desryJspServlet(Bundle bundle) {
		JasperServletWrapper result = buffers.remove(bundle);
		if (result != null)
			result.destroy();
	}

	public static void destroyAllServlets() {
		Iterator<Entry<Bundle, JasperServletWrapper>> eit = buffers.entrySet()
				.iterator();
		while (eit.hasNext()) {
			Entry<Bundle, JasperServletWrapper> entry = eit.next();
			entry.getValue().destroy();
			eit.remove();
		}
	}

	private static void initJspServlet(IWABServletContext wabCtx,
			JasperServletWrapper result) throws ServletException {
		ServletConfig newConfig = new BundledServletConfig(wabCtx,
				getJspServletConfig(), wabCtx.getContextPath()
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
