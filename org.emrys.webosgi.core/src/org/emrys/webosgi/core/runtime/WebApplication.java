/**
 * 
 */
package org.emrys.webosgi.core.runtime;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.emrys.webosgi.core.FwkActivator;
import org.emrys.webosgi.core.IFwkConstants;
import org.emrys.webosgi.core.ServiceInitException;
import org.emrys.webosgi.core.internal.FwkRuntime;
import org.emrys.webosgi.core.jeeres.FilterDelegate;
import org.emrys.webosgi.core.jeeres.FilterDelegateBase;
import org.emrys.webosgi.core.jeeres.ListenerInfo;
import org.emrys.webosgi.core.jeeres.ServletDelegate;
import org.emrys.webosgi.core.jeeres.TaglibInfo;
import org.emrys.webosgi.core.service.IOSGiWebContainer;
import org.emrys.webosgi.core.service.IWABServletContext;
import org.emrys.webosgi.core.service.IWebApplication;
import org.emrys.webosgi.core.util.WebBundleUtil;
import org.osgi.framework.Bundle;
import org.osgi.service.http.NamespaceException;

/**
 * @author LeoChang
 * 
 */
public class WebApplication implements IWebApplication {

	private static IOSGiWebContainer webContainer = FwkActivator.getInstance()
			.getJeeContainer();
	private static FwkRuntime fwkRuntime = FwkRuntime.getInstance();

	private final Bundle bundle;
	private File webContentRoot;
	private IWABServletContext servletContext;
	private boolean isDynaServicesStarted;
	private String ctxPath;
	private boolean triedToStartFailed;

	public WebApplication(Bundle webbundle) {
		this.bundle = webbundle;
		String ctxPath = WebBundleUtil.getWabContextPathHeader(webbundle);
		if (StringUtils.isNotEmpty(ctxPath)) {
			if (!ctxPath.startsWith("/"))
				ctxPath = "/" + ctxPath;
			this.setWebContextPath(ctxPath);
		}
		// else check if this given bundle is a web bundle with context path.
	}

	public IOSGiWebContainer getWebContainer() {
		return webContainer;
	}

	public Bundle getWebBundle() {
		return bundle;
	}

	public File findWebContentRoot(boolean forceUpdate) {
		if (webContentRoot == null || forceUpdate) {
			try {
				webContentRoot = WebBundleUtil.getExtractedWebContentRoot(
						bundle, forceUpdate).toFile();
			} catch (IOException e) {
				// e.printStackTrace();
				log(e);
			}
		}
		return webContentRoot;
	}

	public IWABServletContext getBundleServletContext() {
		if (servletContext == null)
			servletContext = new WabServletContext(this);

		return servletContext;
	}

	public void setWebContextPath(String ctxPath) {
		this.ctxPath = ctxPath;
	}

	public String getWebContextPath() {
		String ctxPath = WebBundleUtil.getWabContextPathHeader(bundle);
		if (StringUtils.isNotEmpty(ctxPath))
			return ctxPath.startsWith("/") ? ctxPath : "/" + ctxPath;

		return this.ctxPath;
	}

	public void pubStaticResources() throws ServiceInitException {
		// Do nothing by default.
	}

	public boolean isStaticResPublished() {
		return true;
	}

	public void init() throws ServiceInitException {
		// Validate context path
		validateContextPath();

		// It need wab servlet context be registered to web container early for
		// resource publish.
		IWABServletContext servletContext = getBundleServletContext();
		try {
			if (!getWebContainer().getAllBundledServletContext().contains(
					servletContext)) {
				getWebContainer().regServletContext(servletContext);
			}
		} catch (Exception e) {
			throw new ServiceInitException(new Status(Status.ERROR, bundle
					.getSymbolicName(), "Web Service init failed.", e));
		}

		// Parse JavaEE Configuration to servlet context.
		// avoid dom4j using SAXParser provided from server
		/*
		 * String externalSaxParser =
		 * System.getProperty("javax.xml.parsers.SAXParserFactory"); // init
		 * web.xml config all from other source.
		 * System.setProperty("javax.xml.parsers.SAXParserFactory", null); try {
		 * initWebConfig(); } finally {
		 * System.setProperty("javax.xml.parsers.SAXParserFactory",
		 * externalSaxParser); }
		 */

		// Parse JavaEE Configure from web.xml if exists.
		initWebConfig();
	}

