/**
 * 
 */
package org.emrys.webosgi.core.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.eclipse.osgi.service.resolver.BaseDescription;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.State;
import org.emrys.webosgi.common.ComActivator;
import org.emrys.webosgi.common.ComponentCore;
import org.emrys.webosgi.common.IComActivator;
import org.emrys.webosgi.common.IComponentCore;
import org.emrys.webosgi.common.util.BundleServiceUtil;
import org.emrys.webosgi.core.FwkActivator;
import org.emrys.webosgi.core.IFwkRuntime;
import org.emrys.webosgi.core.ServiceInitException;
import org.emrys.webosgi.core.WebComActivator;
import org.emrys.webosgi.core.extender.WABDeployer;
import org.emrys.webosgi.core.extender.Watcher4ActivatorWAB;
import org.emrys.webosgi.core.extender.Watcher4ExtPointWAB;
import org.emrys.webosgi.core.extender.Watcher4ManifestMarkedWab;
import org.emrys.webosgi.core.extension.IEarlyStarter;
import org.emrys.webosgi.core.service.IOSGiWebContainer;
import org.emrys.webosgi.core.service.IWebApplication;
import org.emrys.webosgi.launcher.internal.FwkExternalAgent;
import org.emrys.webosgi.launcher.internal.adapter.ServletContextAdapter;
import org.ops4j.pax.swissbox.extender.BundleWatcher;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * @author Leo Chang
 */
public final class FwkRuntime implements IFwkRuntime {
	/**
	 * the singleton instance
	 */
	private static FwkRuntime INSTANCE;

	/**
	 * the host web bundle id buffer.
	 */
	private long hostWebBundleId = 0l;
	/**
	 * the instance of IOSGiWebContainer
	 */
	private IOSGiWebContainer webContainer;
	/**
	 * Framwork properties buffer.
	 */
	private final Map<String, Object> frameworkProperties;

	/**
	 * ComponentCore instance to delegate its methods.
	 */
	IComponentCore componentCore = ComponentCore.getInstance();

	Vector<WABDeployer> deployers;

	List<IWebApplication> applications = new ArrayList<IWebApplication>();

	private BundleWatcher[] bundleWatchers;

	private final List<WebAppStartInfo> startingAppInfos = new ArrayList<WebAppStartInfo>();

	/**
	 * Help class to store the WebApp starting information.
	 * 
	 * @author LeoChang
	 * 
	 */
	private static class WebAppStartInfo {
		private final Long threadId;
		private final IWebApplication webApp;

		WebAppStartInfo(Long threadId, IWebApplication webApp) {
			this.threadId = threadId;
			this.webApp = webApp;
		}

		IWebApplication getApp() {
			return webApp;
		}

		Long getThreadId() {
			return threadId;
		}

		@Override
		public boolean equals(Object o) {
			boolean b = super.equals(o);
			if (b)
				return true;
			if (!(o instanceof WebAppStartInfo))
				return false;
			return this.hashCode() == o.hashCode();
		}

		@Override
		public int hashCode() {
			return 17 + getApp().hashCode() * 3 + getThreadId().hashCode() * 5;
		}
	}

	private final BundleContext fwkBdCtx;
	/**
	 * indicating whether all web service initialization of web bundles'
	 * started.
	 */
	private boolean fwkEarlyInitializing;

	/**
	 * indicating whether all web service initialization of web bundles'
	 * completed.
	 */
	private boolean fwkEarlyInitCompleted;

	private final ComActivator fwkActivator;

	/**
	 * Get the singleton instance of FrameworkCore
	 * 
	 * @return
	 */
	public static FwkRuntime getInstance() {
		if (INSTANCE == null)
			INSTANCE = new FwkRuntime();
		return INSTANCE;
	}

	/**
	 * Hide the constructor.
	 */
	private FwkRuntime() {
		// some initialization...
		frameworkProperties = new HashMap<String, Object>();
		fwkActivator = FwkActivator.getInstance();
		fwkBdCtx = fwkActivator.getContext();
	}

	public void init(Map<String, Object> fwkAttr) {
		frameworkProperties.putAll(fwkAttr);
	}

