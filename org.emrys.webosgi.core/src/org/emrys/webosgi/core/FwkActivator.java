package org.emrys.webosgi.core;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.emrys.webosgi.common.ComActivator;
import org.emrys.webosgi.common.ComponentCore;
import org.emrys.webosgi.core.classloader.WabClassLoaderFactory;
import org.emrys.webosgi.core.extension.IEarlyStarter;
import org.emrys.webosgi.core.internal.FrameworkCoreCmdService;
import org.emrys.webosgi.core.internal.FwkRuntime;
import org.emrys.webosgi.core.jsp.JspServletPool;
import org.emrys.webosgi.core.logger.StdLogService;
import org.emrys.webosgi.core.runtime.OSGiWebContainer;
import org.emrys.webosgi.core.service.IOSGiWebContainer;
import org.emrys.webosgi.launcher.internal.FwkExternalAgent;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public final class FwkActivator extends ComActivator implements IFwkConstants,
		ServiceTrackerCustomizer {

	private static final String PLUGIN_ID = "org.emrys.webosgi.core";
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
	private OSGiWebContainer jeeContainer;

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
		INSTANCE = this;
		super.start(context);
		// Invoke extended early starters.
		FwkRuntime.getInstance().invokeEarlyStarters(
				IEarlyStarter.WHEN_BEFORE_FWK_START);

		// switch workspace according to the extension point.
		switchWorkspaceLocation();
		packageAdminTracker = new ServiceTracker(context, PackageAdmin.class
				.getName(), this);
		packageAdminTracker.open();

		httpServiceTracker = new HttpServiceTracker(context);
		httpServiceTracker.open();

		jeeContainer = new OSGiWebContainer();
		// Register the IOSGiJEEContainer Service
		getBundle().getBundleContext().registerService(
				IOSGiWebContainer.class.getName(), jeeContainer, null);

		// start log service.
		StdLogService.start();
		log("Workspace Located:" + Platform.getInstanceLocation().getURL(), 0,
				false);
		FwkRuntime.getInstance().setJeeContainer(jeeContainer);
		getFwkRuntime().start();

		// Register framework commands service.
		this.getBundle().getBundleContext().registerService(
				CommandProvider.class.getName(), new FrameworkCoreCmdService(),
				null);

		// Invoke extended early starters.
		FwkRuntime.getInstance().invokeEarlyStarters(
				IEarlyStarter.WHEN_AFTER_FWK_STARTED);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		JspServletPool.destroyAllServlets();
		StdLogService.stop();
		WabClassLoaderFactory.clean();
		httpServiceTracker.close();
		httpServiceTracker = null;
		packageAdminTracker.close();
		packageAdminTracker = null;
		super.stop(context);
	}

	public IFwkRuntime getFwkRuntime() {
		return FwkRuntime.getInstance();
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
	public OSGiWebContainer getJeeContainer() {
		return jeeContainer;
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

	public void bundleChanged(BundleEvent event) {
		// Only after the Fwk started, this listener will work.
		/*
		 * if (!isWabsInitCompleted()) return;
		 */

		int et = event.getType();
		Bundle bundle = event.getBundle();

		if (et == BundleEvent.STOPPED) {
			ComActivator activator = FwkRuntime.getInstance()
					.getBundleActivator(bundle.getBundleId());
			if (activator instanceof WebComActivator) {
				WebComActivator webActivator = (WebComActivator) activator;
				jeeContainer.unregServletContext(webActivator
						.getBundleServletContext());
				try {
					jeeContainer.refresh();
					FwkActivator.getInstance().log(
							"Removed web bundle service: "
									+ webActivator.getBundleSymbleName(), 0,
							false);
				} catch (Exception e) {
					// e.printStackTrace();
					FwkActivator.getInstance().log(e);
				}
			}
		}

		if (et == BundleEvent.STARTED) {
			ComActivator activator = FwkRuntime.getInstance()
					.getBundleActivator(bundle.getBundleId());
			if (activator instanceof WebComActivator) {
				WebComActivator webActivator = (WebComActivator) activator;
				if (!webActivator.isWebServiceStarted()) {
					try {
						webActivator.startApplication();
						jeeContainer.refresh();
					} catch (Exception e) {
						// e.printStackTrace();
						FwkActivator.getInstance().log(e);
					}
				}
			}
		}
	}

	/**
	 * HttpServcie tracker
	 * 
	 * @author Leo Chang
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
