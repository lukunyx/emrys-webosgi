package org.emrys.core.runtime;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.emrys.common.ComActivator;
import org.emrys.common.ComponentCore;
import org.emrys.core.runtime.classloader.BundledClassLoaderFactory;
import org.emrys.core.runtime.extension.IEarlyStarter;
import org.emrys.core.runtime.internal.FrameworkCoreCmdService;
import org.emrys.core.runtime.internal.FwkRuntime;
import org.emrys.core.runtime.jeecontainer.IOSGiWebContainer;
import org.emrys.core.runtime.jeecontainer.OSGiJEEContainer;
import org.emrys.core.runtime.logger.StdLogService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import org.emrys.core.launcher.internal.FwkExternalAgent;

public final class FwkActivator extends ComActivator implements IFwkConstants,
		ServiceTrackerCustomizer {

	private static final String PLUGIN_ID = "org.emrys.core.runtime";
	/**
	 * the singleton instance of this class
	 */
	private static FwkActivator INSTANCE;
	/**
	 * the instance of Package Admin Service
	 */
	private static PackageAdmin packageAdmin;

	/**
	 * http service tracker
	 */
	private HttpServiceTracker httpServiceTracker;
	/**
	 * Package Admin Service tracker
	 */
	private ServiceTracker packageAdminTracker;
	/**
	 * OSGiJEEContainer instance.
	 */
	private OSGiJEEContainer jeeContainer;
	/**
	 * indicating whether all web service of web bundles' are initialized.
	 */
	private boolean earlyInitialized;

	/**
	 * @return the singleton instance of this class.
	 */
	public static FwkActivator getInstance() {
		return INSTANCE;
	}

	/**
	 * @return the instance of Package Admin Service.
	 */
	public static PackageAdmin getPackageAdmin() {
		return packageAdmin;
	}

	@Override
	public String getBundleSymbleName() {
		// We cann't let it get from context like the super does for early
		// starter may be invoked before this bundle started.
		return PLUGIN_ID;
	}

	@Override
	public void start(BundleContext context) throws Exception {

		// Invoke extended early starters.
		invokeEarlyStarters(IEarlyStarter.WHEN_BEFORE_FWK_START);

		INSTANCE = this;
		super.start(context);
		// switch workspace according to the extension point.
		switchWorkspaceLocation();
		packageAdminTracker = new ServiceTracker(context, PackageAdmin.class
				.getName(), this);
		packageAdminTracker.open();

		httpServiceTracker = new HttpServiceTracker(context);
		httpServiceTracker.open();

		jeeContainer = new OSGiJEEContainer();
		// Register the IOSGiJEEContainer Service
		getBundle().getBundleContext().registerService(
				IOSGiWebContainer.class.getName(), jeeContainer, null);

		// Register JEE Container as Bundle Listener to support dynimically
		// update of web bundles after the fwk initialized.
		getBundle().getBundleContext().addBundleListener(jeeContainer);

		// OSGi Equinox3.4 Runtime of IBM domino8.5.x not emit
		// FrameworkEvent.STARTED event, we use a listening thread to monitor
		// the system bundle's state.
		/*
		 * Thread sysBundleLisThread = new Thread(new Runnable() { public void
		 * run() { Bundle sysBundle =
		 * FwkActivator.this.getContext().getBundle(0); try { // FIXME: It seems
		 * that Equinox of Domino8.5.3 not change // system bundle's state as we
		 * expect. The ACTIVE state // isn't swapped at the end, but in early
		 * time. while (sysBundle.getState() != Bundle.ACTIVE) {
		 * Thread.sleep(25); } // Start web bundle. earlyInitOnFwkStarted(); }
		 * catch (InterruptedException e) { e.printStackTrace(); } } });
		 * sysBundleLisThread.start();
		 */

		/*
		 * getBundle().getBundleContext().addFrameworkListener( new
		 * FrameworkListener() { public void frameworkEvent(FrameworkEvent
		 * event) { // FrameworkEvent.INFO output throwable exception when //
		 * bundle starting peroid. // In Equinox3.4 used by IBM domino8.5.x, the
		 * fwk // started event not emit as equinox3.5. if (event.getType() ==
		 * FrameworkEvent.STARTED) { // Start web bundle.
		 * earlyInitOnFwkStarted(); } } });
		 */

		// start log service.
		StdLogService.start();

		log("Workspace Located:" + Platform.getInstanceLocation().getURL(), 0,
				false);

		FwkRuntime.getInstance().setJeeContainer(jeeContainer);
		getFwkRuntime().start();

		// register framework commands service.
		this.getBundle().getBundleContext().registerService(
				CommandProvider.class.getName(), new FrameworkCoreCmdService(),
				null);

		// Start Registered Jee Bundle from Extension Point.
		// FIXME: This procedure need to be optimized to lazy-loading.
		// startJeeSVCBundles();

		// Invoke extended early starters.
		invokeEarlyStarters(IEarlyStarter.WHEN_AFTER_FWK_STARTED);
	}

	protected void invokeEarlyStarters(int when) {
		IExtensionPoint extensionPoint = RegistryFactory
				.getRegistry()
				.getExtensionPoint(this.getBundleSymbleName() + ".EarlyStarter");
		IConfigurationElement[] eles = extensionPoint
				.getConfigurationElements();
		for (IConfigurationElement ele : eles) {
			if (ele.getName().equals("starter")) {
				try {
					// Only when specified starter be created and invoked.
					if (when != Integer.parseInt(ele.getAttribute("when")))
						continue;

					IEarlyStarter starter = (IEarlyStarter) ele
							.createExecutableExtension("class");
					starter.start();
				} catch (Exception e) {
					// e.printStackTrace();
					log(e);
				}
			}
		}
	}

	public IFwkRuntime getFwkRuntime() {
		return FwkRuntime.getInstance();
	}

	/**
	 * start the second period of starting proces of web bundles.
	 */
	public void earlyInitOnFwkStarted() {
		if (earlyInitialized)
			return;
		// Invoke early starter before JEE Service initialization.
		invokeEarlyStarters(IEarlyStarter.WHEN_BEFORE_JEE_START);

		// init debug options of the framework.
		switchDebugOption(true);
		// Start Jee SVC Bundle from Extension Point.
		startJeeExtBundles();
		Collection<ComActivator> activators = FwkRuntime.getInstance()
				.getAllComponentActivators();
		ComActivator[] aArr = activators.toArray(new ComActivator[activators
				.size()]);
		try {
			for (int i = 0; i < aArr.length; i++) {
				ComActivator activator = aArr[i];
				if (activator instanceof WebComActivator) {
					try {
						((WebComActivator) activator).start2ndPeriod();
					} catch (Throwable t) {
						// e.printStackTrace();
						this.log(t);
					}
				}
			}
		} finally {
			try {
				jeeContainer.refresh();
				earlyInitialized = true;

				WebComActivator hostWebActivator = FwkRuntime.getInstance()
						.getHostBundleActivator();
				// If fwk isn't launched as osgi embedded mode, we need to check
				// if the host bundle exists.
				Object osgiEmbeddedLauncher = getFwkRuntime()
						.getFrameworkAttribute(ATTR_FWK_OSGI_EMBEDDED);
				if (osgiEmbeddedLauncher == null
						|| osgiEmbeddedLauncher.equals(Boolean.FALSE)) {
					if (hostWebActivator == null
							|| !hostWebActivator.isWebServiceStarted()) {
						throw new ServiceInitException(
								new Status(
										Status.ERROR,
										getInstance().getBundleSymbleName(),
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
							log(e);
						}
					}
				}).start();

			} catch (Exception e) {
				// e.printStackTrace();
				this.log(e);
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
						this.getBundleSymbleName() + ".jeeSvcContribution");
		IConfigurationElement[] ces = extPoint.getConfigurationElements();
		for (IConfigurationElement ce : ces) {
			Bundle declaringBundle = null;
			try {
				String bundleId = ((RegistryContributor) (ce.getContributor()))
						.getId();
				declaringBundle = this.getContext().getBundle(
						Long.parseLong(bundleId));
				int state = declaringBundle.getState();
				if (state != Bundle.ACTIVE) {
					declaringBundle.start();
					ComActivator activator = FwkRuntime.getInstance()
							.getBundleActivator(declaringBundle.getBundleId());
					if (activator instanceof WebComActivator) {
						WebComActivator webActivator = (WebComActivator) activator;
						try {
							webActivator.start2ndPeriod();
							jeeContainer.refresh();
						} catch (Exception e) { // e.printStackTrace();
							FwkActivator.getInstance().log(e);
						}
					}
				}
			} catch (Exception e) {
				// e.printStackTrace();
				if (declaringBundle != null)
					log(LOG_ERROR, 0, "JavaEE Service Bundle["
							+ declaringBundle.getSymbolicName()
							+ "] start failed.", e);
				else
					log(e);
			}
		}
	}

	/**
	 * switch the framework's debug mode.
	 */
	public void switchDebugOption(boolean openDebug) {
		// Not use OSGi Debug Options service.
		/*
		 * ServiceTracker debugTracker = new ServiceTracker(context,
		 * DebugOptions.class.getName(), null); debugTracker.open();
		 * DebugOptions debugOptions = (DebugOptions) debugTracker.getService();
		 */

		// Open the switch of debug for all registered components.
		Collection<ComActivator> components = ComponentCore.getInstance()
				.getAllComponentActivators();
		for (Iterator<ComActivator> it = components.iterator(); it.hasNext();) {
			ComActivator activator = it.next();
			activator.setDebugging(openDebug);
		}

		// debugOptions.setDebugEnabled(openDebug);
	}

	/**
	 * Get singleton instance of OSGiJEEContainer
	 * 
	 * @return
	 */
	public OSGiJEEContainer getJeeContainer() {
		return jeeContainer;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		getBundle().getBundleContext().removeBundleListener(jeeContainer);
		StdLogService.stop();
		BundledClassLoaderFactory.clean();
		httpServiceTracker.close();
		httpServiceTracker = null;
		packageAdminTracker.close();
		packageAdminTracker = null;
		super.stop(context);
	}

	/**
	 * Relocate the workspace directory according to the extension point sorting
	 * by the priority. If not any extension point available, the default
	 * workspace directory will be applied as
	 * "SERVER_WORK_DIR/local/WEB_APP_NAME/meta-info/eclipse/workspace/".
	 */
	protected void switchWorkspaceLocation() {
		if (Platform.getInstanceLocation().getURL() != null)
			return;

		IExtensionPoint extPoint = RegistryFactory
				.getRegistry()
				.getExtensionPoint(this.getBundleSymbleName() + ".workspaceDef");
		IConfigurationElement[] ces = extPoint.getConfigurationElements();
		int maxPriority = -1;
		String workspaceDir = null;
		for (IConfigurationElement ce : ces) {
			if (!ce.getName().equals("workspace"))
				continue;

			int priority = 0;
			try {
				priority = Integer.parseInt(ce.getAttribute("priority"));
			} catch (Exception e) {
			}

			if (priority > maxPriority) {
				maxPriority = priority;
				workspaceDir = ce.getAttribute("target-dir");
				// String name = ce.getAttribute("name");
			}
		}

		// Internal Directory Attributes: ${webroot-dir} ${platform-dir}
		// "server-tmp-dir"

		if (workspaceDir != null && workspaceDir.length() > 0) {
			String platformDir = new Path(Platform.getInstallLocation()
					.getURL().getFile()).toPortableString();
			workspaceDir = workspaceDir.replace("${platform-dir}", platformDir);

			String webRootDir = (String) FwkExternalAgent.getInstance()
					.getFwkEvnAttribute(
							FwkExternalAgent.ATTR_FWK_WEBAPP_DEPLOY_PATH);
			workspaceDir = workspaceDir.replace("${webroot-dir}", new Path(
					webRootDir).toPortableString());

			// NOTE: get form top servlet context attribute:
			// "javax.servlet.context.tempdir", the actual type if java.io.File.
			String serverTmpDir = ((File) FwkExternalAgent.getInstance()
					.getFwkEvnAttribute(FwkExternalAgent.ATTR_JEE_WORK_DIR))
					.getAbsolutePath();
			workspaceDir = workspaceDir.replace("${server-tmp-dir}",
					serverTmpDir);

			try {
				Platform.getInstanceLocation().set(
						new Path(workspaceDir).toFile().toURI().toURL(), false);
			} catch (Exception e) {
				// e.printStackTrace();
				log(LOG_DEBUG, 0, "Framework's workspace specify failed.", e);
			}
		}
	}

	/**
	 * @return HttpServiceTracker
	 */
	public HttpServiceTracker getHttpServiceTracker() {
		return httpServiceTracker;
	}

	public Object addingService(ServiceReference reference) {
		synchronized (FwkActivator.class) {
			packageAdmin = (PackageAdmin) context.getService(reference);
		}
		return packageAdmin;
	}

	public void modifiedService(ServiceReference reference, Object service) {
	}

	public void removedService(ServiceReference reference, Object service) {
		synchronized (FwkActivator.class) {
			context.ungetService(reference);
			packageAdmin = null;
		}
	}

	/**
	 * @param clazz
	 * @return
	 */
	public static synchronized Bundle getBundle(Class clazz) {
		if (packageAdmin == null)
			throw new IllegalStateException("Not started"); //$NON-NLS-1$

		return packageAdmin.getBundle(clazz);
	}

	public static Bundle[] getFragments(Bundle bundle) {
		if (packageAdmin == null)
			throw new IllegalStateException("Not started"); //$NON-NLS-1$

		return packageAdmin.getFragments(bundle);
	}

	/**
	 * Method for internal use to get apache jasper bundle.
	 * 
	 * @return
	 */
	public static Bundle getJasperBundle() {
		Bundle bundle = getBundle(org.apache.jasper.servlet.JspServlet.class);
		if (bundle != null)
			return bundle;

		if (INSTANCE == null)
			throw new IllegalStateException("Not started"); //$NON-NLS-1$

		ExportedPackage[] exportedPackages = packageAdmin
				.getExportedPackages("org.apache.jasper.servlet"); //$NON-NLS-1$
		for (int i = 0; i < exportedPackages.length; i++) {
			Bundle[] importingBundles = exportedPackages[i]
					.getImportingBundles();
			for (int j = 0; j < importingBundles.length; j++) {
				if (INSTANCE.getBundle().equals(importingBundles[j]))
					return exportedPackages[i].getExportingBundle();
			}
		}
		return null;
	}

	/**
	 * @return whether all web service are initialized in web bundles.
	 */
	public boolean isFwkEarlyInitialized() {
		return earlyInitialized;
	}

	/**
	 * HttpServcie tracker
	 * 
	 * @author Leo Chang - EMRYS
	 * @version 2011-2-26
	 */
	public static class HttpServiceTracker extends ServiceTracker {
		private HttpContext httpContext;
		private HttpService httpService;

		public HttpServiceTracker(BundleContext context) {
			super(context, HttpService.class.getName(), null);
		}

		@Override
		public Object addingService(ServiceReference reference) {
			httpService = (HttpService) context.getService(reference);
			return httpService;
		}

		@Override
		public void removedService(ServiceReference reference, Object service) {
			super.removedService(reference, service);
		}

		public HttpContext getHttpContext() {
			return httpContext;
		}

		public void setHttpContext(HttpContext httpContext) {
			this.httpContext = httpContext;
		}

		public HttpService getHttpService() {
			return httpService;
		}

		public void setHttpService(HttpService httpService) {
			this.httpService = httpService;
		}

		@Override
		public void open(boolean trackAllServices) {
			super.open(trackAllServices);
		}

		@Override
		public Object waitForService(long timeout) throws InterruptedException {
			return super.waitForService(timeout);
		}
	}
}
