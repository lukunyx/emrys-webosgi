package org.emrys.core.runtime;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.emrys.common.ComActivator;
import org.emrys.common.IComActivator;
import org.emrys.common.util.BundleServiceUtil;
import org.emrys.common.util.FileUtil;
import org.emrys.core.runtime.internal.FwkRuntime;
import org.emrys.core.runtime.jeecontainer.BundledServletContext;
import org.emrys.core.runtime.jeecontainer.IBundledServletContext;
import org.emrys.core.runtime.jeecontainer.IOSGiWebContainer;
import org.emrys.core.runtime.jeeres.FilterDelegate;
import org.emrys.core.runtime.jeeres.ListenerInfo;
import org.emrys.core.runtime.jeeres.ServletDelegate;
import org.emrys.core.runtime.jeeres.TaglibInfo;
import org.emrys.core.runtime.util.WebBundleUtil;
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
public abstract class WebComActivator extends ComActivator implements
		IWebComActivator {
	/**
	 * the bundle ID of this component.
	 */
	private String bundleID = null;
	/**
	 * Indicating whether the web service started.
	 */
	private boolean webServiceStarted = false;
	/**
	 * Indicating whether this component is running in a JEE container.
	 */
	private Boolean isHostWebBundle = null;
	/**
	 * the Bundled ServletContext of this web component.
	 */
	private IBundledServletContext servletContext;
	/**
	 * the singleton instance of OSGiJEEContainer
	 */
	protected IOSGiWebContainer jeeContainerSVC;
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

	/**
	 * The Framework Listener in FrameworkActivator listen to the
	 * FrameworkEvent.STARTED event to start Web Service of this web bundle by
	 * invoke this method. The starting process will be triggered once Framework
	 * wholly started, that is say, all bundles except system bundle be started
	 * if possible.
	 */
	public final void start2ndPeriod() {
		synchronized (this) {
			// Only call once.
			if (!isWebServiceStarted()) {
				try {
					checkDoubleNsPrefix();
					startWebService(context);
					webServiceStarted = true;

					WebComActivator.this.log(new Status(Status.INFO,
							WebComActivator.this.getBundleSymbleName(),
							"Web Service in bundle ID:"
									+ WebComActivator.this.getServiceNS()
									+ " ["
									+ WebComActivator.this.getServiceNSPrefix()
									+ "] published OK."));

				} catch (ServiceInitException e) {
					// e.printStackTrace();
					WebComActivator.this.log(new Status(Status.INFO,
							WebComActivator.this.getBundleSymbleName(),
							"Web Service in bundle ID:"
									+ WebComActivator.this.getServiceNS()
									+ " ["
									+ WebComActivator.this.getServiceNSPrefix()
									+ "] published Failed.", e));
				}
			}
		}
	}

	public boolean isHostWebBundle() {
		if (isHostWebBundle == null) {
			// the default host web bundle shoule has the same install path with
			// the framework. But the donwflow bundle can specify this feature
			// bu override this method. If multiple host bundles specified, the
			// first bundle will be applied.
			ServletContext globalCtx = getBundleServletContext()
					.getGlobalContext();
			String fwkServletCtxRoot = globalCtx.getRealPath("/");
			// If OSGi HttpService Embeded, this root path maybe null.
			if (fwkServletCtxRoot == null)
				return false;
			IPath fwkServletCtxRootPath = new Path(fwkServletCtxRoot);
			IPath thePath = null;// new
			// Path(getBundleServletContext().getRealPath("/"));
			try {
				thePath = new Path(FileLocator.getBundleFile(this.getBundle())
						.getAbsolutePath());
				if (fwkServletCtxRootPath.equals(thePath)) {
					isHostWebBundle = true;
				} else {
					isHostWebBundle = false;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return isHostWebBundle == null ? false : isHostWebBundle;
	}

	public boolean isWebServiceStarted() {
		return webServiceStarted;
	}

	/**
	 * If this bundle not in a JEE Container, start common OSGi component in
	 * this method.
	 * 
	 * @param context
	 */
	protected void startComponent(BundleContext context) {
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
		jeeContainerSVC = (IOSGiWebContainer) getBundle().getBundleContext()
				.getService(svcRef);
		servletContext = getBundleServletContext();
		// Get Web Root Directory and set into current context.
		File webContent = this.getResolvedWebContentRoot(false);
		if (webContent != null)
			servletContext.setWebRootFolder(new Path(webContent
					.getAbsolutePath()));

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
			if (!jeeContainerSVC.getAllBundledServletContext().contains(
					servletContext)) {
				jeeContainerSVC.addBundledServletContext(servletContext);
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

	@Override
	public void stop(BundleContext context) throws Exception {
		// the OSGiJEEContainer is listening the bundle event to process the
		// removing of thi bundle.
		// here it seems no need to do as following.
		if (jeeContainerSVC != null)
			jeeContainerSVC
					.removeBundledServletContext(getBundleServletContext());
		super.stop(context);
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
											getServiceNS(),
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

	public String getWebContentPath() {
		return null;
	}

	public File getResolvedWebContentRoot(boolean forceUpdate) {
		if (webContentRoot == null || forceUpdate) {
			if (forceUpdate && webContentRoot != null
					&& webContentRoot.exists()) {
				try {
					// Only when the bundle file is a .jar file, need to clear
					// WebContent Folder.
					File bundleFile = FileLocator.getBundleFile(this
							.getBundle());
					if (bundleFile.isFile()) {
						FileUtil.deleteAllFile(webContentRoot, null);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

			}

			// URL url = WebBundleUtil.findWebContentURL(getBundle());
			try {
				String webContentPath = getWebContentPath();
				File bundleFile = FileLocator.getBundleFile(getBundle());
				if (bundleFile.isDirectory()) {
					URL webContentUrl = null;
					if (webContentPath != null) {
						webContentUrl = getBundle().getEntry(webContentPath);
						webContentUrl = FileLocator.toFileURL(webContentUrl);
					} else
						webContentUrl = WebBundleUtil
								.findWebContentURL(getBundle());

					if (webContentUrl != null) {
						File webContentFile = new Path(webContentUrl.getPath())
								.toFile();
						if (webContentFile != null)
							webContentRoot = webContentFile;
					}

				} else if (bundleFile.isFile()/*
											 * &&
											 * bundleFile.getName().endsWith(".jar"
											 * )
											 */) {
					// External Component Jars will be copied to configure
					// directory by OSGi Framework when install.
					// However, the coppied file's name not end with .jar. Here
					// not regard a bunlde file only according to the name
					// ending with .jar.
					if (webContentPath == null)
						webContentPath = WebBundleUtil
								.findWebContentPath(getBundle());

					if (webContentPath != null) {
						webContentRoot = new Path(getComponentWorkspaceRoot()
								.getAbsolutePath()).append("WebContent")
								.toFile();
						FileUtil.unZipFile(bundleFile, webContentPath,
								webContentRoot);
					}
				}
			} catch (IOException e) {
				// e.printStackTrace();
				return null;
			}
		}

		return webContentRoot;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.hirisun.components.web.core.IWebComponentActivator#getServiceNS()
	 */
	public final String getServiceNS() {
		return bundleID;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.hirisun.components.web.core.IWebComponentActivator#getServiceNSPrefix
	 * ()
	 */
	public String getServiceNSPrefix() {
		if (this.isHostWebBundle()) {
			// if this is Host Web Bundle, use the ServletContext name as NS
			// prefix.
			if (hostWebBundleNsPrefix == null) {
				hostWebBundleNsPrefix = (String) FwkActivator.getInstance()
						.getFwkRuntime().getFrameworkAttribute(
								FwkRuntime.ATTR_FWK_SERVLET_CTX_PATH);
				hostWebBundleNsPrefix = hostWebBundleNsPrefix.substring(1);
			}
			return hostWebBundleNsPrefix;
		} else {
			Path bundleIdPath = new Path(bundleID.replace('.', '/'));
			if (bundleIdPath.segmentCount() == 1)
				return bundleID;
			String defaultNSPrefix = bundleIdPath.lastSegment().equals("web") ? bundleIdPath
					.segment(bundleIdPath.segmentCount() - 2)
					: bundleIdPath.lastSegment();
			return defaultNSPrefix;
		}
	}

	public File getWebResource(String path) {
		File webContentRoot = this.getResolvedWebContentRoot(false);
		if (webContentRoot == null)
			return null;

		IPath tmpPath = new Path(webContentRoot.getAbsolutePath());
		File targetFile = tmpPath.append(path).toFile();
		if (targetFile != null && targetFile.exists())
			return targetFile;
		return null;
	}

	public IBundledServletContext getBundleServletContext() {
		if (servletContext == null) {
			servletContext = new BundledServletContext(this);
		}
		return servletContext;
	}

	protected void initWebConfig() throws ServiceInitException {
		/*
		 * ServiceReference httpSvcRef =
		 * getBundle().getBundleContext().getServiceReference(
		 * HttpService.class.getName()); HttpService httpService = (HttpService)
		 * getBundle().getBundleContext().getService( httpSvcRef);
		 */
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
				jeeContainerSVC.addBundledServletContext(servletContext);
			} catch (Exception e) {
				// Process exception and then try to remove servlet context.
				try {
					jeeContainerSVC.removeBundledServletContext(servletContext);
				} catch (Throwable t) {
					// t.printStackTrace();
				} finally {
					// e.fillInStackTrace();
					throw e;
				}
			}

			initJspConfigs(doc);

			// Init servlet filter and init each.
			initServletContextFilters(doc);

			// Init servelts config and init each servlet by its load-on-setup
			// paramater.
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
					getBundleServletContext().setSessionTimeout(interval);
				} catch (NumberFormatException e) {
					log(LOG_ERROR, 0, "Session timeout invalid.", e);
				}
			}
		}
	}

	/**
	 * Get web.xml file as InputStream. Let sub class to provide singleton
	 * web.xml file, not only in a WebContent folder. By default, get
	 * /WEB-INF/web.xml file from WebContext forlder if any.
	 */
	protected InputStream getWebXmlInput() {
		try {
			File webContextRoot = getResolvedWebContentRoot(false);
			if (webContextRoot != null) {
				IPath rootPath = new Path(webContextRoot.getAbsolutePath());
				File webXmlFile = rootPath.append(
						isHostWebBundle() ? "/WEB-INF/web0.xml"
								: "/WEB-INF/web.xml").toFile();
				return new FileInputStream(webXmlFile);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
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
		 * <jsp-config> <taglib> <taglib-uri>www.hirisun.com/jsptag</taglib-uri>
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
			String value = pEle.elementText("param-value");
			if (name != null && value != null) {
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
		 * </filter-mapping>
		 */
		for (Iterator i = doc.getRootElement().elementIterator("filter"); i
				.hasNext();) {
			Element pEle = (Element) i.next();
			String filterName = pEle.elementTextTrim("filter-name");
			if (filterName == null || filterName.length() == 0)
				continue;

			String clazzName = pEle.elementTextTrim("filter-class");
			if (clazzName == null || clazzName.length() == 0)
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

			String targetServletNames = null;
			String urlParttern = null;
			String dispatchers = null;
			for (Iterator i1 = doc.getRootElement().elementIterator(
					"filter-mapping"); i1.hasNext();) {
				Element pEle1 = (Element) i1.next();
				String mapName = pEle1.elementTextTrim("filter-name");
				if (mapName == null || mapName.length() == 0
						|| !mapName.equals(filterName))
					continue;

				// Search for multiple URL-Pattern setting unders a
				// filter-mapping element.
				for (Iterator urlPatternIt = pEle1
						.elementIterator("url-pattern"); urlPatternIt.hasNext();) {
					String s = ((Element) urlPatternIt.next()).getTextTrim();
					// if a #{system} prefixed, not insert bundle's service
					// prefix.
					if (s.startsWith("#{system}")) {
						s = s.replace("#{system}", "");
					} else if (s != null && !this.isHostWebBundle()) {
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

				// Search for mutiple servlet map.
				for (Iterator servletMapIt = pEle1
						.elementIterator("servlet-name"); servletMapIt
						.hasNext();) {
					String s = ((Element) servletMapIt.next()).getTextTrim();
					if (s != null && s.length() != 0) {
						if (targetServletNames == null)
							targetServletNames = s;
						else
							targetServletNames = targetServletNames
									+ FilterDelegate.MULTI_MAP_SEG_SEPERATOR
									+ s;
					}
				}

				// Search for mutiple dispatchers map.
				for (Iterator dispatchersIt = pEle1
						.elementIterator("dispatcher"); dispatchersIt.hasNext();) {
					String s = ((Element) dispatchersIt.next()).getTextTrim();
					if (s != null && s.length() != 0) {
						if (dispatchers == null)
							dispatchers = s;
						else
							dispatchers = dispatchers
									+ FilterDelegate.MULTI_MAP_SEG_SEPERATOR
									+ s;
					}
				}
			}

			FilterDelegate info = new FilterDelegate();
			info.clazzName = clazzName;
			info.parameters = params;
			info.name = filterName;
			info.dispatcherNames = dispatchers;
			info.targetServletNames = targetServletNames;
			info.setRawURLPatterns(urlParttern);
			info.setBundleContext(this.getBundleServletContext());
			getBundleServletContext().getFilters().add(info);
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

			String clazzName = pEle.elementTextTrim("servlet-class");
			if (clazzName == null || clazzName.length() == 0)
				continue;

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
				if (paramName != null && paramName.length() > 0
						&& paramValue != null)
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

					// if a #{system} prefixed, not insert bundle's service
					// prefix.
					if (s.startsWith("#{system}")) {
						s = s.replace("#{system}", "");
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
			info.setRawURLPatterns(urlParttern);
			info.parameters = params;
			info.loadOnSetupPriority = loadOnSetupPriority;
			info.setBundleContext(this.getBundleServletContext());
			getBundleServletContext().getServletsInfo().add(info);
		}
	}
}
