package org.emrys.webosgi.core;

import java.io.File;
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
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.emrys.webosgi.common.ComActivator;
import org.emrys.webosgi.common.IComActivator;
import org.emrys.webosgi.common.util.BundleServiceUtil;
import org.emrys.webosgi.core.internal.FwkRuntime;
import org.emrys.webosgi.core.jeeres.FilterDelegate;
import org.emrys.webosgi.core.jeeres.FilterDelegateBase;
import org.emrys.webosgi.core.jeeres.ListenerInfo;
import org.emrys.webosgi.core.jeeres.ServletDelegate;
import org.emrys.webosgi.core.jeeres.TaglibInfo;
import org.emrys.webosgi.core.runtime.WabServletContext;
import org.emrys.webosgi.core.service.IOSGiWebContainer;
import org.emrys.webosgi.core.service.IWABServletContext;
import org.emrys.webosgi.core.service.IWebApplication;
import org.emrys.webosgi.core.util.WebBundleUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.NamespaceException;

/**
 * The Activator class of web bundle which has some web service to publish by
 * WebOSGi framework, such as servlet, jsp, static resources or event a whole
 * normal Java EE project without any modification(in this case, may be a
 * subclass of this is can be benefited from}.
 * 
 * @author Leo Chang
 */
public class WebComActivator extends ComActivator implements IWebComActivator,
		IWebApplication {
	/**
	 * the bundle ID of this component.
	 */
	private String bundleID = null;
	/**
	 * Indicating whether the web service started.
	 */
	private boolean webServiceStartDone = false;
	/**
	 * Indicating whether this component is running in a JEE container.
	 */
	private Boolean isHostWebBundle = null;
	/**
	 * the Bundled ServletContext of this web component.
	 */
	private IWABServletContext servletContext;
	/**
	 * the singleton instance of OSGiWebContainer
	 */
	protected IOSGiWebContainer webContainer;
	/**
	 * Web Content Root file buffered.
	 */
	protected File webContentRoot;
	/**
	 * the buffered host web bundle's prefix.
	 */
	private static String hostWebBundleNsPrefix;

	/**
	 * Indicating that whether the http service available.
	 */
	private final Boolean isHttpServiceAvailable = null;
	private String nsPrefix;
	private boolean triedToStartFailed;

	@Override
	public final void start(BundleContext context) throws Exception {
		super.start(context);
		try {
			// always invoke startComponent() method though most of them is
			// empty.
			bundleID = getBundleSymbleName();
			startComponent(context);
			if (isHttpServiceAvailable()) {
				earlyStartWebService();
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * Do some early start work when bundle be started. By default, here will
	 * register the host web bundle to JEE bridge Servlet of WebOSGi framework,
	 * so the framework's parent class loader can wrapper the class load
	 * procedure back to the host web bundle. In this manner, the special
	 * requirement to circle dependent between host web bundle and its dependent
	 * bundle can be satisfied.
	 */
	protected void earlyStartWebService() {
		// If this is the web host bundle, set its bundle class
		// loader(DefaultClassLoader) to osgi
		// framework's parent classloader. And if this bundle's dependent bundle
		// need to refer the
		// class and resources in this bundle, the osgi frameworks class loader
		// will wired the class
		// loade process back to here.
		if (isHostWebBundle()) {
			FwkRuntime.getInstance().setHostWebBundleId(
					getBundle().getBundleId());
		}
	}

	public File findWebContentRoot(boolean forceUpdate) {
		if (webContentRoot == null || forceUpdate) {
			try {
				webContentRoot = WebBundleUtil.getExtractedWebContentRoot(
						getBundle(), forceUpdate).toFile();
			} catch (IOException e) {
				// e.printStackTrace();
				log(e);
			}
		}
		return webContentRoot;
	}

	public IOSGiWebContainer getWebContainer() {
		return this.webContainer;
	}

	public void setServiceNSPrefix(String nsPrefix) {
		// Host web app not allow to set new nsPrefix.
		if (!this.isHostWebBundle() && StringUtils.isNotBlank(nsPrefix))
			this.nsPrefix = nsPrefix.trim();
	}

	public String getServiceNSPrefix() {
		if (StringUtils.isNotBlank(nsPrefix))
			return nsPrefix;

		// If this web bundle is a host JavaEE bridge app, its service namespace
		// prefix is empty string.
		if (this.isHostWebBundle()) {
			return "";
		} else {
			// Else, generate it.
			Path bundleIdPath = new Path(bundleID.replace('.', '/'));
			if (bundleIdPath.segmentCount() == 1)
				return bundleID;
			String defaultNSPrefix = bundleIdPath.lastSegment().equals("web") ? bundleIdPath
					.segment(bundleIdPath.segmentCount() - 2)
					: bundleIdPath.lastSegment();
			return defaultNSPrefix;
		}
	}

	public BundleContext getBundleContext() {
		return this.getContext();
	}

	public IWABServletContext getBundleServletContext() {
		if (servletContext == null) {
			servletContext = new WabServletContext(this);
		}
		return servletContext;
	}

	public void setWebContextPath(String ctxPath) {
		// Do nothing for a component activator.
	}

	public String getWebContextPath() {
		return "/" + getServiceNSPrefix();
	}

	public void init() {
		// Parse JavaEE Configuration to servlet context.
	}

	public void pubStaticResources() {
		// Not any static resources published by default.
	}

	public void startDynamicServices() {
		startApplication();
	}

	public void stopDynamicServices() {
		this.webContainer.unregServletContext(this.getBundleServletContext());
		webServiceStartDone = false;
		servletContext = null;
	}

	public boolean isStaticResPublished() {
		return false;
	}

	public boolean isDynaServicesStarted() {
		return isWebServiceStarted();
	}

	/**
	 * The Framework Listener in FrameworkActivator listen to the
	 * FrameworkEvent.STARTED event to start Web Service of this web bundle by
	 * invoke this method. The starting process will be triggered once Framework
	 * wholly started, that is say, all bundles except system bundle be started
	 * if possible.
	 */
	public void startApplication() {
		synchronized (this) {
			// Only call once.
			if (!isWebServiceStarted() && !triedToStartFailed) {
				try {
					triedToStartFailed = true;
					checkDoubleNsPrefix();
					startWebService(context);
					webServiceStartDone = true;
					WebComActivator.this.log(new Status(Status.INFO,
							WebComActivator.this.getBundleSymbleName(),
							"Web Service in bundle ID:"
									+ WebComActivator.this
											.getBundleSymbleName() + " ["
									+ WebComActivator.this.getServiceNSPrefix()
									+ "] published OK."));

				} catch (Throwable t) {
					// t.printStackTrace();
					WebComActivator.this.log(new Status(Status.INFO,
							WebComActivator.this.getBundleSymbleName(),
							"Web Service in bundle ID:"
									+ WebComActivator.this
											.getBundleSymbleName() + " ["
									+ WebComActivator.this.getServiceNSPrefix()
									+ "] published Failed.", t));
				}
			}
		}
	}

	public boolean isHostWebBundle() {
		if (isHostWebBundle == null)
			isHostWebBundle = WebBundleUtil.isHostWebBundle(getBundle());
		return isHostWebBundle == null ? false : isHostWebBundle;
	}

	public boolean isWebServiceStarted() {
		return webServiceStartDone;
	}

	/**
	 * If this bundle not in a JEE Container, start common OSGi component in
	 * this method.
	 * 
	 * @param context
	 */
	protected void startComponent(BundleContext context) {
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		// the OSGiWebContainer is listening the bundle event to process the
		// removing of thi bundle.
		// here it seems no need to do as following.
		if (webContainer != null)
			webContainer.unregServletContext(getBundleServletContext());
		super.stop(context);
	}

	/**
	 * Start jee web service of this bundle, like servlet, jsp or publishing
	 * other static resource.
	 * 
	 * @param context
	 * @throws Exception
	 */
	protected void startWebService(BundleContext context)
			throws ServiceInitException {
		// Add this Bundle Servlet Context to IOSGiJeeContainer.
		ServiceReference svcRef = getBundle().getBundleContext()
				.getServiceReference(IOSGiWebContainer.class.getName());
		webContainer = (IOSGiWebContainer) getBundle().getBundleContext()
				.getService(svcRef);
		servletContext = getBundleServletContext();

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

		try {
			initOnStartServlet();
			if (!webContainer.getAllBundledServletContext().contains(
					servletContext)) {
				webContainer.regServletContext(servletContext);
				webContainer.activeServletContext(servletContext);
			}
		} catch (Exception e) {
			throw new ServiceInitException(new Status(Status.ERROR,
					getBundleSymbleName(), "Web Service init failed.", e));
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
	private void checkDoubleNsPrefix() throws ServiceInitException {
		// check double named Name Space and its prefix.
		Bundle[] bundles = context.getBundles();
		for (Bundle b : bundles) {
			if (b.getState() == Bundle.ACTIVE) {
				try {
					IComActivator activator = BundleServiceUtil
							.getBundleActivator(b);
					if (activator instanceof IWebComActivator
							&& activator != this) {
						IWebComActivator webBundleActivator = (IWebComActivator) activator;
						String nsPrefix = getServiceNSPrefix();
						if (nsPrefix != null
								&& webBundleActivator.getServiceNSPrefix()
										.equals(nsPrefix)) {
							throw new ServiceInitException(
									new Status(
											Status.ERROR,
											getBundleSymbleName(),
											"Service NS Prefix initialized failed for not define or double named:"
													+ this
															.getBundleSymbleName()
													+ " ["
													+ webBundleActivator
															.getServiceNSPrefix()
													+ "]"));
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					if (e instanceof ServiceInitException)
						throw (ServiceInitException) e;
				}
			}
		}
	}

	protected InputStream getWebXmlInput() throws Exception {
		return WebBundleUtil.getWebXmlContent(getBundle());
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
			try {
				webContainer.regServletContext(servletContext);
				webContainer.activeServletContext(servletContext);
			} catch (Exception e) {
				// Process exception and then try to remove servlet context.
				try {
					webContainer.unregServletContext(servletContext);
				} catch (Throwable t) {
					// t.printStackTrace();
				} finally {
					// e.fillInStackTrace();
					throw e;
				}
			}

			initJspConfigs(doc);

			// Init servlet filter and init each and init them.
			initServletContextFilters(doc);

			// Init servelts config and later we need init each servlet by its
			// load-on-setup paramater.
			initServlets(doc);

			initOthers(doc);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServiceInitException(new Status(Status.ERROR, this
					.getBundleSymbleName(),
					"Initialize Web.xml configre failed["
							+ this.getBundleSymbleName() + "].", e));
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
					getBundleServletContext().getWelcomePages().add(page);
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
					getBundleServletContext().getErrorPages().put(
							Integer.parseInt(code), location);
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
						log(LOG_ERROR, 0,
								"Session timeout to big. Use default timeout.",
								new IllegalAccessException(
										"session-timeout too big"));
					} else {
						// session-timeout unit is minutes, and context need it
						// to be seconds.
						getBundleServletContext().setSessionTimeout(
								interval * 60);
					}
				} catch (NumberFormatException e) {
					log(LOG_ERROR, 0, "Session timeout invalid.", e);
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
					info.location = "/" + this.getServiceNSPrefix()
							+ (location.startsWith("/") ? "" : "/") + location;
					getBundleServletContext().getTaglibs().add(info);
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
				getBundleServletContext().setInitParameter(name, value);
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
				info.setBundleContext(this.getBundleServletContext());
				getBundleServletContext().getListeners().add(info);
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
				} else if (s != null && !this.isHostWebBundle()) {
					// insert the namespace prefix of this web component to
					// each
					// url map parttern.
					s = "/" + this.getServiceNSPrefix()
							+ (s.startsWith("/") ? "" : "/") + s;
				}

				// Add the dispatcher marks before the url map parttern if
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

			// Search for mutiple servlet map.
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
			info.setBundleContext(this.getBundleServletContext());
			getBundleServletContext().getFilters().add(info);
			// We init filter here before servelts.
			info.init(null);
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
					} else if (!this.isHostWebBundle()) {
						// insert the namespace prefix of this web component to
						// each
						// url map parttern.
						s = "/" + this.getServiceNSPrefix()
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
			info.setBundleContext(this.getBundleServletContext());
			getBundleServletContext().getServletsInfo().add(info);
		}
	}

	public Bundle getWebBundle() {
		return getBundle();
	}
}