	public void startDynamicServices() throws ServiceInitException {
		if (!fwkRuntime.isFwkInited())
			throw new ServiceInitException(
					new Status(Status.ERROR, bundle.getSymbolicName(),
							"The web bundle cann't active before web container initiliazed."));

		synchronized (this) {
			// Only call once.
			if (!isDynaServicesStarted() && !triedToStartFailed) {
				// Enter app starting block.
				fwkRuntime.enterAppSvcStart(this);

				try {
					triedToStartFailed = true;
					// Start JavaEE service to wab servlet context. Now, the
					// bundle should be actived and bundle context is available.
					active();
					isDynaServicesStarted = true;

					log(new Status(Status.INFO, bundle.getSymbolicName(),
							"Web Service in bundle ID:"
									+ bundle.getSymbolicName() + " ["
									+ getWebContextPath() + "] published OK."));
				} catch (Throwable t) {
					// t.printStackTrace();
					webContainer.unregServletContext(servletContext);
					log(new Status(Status.ERROR, bundle.getSymbolicName(),
							"Web Service in Web bundle:"
									+ bundle.getSymbolicName() + " ["
									+ getWebContextPath()
									+ "] published Failed.", t));
				} finally {
					// Quit app starting block anyhow at last.
					fwkRuntime.quitAppSvcStart(this);
				}
			}
		}
	}

	public void stopDynamicServices() {
		// No need to deactive the servletContext here, the web container will
		// do this for use.
		this.getWebContainer().unregServletContext(servletContext);
		isDynaServicesStarted = false;
		servletContext = null;
	}

	public boolean isDynaServicesStarted() {
		return isDynaServicesStarted;
	}

	public void setDynaServicesStarted(boolean b) {
		this.isDynaServicesStarted = b;
	}

	protected void active() throws ServiceInitException {
		try {
			getWebContainer().activeServletContext(servletContext);
			initFilters();
			initOnStartServlet();
		} catch (Exception e) {
			throw new ServiceInitException(new Status(Status.ERROR, bundle
					.getSymbolicName(), "Web Service init failed.", e));
		}
	}

	private void initFilters() throws ServletException {
		for (FilterDelegate f : servletContext.getFilters()) {
			try {
				f.init(null);
			} catch (Exception e) {
				throw new ServletException("Init servlet filter:"
						+ f.getFilterName() + " failed.", e);
			}
		}
	}

	private void initOnStartServlet() throws ServletException {
		List<ServletDelegate> servlets = new ArrayList<ServletDelegate>();
		servlets.addAll(servletContext.getServletsInfo());
		Collections.sort(servlets, new Comparator() {
			public int compare(Object o1, Object o2) {
				ServletDelegate info1 = (ServletDelegate) o1;
				ServletDelegate info2 = (ServletDelegate) o2;
				if (info1.loadOnSetupPriority < info2.loadOnSetupPriority)
					return -1;
				else if (info1.loadOnSetupPriority > info2.loadOnSetupPriority)
					return 1;
				else
					return 0;
			}
		});

		for (ServletDelegate servletDelegate : servlets) {
			if (servletDelegate.loadOnSetupPriority > 0)
				servletDelegate.init(null);
		}
	}

	/**
	 * Check the doubled service name space id in loaded web bundles.
	 * 
	 * @throws ServiceInitException
	 */
	private void validateContextPath() throws ServiceInitException {
		// check double named Name Space and its prefix.
	}

