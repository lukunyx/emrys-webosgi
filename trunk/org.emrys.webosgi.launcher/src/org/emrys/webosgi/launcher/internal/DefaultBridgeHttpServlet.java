package org.emrys.webosgi.launcher.internal;

import java.io.IOException;
import java.lang.reflect.Method;
import java.rmi.ServerException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.emrys.webosgi.launcher.internal.adapter.HttpServletRequestAdapter;
import org.emrys.webosgi.launcher.internal.adapter.HttpServletResponseAdapter;
import org.emrys.webosgi.launcher.internal.adapter.ServletContextAdapter;


/**
 * Default implementation of Bridget Http Servlet for OSGi Java EE Framework.
 * 
 * @author LeoChang
 * 
 */
public abstract class DefaultBridgeHttpServlet extends HttpServlet {

	private static final long serialVersionUID = 1697892540453511903L;

	private Object fwkDelegateServlet;
	private ServletConfig config;
	private int delegateReferenceCount;
	private boolean isFwkLaunchedByOther;
	protected FwkExternalAgent fwkAgent;

	private static boolean isJeeContainerInitialized;

	private static boolean isJeeContainerInitializing;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		fwkAgent = FwkExternalAgent.getInstance();

		// Check if the fwk has been launched by another launcher to skip.
		ServletContextAdapter fwkServletCtxAdapter = fwkAgent
				.getFwkServletContext(FwkExternalAgent.SERVLET_TYPE_HTTP);
		if (fwkServletCtxAdapter != null) {
			isFwkLaunchedByOther = true;
			return;
		}

		this.config = config;
		ServletContext fwkServletCtx = config.getServletContext();
		ServletContextAdapter servletCtxAdapter = new ServletContextAdapter(
				fwkServletCtx);
		fwkAgent.setFwkServletContext(FwkExternalAgent.SERVLET_TYPE_HTTP,
				servletCtxAdapter);

		try {
			IFwkLauncher fwkLauncher = getFwkLauncher();
			fwkLauncher.init(getServletConfig());
			fwkLauncher.deploy();
			fwkLauncher.start();
		} catch (Exception e) {
			throw new ServletException("Framework start failed.", e);
		}
	}

	public abstract IFwkLauncher getFwkLauncher();

	public boolean isFwkLaunchedByOther() {
		return isFwkLaunchedByOther;
	}

	@Override
	public void destroy() {
		// Is OK to ignore uncompleted requests(the delegate servlet reference
		// count > 0)?
		getFwkLauncher().stop();
		getFwkLauncher().destroy();
		super.destroy();
	}

	@Override
	public ServletConfig getServletConfig() {
		// More customized configuration here...
		return config;
	}

	@Override
	public String getServletInfo() {
		return super.getServletInfo()
				+ "/Bridge Servlet for OSGi JavaEE Container 1.0.0";
	}

	/**
	 * service is called by the Servlet Container and will first determine if
	 * the request is a framework control and will otherwise try to delegate to
	 * the registered servlet delegate
	 * 
	 */
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// If framework has been launched by another launcher, return
		// NOT_FOUND http status.
		if (isFwkLaunchedByOther()) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		// To support both servlet 2.4/2.5 server(here domino8/servlet2.5),
		// adapt all servlet resources on the entrance of framework.
		HttpServletRequestAdapter newReq = new ExtensionMappingRequest(req);
		HttpServletResponseAdapter newResp = new HttpServletResponseAdapter(
				resp);

		Object servletReference = acquireDelegateReference();
		if (servletReference == null) {
			resp
					.sendError(
							HttpServletResponse.SC_NOT_FOUND,
							"Framework Servlet Delegate not found for: " + newReq.getRequestURI()); //$NON-NLS-1$
			return;
		}

		// Buffer the original thread context class loader and ready to invoke
		// framework objecct. For framework may change this.
		ClassLoader original = Thread.currentThread().getContextClassLoader();
		try {
			if (!isJeeContainerInitialized) {
				// If the delegate servlet is initializing, wait.
				while (isJeeContainerInitializing) {
					Thread.sleep(20);
				}
				// If the JavaEE Container not initilaized yet, do it at first.
				Method method = servletReference.getClass().getMethod(
						"isInitialized");
				isJeeContainerInitialized = (Boolean) method
						.invoke(servletReference);
				if (!isJeeContainerInitialized) {
					isJeeContainerInitializing = true;
					try {
						// Set several attributes which can only be retrieved
						// when
						// the
						// first request comes.
						// Servlet Context path(for in JavaEE 2.4,
						// ServletContext.getContextPath() not available).
						String fwkServletCtxPath = getSpecifiedContextPath();
						if (fwkServletCtxPath == null)
							fwkServletCtxPath = req.getContextPath();

						fwkAgent.setFwkEvnAttribute(
								FwkExternalAgent.ATTR_FWK_SERVLET_CTX_PATH,
								fwkServletCtxPath);
						fwkAgent.getFwkServletContext(
								FwkExternalAgent.SERVLET_TYPE_HTTP)
								.setContextPath(fwkServletCtxPath);
						// Set Server host and port
						fwkAgent.setFwkEvnAttribute(
								FwkExternalAgent.ATTR_WEB_APP_HOST, req
										.getServerName());
						fwkAgent.setFwkEvnAttribute(
								FwkExternalAgent.ATTR_WEB_APP_PORT, req
										.getServerPort());

						method = servletReference.getClass().getMethod("init");
						method.invoke(servletReference);
					} finally {
						isJeeContainerInitializing = false;
					}
				}
			}

			// Dispatch the request to JavaEE Containe inside framework.
			Method serviceMethod = servletReference.getClass().getMethod(
					"service", HttpServletRequestAdapter.class,
					HttpServletResponseAdapter.class);
			serviceMethod.invoke(servletReference, newReq, newResp);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServerException(
					"Invoke serivce method from Delegate Servlet failed.", e);
		} finally {
			releaseDelegateReference();
			Thread.currentThread().setContextClassLoader(original);
		}
	}

	protected String getSpecifiedContextPath() {
		return null;
	}

	private void releaseDelegateReference() {
		--delegateReferenceCount;
	}

	private Object acquireDelegateReference() {
		if (fwkDelegateServlet == null)
			fwkDelegateServlet = fwkAgent
					.getFwkDelegateServlet(FwkExternalAgent.SERVLET_TYPE_HTTP);
		if (fwkDelegateServlet != null)
			++delegateReferenceCount;
		return fwkDelegateServlet;
	}

	static class ExtensionMappingRequest extends HttpServletRequestAdapter {

		public ExtensionMappingRequest(HttpServletRequest req) {
			super(req);
		}

		@Override
		public String getPathInfo() {
			// Let the pathInfo be empty string. Tomcat6's hehavior is to set it
			// null.
			return null;//$NON-NLS-1$
		}

		@Override
		public String getServletPath() {
			// At the entrance of JEE OSGi Container, use the pathInfo as
			// servlet path.
			return super.getPathInfo();
		}
	}
}
