package org.emrys.webosgi.help;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.emrys.webosgi.core.jeeres.ServletDelegate;
import org.emrys.webosgi.launcher.osgi.BridgeHttpServlet;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

/**
 * 
 * @author Leo Chang
 * @version 2011-7-7
 */
public class HelpHttpService implements HttpService {

	private final HttpServiceContext servletContext;

	public HelpHttpService() {
		servletContext = (HttpServiceContext) HelpSupportActivator
				.getInstance().getBundleServletContext();
	}

	public HttpContext createDefaultHttpContext() {
		return null;
	}

	public void registerResources(String alias, String name, HttpContext context)
			throws NamespaceException {
		servletContext.addResourcesContext(alias, name, context);
	}

	public void registerServlet(String alias, Servlet servlet,
			Dictionary initparams, HttpContext context)
			throws ServletException, NamespaceException {
		// Except framework bridge servlet to avoid circle recurse.
		if (initparams != null
				&& BridgeHttpServlet.SERVLET_NAME
						.equals(initparams.get("name")))
			return;

		// We wrap the servlet to monitor the Jasper Runtime change event by
		// other plugin.
		servlet = new ServletWrapper(servlet);

		ServletDelegate servletDelegate = new ServletDelegate();
		servletDelegate.className = servlet.getClass().getName();
		servletDelegate.loadOnSetupPriority = 1;
		servletDelegate.name = alias;

		Hashtable<String, String> params = new Hashtable<String, String>();
		if (initparams != null && !initparams.isEmpty()) {
			Enumeration keys = initparams.keys();
			while (keys.hasMoreElements()) {
				Object key = keys.nextElement();
				if (key instanceof String
						&& initparams.get(key) instanceof String)
					params.put((String) key, (String) initparams.get(key));
			}
		}

		servletDelegate.parameters = params;
		servletDelegate.setBundleContext(servletContext);
		servletDelegate.servlet = servlet;
		String nsPrefix = HelpSupportActivator.getInstance()
				.getServiceNSPrefix();
		if (alias.indexOf('*') == -1 && alias.indexOf('.') == -1)
			alias = "/" + nsPrefix + alias
					+ ServletDelegate.MULTI_MAP_SEG_SEPERATOR + "/" + nsPrefix
					+ alias + "/*";
		else {
			// case /*.jsp map from org.eclipse.help.webapp extension point.
			alias = "/" + nsPrefix + alias;
		}

		servletDelegate.setRawURLPatterns(alias);
		servletContext.addHttpContext(context);
		servletContext.getServletsInfo().add(servletDelegate);
	}

	public void unregister(String alias) {
		Iterator<ServletDelegate> it = servletContext.getServletsInfo()
				.iterator();
		while (it.hasNext()) {
			ServletDelegate delegate = it.next();
			if (delegate.getRawURLPatterns().equals(alias))
				it.remove();
		}

		// need to refresh OSGiJEEContainer
	}
}