	/**
	 * Default method to find web.xml file from web content directory.
	 * 
	 * @return
	 * @throws Exception
	 */
	protected InputStream getWebXmlInput() throws Exception {
		// If this web bundle has not Web Content directory, not find any
		// web.xml.
		if (webContentRoot != null) {
			String webXmlFileName = "web.xml";
			// If this wab bundle is bridge web host bundle, use special
			// web.xml file name.
			if (getBundleServletContext().isHostBundle())
				webXmlFileName = WebBundleUtil.HOST_WEB_XML_NAME;
			IPath webXmlFilePath = new Path(webContentRoot.getAbsolutePath())
					.append("WEB-INF/" + webXmlFileName);
			File webXmlFile = webXmlFilePath.toFile();
			if (webXmlFile != null && webXmlFile.exists())
				return new FileInputStream(webXmlFile);
		}
		return null;
	}

	protected void initWebConfig() throws ServiceInitException {
		try {
			// Let sub class to provide singleton web.xml file, not only in a
			// WebContent folder.
			InputStream in = getWebXmlInput();
			if (in == null)
				return;

			SAXReader reader = new SAXReader();
			Document doc = reader.read(in);

			initServletContextParameters(doc);
			initServletContextListeners(doc);
			initJspConfigs(doc);
			// Init servlet filter and init each and init them.
			initServletContextFilters(doc);
			// Init servelts config and later we need init each servlet by its
			// load-on-setup paramater.

			initServlets(doc);
			initOthers(doc);
			webContainer.refresh();
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceInitException(new Status(Status.ERROR, bundle
					.getSymbolicName(), "Initialize Web.xml configre failed["
					+ this.getWebContextPath() + "].", e));
		}
	}

	/**
	 * @param doc
	 */
	private void initOthers(Document doc) {
		// Welcome pages
		Element welcomePageEle = doc.getRootElement().element(
				"welcome-file-list");
		if (welcomePageEle != null) {
			for (Iterator i = welcomePageEle.elementIterator("welcome-file"); i
					.hasNext();) {
				Element pEle = (Element) i.next();
				String page = pEle.getTextTrim();
				if (page != null && page != null) {
					servletContext.getWelcomePages().add(page);
				}
			}
		}

		// err pages
		for (Iterator i = doc.getRootElement().elementIterator("error-page"); i
				.hasNext();) {
			Element pEle = (Element) i.next();
			String code = pEle.elementTextTrim("error-code");
			String location = pEle.elementTextTrim("location");
			if (code != null && location != null && location.length() > 0) {
				try {
					servletContext.getErrorPages().put(Integer.parseInt(code),
							location);
				} catch (Exception e) {
					// e.printStackTrace();
				}
			}
		}

		// session valid interval in seconds.
		Element sessionConfigEle = doc.getRootElement().element(
				"session-config");
		if (sessionConfigEle != null) {
			String intervalStr = sessionConfigEle
					.elementText("session-timeout");
			if (intervalStr != null) {
				try {
					int interval = Integer.parseInt(intervalStr);
					// If interval set too big, the expiretion may be bigger the
					// Long.MAX_VALUE.
					if (interval > 0 && interval * 60 * 1000 < 0) {
						log(FwkActivator.LOG_ERROR, 0,
								"Session timeout to big. Use default timeout.",
								new IllegalAccessException(
										"session-timeout too big"));
					} else {
						// session-timeout unit is minutes, and context need it
						// to be seconds.
						servletContext.setSessionTimeout(interval * 60);
					}
				} catch (NumberFormatException e) {
					log(FwkActivator.LOG_ERROR, 0, "Session timeout invalid.",
							e);
				}
			}
		}
	}

