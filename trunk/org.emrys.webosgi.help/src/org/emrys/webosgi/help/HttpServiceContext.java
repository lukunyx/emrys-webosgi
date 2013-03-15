package org.emrys.webosgi.help;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.emrys.webosgi.core.WebComActivator;
import org.emrys.webosgi.core.runtime.WabServletContext;
import org.osgi.service.http.HttpContext;

/**
 * Servlet Context to adapt the OSGi HttpContext service to Framework Servlet
 * container. The *.jsp file of org.eclipse.help.wabapp bundle will ignored by
 * framework's Jsp engine and let that bundle to handle, though the apache
 * jasper will duplicated in runtime.
 * 
 * @author Leo Chang
 * @version 2011-7-8
 */
public class HttpServiceContext extends WabServletContext {

	private final Set<HttpContext> httpContexts;
	private ClassLoader classLoader;

	public HttpServiceContext(WebComActivator webComponentActivator) {
		super(webComponentActivator);
		this.getWelcomePages().add("index.jsp");
		httpContexts = new HashSet<HttpContext>();
	}

	public void addResourcesContext(String alias, String name,
			HttpContext context) {
		addHttpContext(context);
	}

	@Override
	public URL getResource(String path) throws MalformedURLException {
		// /*.jsp will be processed by a servlet in org.eclipse.help.webapp,
		// here skip.
		if (path.startsWith(this.getWABContextPath() + "/"))
			path = path.replaceFirst(this.getWABContextPath() + "/", "/");
		for (HttpContext httpContext : httpContexts) {
			URL url = httpContext.getResource(path);
			if (url != null)
				return url;
		}

		// Search from other Toc provider extension.
		Set<ITocResProvider> resProviders = HelpSupportActivator.getInstance().resProviders;
		for (ITocResProvider rp : resProviders) {
			URL url = rp.getResource(path);
			if (url != null)
				return url;
		}

		return null;// super.getResource(path);
	}

	@Override
	public String getRealPath(String path) {
		// this is a virturl servlet context root, no real path.
		return null;
	}

	public void addHttpContext(HttpContext httpContext) {
		this.httpContexts.add(httpContext);
	}

	@Override
	public boolean isStaticResource(String path) {
		// Not let framework to handle *.jsp as resource, the servlet from help
		// webapp will do so.
		return !path.endsWith(".jsp");
	}
}