	/**
	 * Start Framework, do some initialization, such as start wab watching.
	 */
	public void start() {
		FwkExternalAgent.getInstance().regiesterFwkDelegateServlet(
				SERVLET_TYPE_HTTP, getWebContainer());

		// Start all wab watcher.
		Watcher4ManifestMarkedWab manifestMarkedWABWatcher = new Watcher4ManifestMarkedWab(
				fwkBdCtx, this);
		Watcher4ExtPointWAB extpointWABWatcher = new Watcher4ExtPointWAB(
				fwkBdCtx, this);
		Watcher4ActivatorWAB activatorWabWatcher = new Watcher4ActivatorWAB(
				fwkBdCtx, this);
		this.bundleWatchers = new BundleWatcher[] { manifestMarkedWABWatcher,
				extpointWABWatcher, activatorWabWatcher };

		for (BundleWatcher watcher : bundleWatchers) {
			watcher.start();
		}
	}

	public void stop() {
		for (BundleWatcher watcher : bundleWatchers) {
			watcher.stop();
		}
		FwkExternalAgent.getInstance().regiesterFwkDelegateServlet(
				SERVLET_TYPE_HTTP, null);
	}

	/**
	 * Get the framework attribute by given name
	 * 
	 * @param name
	 * @return
	 */
	public Object getFrameworkAttribute(String name) {
		// Case get OSGi platform install directory.
		if (name.equals(ATTR_FWK_INSTALL_DIR)) {
			return new Path(Platform.getInstallLocation().getURL().getFile())
					.toPortableString();
		}
		// Case get Framework's Servlet Context path.
		if (name.equals(ATTR_FWK_SERVLET_CTX_PATH)) {
			ServletContextAdapter globalServletCtx = FwkExternalAgent
					.getInstance().getFwkServletContext(SERVLET_TYPE_HTTP);
			if (globalServletCtx != null)
				return globalServletCtx.getContextPath();
		}

		// Search from buffered properties.
		Object value = frameworkProperties.get(name);
		if (value != null)
			return value;
		// Search from top FwkExternalAgent.
		return FwkExternalAgent.getInstance().getFwkEvnAttribute(name);
	}

	/**
	 * Set a framework property.
	 * 
	 * @param name
	 * @param value
	 */
	public void setFrameworkAttribute(String name, Object value) {
		if (value == null && name != null)
			frameworkProperties.remove(name);
		if (name != null)
			frameworkProperties.put(name, value);
	}

	public IOSGiWebContainer getWebContainer() {
		return webContainer;
	}

	/**
	 * Only for internal use, other component should not use this method.
	 * 
	 * @param webContainer
	 */
	public void setJeeContainer(IOSGiWebContainer webContainer) {
		if (this.webContainer != null)
			throw new IllegalArgumentException(
					"The singleton JEE Container has been set already.");
		this.webContainer = webContainer;
	}

	/**
	 * @return the host web bundle's symble name.
	 */
	public String getHostWebBundleSymbleName() {
		if (hostWebBundleId == 0l)
			throw new IllegalStateException("The Host Web Bundle not exists.");
		return FwkActivator.getInstance().getBundle().getBundleContext()
				.getBundle(hostWebBundleId).getSymbolicName();
	}

	/**
	 * Set the host web bundle's id. Only for internal use.
	 * 
	 * @param hostWebBundleId
	 */
	public void setHostWebBundleId(long hostWebBundleId) {
		if (this.hostWebBundleId != 0l)
			throw new IllegalArgumentException(
					"The singleton Host Web Bundle has been set already.");
		this.hostWebBundleId = hostWebBundleId;
	}

	/**
	 * @return host web bundle's Activator instance.
	 */
	public WebComActivator getHostBundleActivator() {
		if (hostWebBundleId == 0l)
			return null;
		return (WebComActivator) getBundleActivator(hostWebBundleId);
	}