	/**
	 * Register Jsp tag lib configure.
	 * 
	 * @param httpService
	 * @param doc
	 * @param wb
	 */
	private void initJspConfigs(Document doc) {
		/*
		 * <jsp-config> <taglib> <taglib-uri>www.example.com/jsptag</taglib-uri>
		 * <taglib-location>sagag.tlb</taglib-location> </taglib> </jsp-config>
		 */
		for (Iterator i = doc.getRootElement().elementIterator("jsp-config"); i
				.hasNext();) {
			Element pEle = (Element) i.next();
			for (Iterator i1 = pEle.elementIterator("taglib"); i1.hasNext();) {
				Element pEle1 = (Element) i1.next();
				String uri = pEle.elementText("taglib-uri");
				String location = pEle.elementText("taglib-location");
				if (uri != null && location != null) {
					TaglibInfo info = new TaglibInfo();
					info.uri = uri;
					// Add web bundle prefix.
					info.location = this.getWebContextPath()
							+ (location.startsWith("/") ? "" : "/") + location;
					servletContext.getTaglibs().add(info);
				}
			}
		}
	}

	private void initServletContextParameters(Document doc) {
		/*
		 * <context-param> <param-name>contextConfigLocation</param-name>
		 * <param-value>classpath:applicationContext.xml</param-value>
		 * </context-param>
		 */
		for (Iterator i = doc.getRootElement().elementIterator("context-param"); i
				.hasNext();) {
			Element pEle = (Element) i.next();
			String name = pEle.elementTextTrim("param-name");
			String value = pEle.elementTextTrim("param-value");
			if (!StringUtils.isEmpty(name) && !StringUtils.isEmpty(value)) {
				servletContext.setInitParameter(name, value);
			}
		}
	}

