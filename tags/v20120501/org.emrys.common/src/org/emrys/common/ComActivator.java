/*******************************************************************************
 * Copyright (c) 2011 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.common;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.emrys.common.license.License;
import org.emrys.common.license.LicenseManager;
import org.emrys.common.log.LogUtil;
import org.emrys.common.util.FileUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

import org.emrys.core.launcher.internal.FwkExternalAgent;

/**
 * The common Component's Activator for other components to benefit from. This
 * Activator will register the its singleton instance by invoking
 * {@link org.emrys.common.ComponentCore #addBundleActivatorEntry(Long, ComActivator)}
 * , and others can get the singleton instance by
 * {@link org.emrys.common.ComponentCore #getBundleActivator(long)}
 * method. What's more, this class support the common License Control Interface.
 * To get the valid license for this component, see {@link #getValidLicense()}
 * method.
 * 
 * @author Leo Chang
 * @version 2011-3-2
 */
public abstract class ComActivator extends Plugin implements IComActivator {
	private Boolean isHttpServiceAvailable = null;
	protected BundleContext context;
	protected File workspaceRoot;
	private License license;
	private boolean debug = true;

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		this.context = context;
		ComponentCore.getInstance().addBundleActivatorEntry(
				this.getBundle().getBundleId(), this);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		ComponentCore.getInstance().getAllComponentActivators().remove(
				getBundle().getBundleId());
	}

	public void log(String message, int code, boolean isDebug) {
		log(isDebug ? LOG_DEBUG : LOG_INFO, code, message, null);
	}

	public void log(Throwable t) {
		log(0, 0, null, t);
	}

	public void log(int severity, int code, String message, Throwable t) {
		Throwable top = t;
		if (t instanceof CoreException) {
			CoreException de = (CoreException) t;
			IStatus status = de.getStatus();
			if (status.getException() != null) {
				top = status.getException();
			}
		}

		if (severity == 0) {
			if (t != null)
				severity = LOG_ERROR;
		}
		log(new Status(severity, getBundleSymbleName(), code,
				message == null ? (t != null ? t.getLocalizedMessage() : "")
						: message, top));
	}

	/**
	 * Logs the specified status with this plug-in's log. Caution, this status
	 * severity code must be one of severity one of {@link #LOG_DEBUG}
	 * {@link #LOG_ERROR} {@link #LOG_INFO} {@link #LOG_WARNING} or other
	 * customized, if this status be log in a none eclipse platform osgi
	 * runtime.
	 * 
	 * @param status
	 *            status to log
	 */
	public void log(IStatus status) {
		if (!isHttpServiceAvailable())
			getLog().log(status);
		else {
			// Use OSGi Standard Log Service in JEE container then.
			try {
				ServiceReference[] logServices = getContext()
						.getServiceReferences(LogService.class.getName(), null);
				if (logServices == null)
					return;

				for (int i = 0; i < logServices.length; i++) {
					LogService log = (LogService) getContext().getService(
							logServices[i]);

					String message = LogUtil.genLogLine(status);
					int logServiceLevel = status.getSeverity();

					// Convert Status level to OSGi Logger Service Severity.
					switch (logServiceLevel) {
					case ComActivator.LOG_DEBUG:
						logServiceLevel = LogService.LOG_DEBUG;
						break;
					case ComActivator.LOG_ERROR:
						logServiceLevel = LogService.LOG_ERROR;
						break;
					case ComActivator.LOG_INFO:
						logServiceLevel = LogService.LOG_INFO;
						break;
					case ComActivator.LOG_WARNING:
						logServiceLevel = LogService.LOG_WARNING;
						break;
					}

					// Log the message to OSGi log service bus.
					log.log(logServiceLevel, message, status.getException());
				}
			} catch (InvalidSyntaxException e) {
				// e.printStackTrace();
			}
		}
	}

	public String getBundleSymbleName() {
		return context.getBundle().getSymbolicName();
	}

	public BundleContext getContext() {
		return context;
	}

	public File getComponentWorkspaceRoot() {
		if (workspaceRoot == null) {
			try {
				IPath tmpWebRoot = new Path(Platform.getInstanceLocation()
						.getURL().getPath()).append(this.getBundleSymbleName());
				workspaceRoot = tmpWebRoot.toFile();
				FileUtil.createFile(workspaceRoot, true);
			} catch (IOException e) {
				return null;
			}
		}

		return workspaceRoot;
	}

	public String[] getRequiredLicenseType() {
		// All components' default null license is EPL. Here requires null
		// license.
		return new String[0];
	}

	public String getLicenseConsumerID() {
		return this.getBundle().getSymbolicName()
				+ "_"
				+ getBundle().getHeaders().get(Constants.BUNDLE_VERSION)
						.toString()/* .getVersion() */;
	}

	public License getValidLicense() {
		// Check out whether has valid license bind to this component.
		if (license == null || !license.isValid()) {
			String[] requiredLicenseTypes = this.getRequiredLicenseType();
			// Create EPL license by default.
			if (requiredLicenseTypes == null
					|| requiredLicenseTypes.length == 0)
				return (license = LicenseManager.getInstance().epl);

			LicenseManager.getInstance().checkLicense(this);
		}
		return license;
	}

	public boolean isHttpServiceAvailable() {
		if (isHttpServiceAvailable == null) {
			Object servletCtx = FwkExternalAgent.getInstance()
					.getFwkServletContext(FwkExternalAgent.SERVLET_TYPE_HTTP);
			isHttpServiceAvailable = (servletCtx != null);
		}
		return isHttpServiceAvailable;
	}

	@Override
	public boolean isDebugging() {
		return debug;// super.isDebugging();
	}

	@Override
	public void setDebugging(boolean debug) {
		this.debug = debug;// super.isDebugging();
	}

	public void setValidLicense(License license) {
		this.license = license;
	}
}