	public void addBundleActivatorEntry(Long bundleId,
			ComActivator componentActivator) {
		componentCore.addBundleActivatorEntry(bundleId, componentActivator);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.emrys.webosgi.common.IComponentCore#getAllComponentActivators()
	 */
	public Collection<ComActivator> getAllComponentActivators() {
		return componentCore.getAllComponentActivators();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.emrys.webosgi.common.IComponentCore#getBundleActivator(long)
	 */
	public ComActivator getBundleActivator(long bundleId) {
		return componentCore.getBundleActivator(bundleId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.emrys.webosgi.common.IComponentCore#getInvalidCompnents()
	 */
	public ComActivator[] getInvalidCompnents() {
		return componentCore.getInvalidCompnents();
	}

	public WABDeployer[] getWABDeployers() {
		if (deployers == null) {
			deployers = new Vector<WABDeployer>();

			// Collect deployer from Extension point.
			IExtensionPoint extensionPoint = RegistryFactory
					.getRegistry()
					.getExtensionPoint(
							fwkActivator.getBundleSymbleName() + ".wabDeployer");
			IConfigurationElement[] eles = extensionPoint
					.getConfigurationElements();
			final Map<IEarlyStarter, Integer> map = new HashMap<IEarlyStarter, Integer>();
			for (IConfigurationElement ele : eles) {
				if (ele.getName().equals("deployer")) {
					try {
						WABDeployer deployer = (WABDeployer) ele
								.createExecutableExtension("class");
						deployers.add(deployer);
					} catch (Exception e) {
						// e.printStackTrace();
						fwkActivator.log(e);
					}
				}
			}
		}
		return deployers.toArray(new WABDeployer[deployers.size()]);
	}

	public void registerWABDeployer(WABDeployer deployer) {
		// Try to collect extension point deployers and init deployers.
		getWABDeployers();
		deployers.add(deployer);
	}

	public void unregisterWABDeployer(WABDeployer deployer) {
		if (deployers != null)
			deployers.remove(deployer);
	}

	public boolean isOSGiEmbedded() {
		return getFrameworkAttribute(FwkRuntime.ATTR_FWK_OSGI_EMBEDDED).equals(
				Boolean.TRUE);
	}

	public void initEarlyStartWabs() {
		if (fwkEarlyInitializing || fwkEarlyInitCompleted)
			return;

		fwkEarlyInitializing = true;
		// Invoke early starter before JEE Service initialization.
		invokeEarlyStarters(IEarlyStarter.WHEN_BEFORE_JEE_START);

		// init debug options of the framework. Default debug mode is close,
		// skip initialization.
		// switchDebugOption(true);

		// Start JEE Web Bundle from Extension Point. The JEE Container will
		// listen the bundle start event and start its web services.
		startJeeExtBundles();

		// Start left wab service not started.
		Collection<ComActivator> activators = FwkRuntime.getInstance()
				.getAllComponentActivators();
		List<WebComActivator> wabActivators = new ArrayList<WebComActivator>();
		for (ComActivator activator : activators) {
			if (activator instanceof WebComActivator)
				wabActivators.add((WebComActivator) activator);
		}

		// Sort and make sure the wabs dependencies order.
		// Do not use Collections.sort(...)
		WebComActivator[] wabActArray = wabActivators
				.toArray(new WebComActivator[wabActivators.size()]);
		final State state = BundleServiceUtil.getPlatformAdmin()
				.getState(false);
		for (int i = 0; i < wabActArray.length;) {
			int lastMatched = i;
			for (int j = i + 1; j < wabActArray.length; j++) {
				boolean isBd1DependentBd2 = false;
				BundleDescription bundleDes1 = state.getBundle(wabActArray[i]
						.getBundle().getBundleId());
				BundleSpecification[] requiredBundleDeses = bundleDes1
						.getRequiredBundles();
				for (BundleSpecification bundleSpec : requiredBundleDeses) {
					BaseDescription supplier = bundleSpec.getSupplier();
					if (supplier instanceof BundleDescription
							&& ((BundleDescription) supplier).getBundleId() == wabActArray[j]
									.getBundle().getBundleId()) {
						isBd1DependentBd2 = true;
						break;
					}
				}

				if (isBd1DependentBd2)
					lastMatched = j;
			}
			if (lastMatched != i) {
				// Move forward
				WebComActivator tmp = wabActArray[i];
				for (int p = i; p <= lastMatched - 1; p++)
					wabActArray[p] = wabActArray[p + 1];
				wabActArray[lastMatched] = tmp;
			} else
				i++;
		}

		try {
			for (WebComActivator wabActivator : wabActArray) {
				try {
					if (!wabActivator.isWebServiceStarted())
						wabActivator.startApplication();
				} catch (Throwable t) {
					// e.printStackTrace();
					fwkActivator.log(t);
				}
			}
		} finally {
			try {
				webContainer.refresh();
				WebComActivator hostWebActivator = FwkRuntime.getInstance()
						.getHostBundleActivator();
				// If fwk isn't launched as osgi embedded mode, we need to check
				// if the host bundle exists.
				Object osgiEmbeddedLauncher = this
						.getFrameworkAttribute(ATTR_FWK_OSGI_EMBEDDED);
				if (osgiEmbeddedLauncher == null
						|| osgiEmbeddedLauncher.equals(Boolean.FALSE)) {
					if (hostWebActivator == null
							|| !hostWebActivator.isWebServiceStarted()) {
						throw new ServiceInitException(
								new Status(
										Status.ERROR,
										fwkActivator.getBundleSymbleName(),
										"Host Web Component not started. The framework maybe not able to work normally."));
					}
				}

				// At last, Invoke early starter after JEE Service initialized.
				// We need do it in s asynchronized mode for some early starter
				// may need the JEE Container inistialized, this may cause dead
				// wait.
				new Thread(new Runnable() {
					public void run() {
						try {
							invokeEarlyStarters(IEarlyStarter.WHEN_AFTER_JEE_STARTED);
						} catch (Exception e) {
							// e.printStackTrace();
							fwkActivator.log(e);
						}
					}
				}).start();

				fwkEarlyInitCompleted = true;
				fwkEarlyInitializing = false;
			} catch (Exception e) {
				// e.printStackTrace();
				fwkActivator.log(e);
			}
		}
	}

	public void invokeEarlyStarters(int when) {
		IExtensionPoint extensionPoint = RegistryFactory.getRegistry()
				.getExtensionPoint(
						fwkActivator.getBundleSymbleName() + ".EarlyStarter");
		IConfigurationElement[] eles = extensionPoint
				.getConfigurationElements();
		final Map<IEarlyStarter, Integer> map = new HashMap<IEarlyStarter, Integer>();
		for (IConfigurationElement ele : eles) {
			if (ele.getName().equals("starter")) {
				try {
					// Only when specified starter be created and invoked.
					if (when != Integer.parseInt(ele.getAttribute("when")))
						continue;

					IEarlyStarter starter = (IEarlyStarter) ele
							.createExecutableExtension("class");
					int priority = Integer.parseInt(ele
							.getAttribute("priority"));
					map.put(starter, priority);
				} catch (Exception e) {
					// e.printStackTrace();
					fwkActivator.log(e);
				}
			}
		}

		// Sort starters by their priority integer, the smaller one is be
		// invoked at first.
		List<IEarlyStarter> starters = new ArrayList<IEarlyStarter>(map
				.keySet());

		Collections.sort(starters, new Comparator<IEarlyStarter>() {
			public int compare(IEarlyStarter o1, IEarlyStarter o2) {
				return (map.get(o1) > map.get(o2)) ? -1 : 0;
			}
		});

		for (IEarlyStarter starter : starters) {
			try {
				starter.start();
			} catch (Exception e) {
				fwkActivator.log(e);
			}
		}
	}

	/**
	 * Start all registered JavaEE Service bundle from extension point
	 * plugin_ID.jeeSvcContribution. This procedure may need to be optimized use
	 * lazy start and only initialize JavaEE meta data.
	 */
	private void startJeeExtBundles() {
		IExtensionPoint extPoint = Platform.getExtensionRegistry()
				.getExtensionPoint(
						fwkActivator.getBundleSymbleName()
								+ ".jeeSvcContribution");
		IConfigurationElement[] ces = extPoint.getConfigurationElements();
		for (IConfigurationElement ce : ces) {
			Bundle declaringBundle = null;
			try {
				String bundleId = ((RegistryContributor) (ce.getContributor()))
						.getId();
				declaringBundle = fwkBdCtx.getBundle(Long.parseLong(bundleId));
				int state = declaringBundle.getState();
				if (state != Bundle.ACTIVE)
					declaringBundle.start();
			} catch (Exception e) {
				// e.printStackTrace();
				if (declaringBundle != null)
					fwkActivator.log(IComActivator.LOG_ERROR, 0, "Web Bundle["
							+ declaringBundle.getSymbolicName()
							+ "] start failed.", e);
				else
					fwkActivator.log(e);
			}
		}
	}

	/**
	 * @return whether all web service initialization of Web Application bundles
	 *         completed.
	 */
	public boolean isFwkInited() {
		return fwkEarlyInitCompleted;
	}

	public IWebApplication getAppliction(Bundle wabundle) {
		// To adaptable with our former ComActivator mechanism.
		ComActivator activator = this
				.getBundleActivator(wabundle.getBundleId());
		if (activator instanceof IWebApplication)
			return (IWebApplication) activator;

		// Search applications registration.
		for (IWebApplication app : applications) {
			if (wabundle.equals(app.getWebBundle()))
				return app;
		}
		return null;
	}

	public void regApplication(IWebApplication app) throws ServiceInitException {
		if (!applications.contains(app)) {
			app.init();
			applications.add(app);
		}
	}

	public void unregApplication(IWebApplication app) {
		applications.remove(app);
	}

	public void enterAppSvcStart(IWebApplication webApp) {
		synchronized (startingAppInfos) {
			Long threadId = Thread.currentThread().getId();
			startingAppInfos.add(new WebAppStartInfo(threadId, webApp));
		}
	}

	public void quitAppSvcStart(IWebApplication webApp) {
		synchronized (startingAppInfos) {
			// Remove the starting webApp from the end.
			Long threadId = Thread.currentThread().getId();
			int lastIdx = startingAppInfos.lastIndexOf(new WebAppStartInfo(
					threadId, webApp));
			if (lastIdx > -1)
				startingAppInfos.remove(lastIdx);
		}
	}

	boolean isAppDynaStarting(IWebApplication webApp, boolean curThread) {
		// Remove the starting webApp from the end.
		Long threadId = Thread.currentThread().getId();
		if (curThread)
			return startingAppInfos.contains(new WebAppStartInfo(threadId,
					webApp));
		else {
			WebAppStartInfo[] array = startingAppInfos
					.toArray(new WebAppStartInfo[startingAppInfos.size()]);
			for (WebAppStartInfo i : array) {
				if (i.getApp().equals(webApp))
					return true;
			}
		}
		return false;
	}

	public boolean makeSureWabActive(IWebApplication webApp) {
		try {
			if (webApp.isDynaServicesStarted())
				return true;

			// If this invoke thread is do web app starting, return true
			// directly to avoid dead-circle.
			if (isAppDynaStarting(webApp, true))
				return true;

			Bundle wabundle = webApp.getWebBundle();
			int wabStatus = wabundle.getState();
			if (wabStatus == Bundle.RESOLVED
					|| (wabStatus == Bundle.ACTIVE && !webApp
							.isDynaServicesStarted())) {
				// If this web app is not dynanic starting by other thread,
				// active or just start this app's service.
				if (!isAppDynaStarting(webApp, false)) {
					if (wabStatus == Bundle.RESOLVED) {
						// Start web bundle will triger the WebApp starting in a
						// while.
						wabundle.start();
						// Wait for the app starting.
						int interval = 0;
						while (!isAppDynaStarting(webApp, false)
								&& !webApp.isDynaServicesStarted()
								&& interval < 15000) {
							interval += 15;
							Thread.sleep(15);
						}
					} else
						webApp.startDynamicServices();
				}

				// Wait for the app complete in timeout.
				int interval = 0;
				while (isAppDynaStarting(webApp, false) && interval < 600000) {
					interval += 25;
					Thread.sleep(25);
				}

				return webApp.isDynaServicesStarted();
			} else if (wabStatus != Bundle.ACTIVE)
				throw new IllegalStateException(
						"Web Bundle status invalid to handle dymamic resource");
		} catch (Exception e) {
			FwkActivator.getInstance().log(e);
		}

		return false;
	}
}