	private void initServletContextListeners(Document doc)
			throws ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		/*
		 * <listener>
		 * <listener-class>org.springframework.web.context.ContextLoaderListener
		 * </listener-class> </listener>
		 */
		for (Iterator i = doc.getRootElement().elementIterator("listener"); i
				.hasNext();) {
			Element pEle = (Element) i.next();
			String clazzName = pEle.elementTextTrim("listener-class");
			if (clazzName != null && clazzName.length() > 0) {
				ListenerInfo info = new ListenerInfo();
				info.className = clazzName;
				info.setBundleContext(servletContext);
				servletContext.getListeners().add(info);
			}
		}
	}

	/**
	 * @param httpService
	 * @param wb
	 * @param webBundles
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws ServletException
	 */
	private void initServletContextFilters(Document doc)
			throws ClassNotFoundException, InstantiationException,
			IllegalAccessException, ServletException {
		/*
		 * <filter> <filter-name>MoreContext</filter-name>
		 * <filter-class>org.more.submit.support.web.SubmitRoot</filter-class>
		 * <init-param> <param-name>buildClass</param-name>
		 * <param-value>org.more
		 * .submit.casing.spring.WebSpringBuilder</param-value> </init-param>
		 * </filter> <filter-mapping>
		 * <filter-name>bridge.servlet.filter</filter-name>
		 * <servlet-name>equinoxbridgeservlet</servlet-name>
		 * <url-pattern>/*</url-pattern> <dispatcher>REQUEST</dispatcher>
		 * <dispatcher>REQUEST</dispatcher> <dispatcher>FORWARD</dispatcher>
		 * <dispatcher>INCLUDE</dispatcher> <dispatcher>EXCEPTION</dispatcher>
		 * or <dispatcher>ERROR</dispatcher> </filter-mapping>
		 */

		// Collect all filters at first.
		Map<String, FilterDelegateBase> filters = new HashMap<String, FilterDelegateBase>();
		// The order of filter decided by the order the map defined.
		for (Iterator i = doc.getRootElement().elementIterator("filter"); i
				.hasNext();) {
			Element pEle = (Element) i.next();
			String filterName = pEle.elementTextTrim("filter-name");
			if (StringUtils.isEmpty(filterName))
				continue;

			String clazzName = pEle.elementTextTrim("filter-class");
			if (StringUtils.isEmpty(clazzName))
				continue;

			Hashtable<String, String> params = new Hashtable<String, String>();
			for (Iterator i0 = pEle.elementIterator("init-param"); i0.hasNext();) {
				Element pEle0 = (Element) i0.next();
				String paramName = pEle0.elementTextTrim("param-name");
				String paramValue = pEle0.elementTextTrim("param-value");
				if (paramName != null && paramName.length() > 0
						&& paramValue != null)
					params.put(paramName, paramValue);
			}

			FilterDelegateBase filter = new FilterDelegateBase();
			filter.clazzName = clazzName;
			filter.parameters = params;
			filter.name = filterName;
			filters.put(filterName, filter);
		}

		// Create filter delegate for each filter-mapping by define order.

		for (Iterator i1 = doc.getRootElement().elementIterator(
				"filter-mapping"); i1.hasNext();) {
			Element pEle1 = (Element) i1.next();
			String mapFilterName = pEle1.elementTextTrim("filter-name");
			if (StringUtils.isEmpty(mapFilterName))
				continue;

			FilterDelegateBase filter = filters.get(mapFilterName);
			if (filter == null)
				continue;

			String targetServletNames = null;
			String urlParttern = null;
			String dispatchers = null;
			// Search for mutiple dispatchers map at first.
			for (Iterator dispatchersIt = pEle1.elementIterator("dispatcher"); dispatchersIt
					.hasNext();) {

				String s = ((Element) dispatchersIt.next()).getTextTrim();
				// dispatcher type can only be one of REQUEST FORWARD
				// INCLUDE EXCEPTION/ERROR
				if (!StringUtils.isEmpty(s)
						&& FilterDelegate.DISPATCHERS.valueOf(s) != null) {
					if (dispatchers == null)
						dispatchers = s;
					else
						dispatchers = dispatchers
								+ FilterDelegate.DISPATCHERS_SEPERATOR + s;
				}
			}

			// Search for multiple URL-Pattern setting unders a
			// filter-mapping element.
			for (Iterator urlPatternIt = pEle1.elementIterator("url-pattern"); urlPatternIt
					.hasNext();) {
				String s = ((Element) urlPatternIt.next()).getTextTrim();
				if (StringUtils.isEmpty(s))
					continue;

				// if a IFwkConstants.SYS_PATH_PREFIX prefixed, not insert
				// bundle's service prefix.
				if (s.startsWith(IFwkConstants.SYS_PATH_PREFIX)) {
					s = s.substring(IFwkConstants.SYS_PATH_PREFIX.length());
				} else if (s != null && !servletContext.isHostBundle()) {
					// insert the name-space prefix of this web component to
					// each URL map pattern.
					s = this.getWebContextPath()
							+ (s.startsWith("/") ? "" : "/") + s;
				}

				// Add the dispatcher marks before the URL map pattern if
				// any.
				if (!StringUtils.isEmpty(dispatchers))
					s = dispatchers
							+ FilterDelegate.DISPATCHERS_PARTTERN_SEPERATOR + s;

				if (urlParttern == null)
					urlParttern = s;
				else
					urlParttern = urlParttern
							+ FilterDelegate.MULTI_MAP_SEG_SEPERATOR + s;
			}

			// Search for multiple servlet map.
			for (Iterator servletMapIt = pEle1.elementIterator("servlet-name"); servletMapIt
					.hasNext();) {
				String s = ((Element) servletMapIt.next()).getTextTrim();
				if (StringUtils.isEmpty(s))
					continue;

				// Add the dispatcher marks before the url map parttern if
				// any.
				if (!StringUtils.isEmpty(dispatchers))
					s = dispatchers
							+ FilterDelegate.DISPATCHERS_PARTTERN_SEPERATOR + s;

				if (s != null && s.length() != 0) {
					if (targetServletNames == null)
						targetServletNames = s;
					else
						targetServletNames = targetServletNames
								+ FilterDelegate.MULTI_MAP_SEG_SEPERATOR + s;
				}
			}
			FilterDelegate info = new FilterDelegate(filter);
			info.targetServletNames = targetServletNames;
			info.setRawURLPatterns(urlParttern);
			info.setBundleContext(servletContext);
			servletContext.getFilters().add(info);
			// Do not init filter, we init them later before servelts.
			// info.init(null);
		}
	}

	/**
	 * @param httpService
	 * @param wb
	 * @throws ClassNotFoundException
	 * @throws NamespaceException
	 * @throws ServletException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	private void initServlets(Document doc) throws ClassNotFoundException,
			ServletException, NamespaceException, InstantiationException,
			IllegalAccessException {
		// servlet: servlet-name servlet-class init-param
		// servlet-mapping: servlet-name url-pattern
		for (Iterator i = doc.getRootElement().elementIterator("servlet"); i
				.hasNext();) {
			Element pEle = (Element) i.next();
			String servletName = pEle.elementTextTrim("servlet-name");
			if (servletName == null || servletName.length() == 0)
				continue;

			// Do not forget the jsp file element.<jsp-file>/abc.jsp</jsp-file>
			String clazzName = pEle.elementTextTrim("servlet-class");
			String jspFilePath = null;
			if (clazzName == null || clazzName.length() == 0) {
				jspFilePath = pEle.elementTextTrim("jsp-file");
				if (jspFilePath == null && jspFilePath.length() > 0)
					continue;
			}

			int loadOnSetupPriority = 0;

			try {
				loadOnSetupPriority = Integer.parseInt(pEle
						.elementTextTrim("load-on-startup"));
			} catch (Exception e) {
				// e.printStackTrace();
			}

			Hashtable<String, String> params = new Hashtable<String, String>();

			for (Iterator i0 = pEle.elementIterator("init-param"); i0.hasNext();) {
				Element pEle0 = (Element) i0.next();
				String paramName = pEle0.elementTextTrim("param-name");
				String paramValue = pEle0.elementTextTrim("param-value");
				if (!StringUtils.isEmpty(paramName) && paramValue != null)
					params.put(paramName, paramValue);
			}

			String urlParttern = null;
			for (Iterator i1 = doc.getRootElement().elementIterator(
					"servlet-mapping"); i1.hasNext();) {
				Element pEle1 = (Element) i1.next();
				String mapName = pEle1.elementTextTrim("servlet-name");
				if (mapName == null || mapName.length() == 0
						|| !mapName.equals(servletName))
					continue;

				// Parse multiple URL-Pattern setting unders a servlet-mapping
				// element.
				for (Iterator urlPatternIt = pEle1
						.elementIterator("url-pattern"); urlPatternIt.hasNext();) {
					Element urlPatternEle = (Element) urlPatternIt.next();
					String s = urlPatternEle.getTextTrim();
					if (s == null || s.length() == 0)
						continue;

					// if a IFwkConstants.SYS_PATH_PREFIX prefixed, not insert
					// bundle's service prefix.
					if (s.startsWith(IFwkConstants.SYS_PATH_PREFIX)) {
						s = s.substring(IFwkConstants.SYS_PATH_PREFIX.length());
					} else if (!servletContext.isHostBundle()) {
						// insert the namespace prefix of this web component to
						// each url map parttern.
						s = this.getWebContextPath()
								+ (s.startsWith("/") ? "" : "/") + s;
					}

					if (urlParttern == null)
						urlParttern = s;
					else
						urlParttern = urlParttern
								+ FilterDelegate.MULTI_MAP_SEG_SEPERATOR + s;
				}
			}

			ServletDelegate info = new ServletDelegate();
			info.name = servletName;
			info.className = clazzName;
			info.jspFile = jspFilePath;
			info.setRawURLPatterns(urlParttern);
			info.parameters = params;
			info.loadOnSetupPriority = loadOnSetupPriority;
			info.setBundleContext(servletContext);
			servletContext.getServletsInfo().add(info);
		}
	}

	public void log(IOException e) {
		FwkActivator.getInstance().log(e);
	}

	public void log(int severity, int code, String message, Throwable t) {
		FwkActivator.getInstance().log(severity, code, message, t);
	}

	public void log(Status status) {
		FwkActivator.getInstance().log(status);
	}
}
