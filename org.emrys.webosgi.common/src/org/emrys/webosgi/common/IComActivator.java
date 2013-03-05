package org.emrys.webosgi.common;

import java.io.File;

import org.eclipse.core.runtime.Status;
import org.emrys.webosgi.common.license.LicenseConsumer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * The Component Activator provide some convenient interface.
 * 
 * @author Leo Chang
 * @version 2010-11-5
 */
public interface IComActivator extends BundleActivator, LicenseConsumer {
	/**
	 * Debug log type. Let the Status.CANCEL as debug mark in JEE Container.
	 */
	public static final int LOG_DEBUG = Status.CANCEL;
	public static final int LOG_ERROR = Status.ERROR;
	public static final int LOG_INFO = Status.INFO;
	public static final int LOG_WARNING = Status.WARNING;
	public static final int LOG_OPERATION = 0x13;

	/**
	 * Get the bundle context.
	 * 
	 * @return
	 */
	BundleContext getContext();

	/**
	 * Get this bundle's symble name.
	 * 
	 * @return
	 */
	String getBundleSymbleName();

	/**
	 * Get the workspace root directory for this bundle. Each component has a
	 * root directory to store its data in the current workspace.
	 * 
	 * @return
	 */
	File getComponentWorkspaceRoot();

	/**
	 * @return if this bundle activated in a JEE Container, not in Eclipse or
	 *         others.
	 */
	boolean isHttpServiceAvailable();

	/**
	 * Log a message maybe with a throwable exception, severity or additional
	 * code.
	 * 
	 * @param severity
	 *            one of {@link #LOG_DEBUG} {@link #LOG_ERROR} {@link #LOG_INFO}
	 *            {@link #LOG_WARNING} or other customized.
	 * @param code
	 *            the additional code of this log
	 * @param message
	 *            message of this log
	 * @param t
	 *            if this log caused by a throwable exception.
	 */
	void log(int severity, int code, String message, Throwable t);

	/**
	 * log the throwable exception as a debug message.
	 * 
	 * @param t
	 */
	void log(Throwable t);

	/**
	 * Log this message with additional code.
	 * 
	 * @param message
	 * @param code
	 *            customized additional code.
	 * @param isDebug
	 *            whether to log this message in debug mode.
	 */
	void log(String message, int code, boolean isDebug);
}
