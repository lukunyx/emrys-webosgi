/**
 * 
 */
package org.emrys.core.runtime.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.emrys.common.ComActivator;
import org.emrys.common.ComponentCore;
import org.emrys.common.IComponentCore;
import org.emrys.core.runtime.FwkActivator;
import org.emrys.core.runtime.IFwkRuntime;
import org.emrys.core.runtime.WebComActivator;
import org.emrys.core.runtime.jeecontainer.OSGiJEEContainer;

import org.emrys.core.adapter.internal.ServletContextAdapter;
import org.emrys.core.launcher.internal.FwkExternalAgent;

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
	 * the instance of OSGiJEEContainer
	 */
	private OSGiJEEContainer jeeContainer;
	/**
	 * Framwork properties buffer.
	 */
	private final Map<String, Object> frameworkProperties;

	/**
	 * ComponentCore instance to delegate its methods.
	 */
	IComponentCore componentCore = ComponentCore.getInstance();

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
	}

	public void init(Map<String, Object> fwkAttr) {
		frameworkProperties.putAll(fwkAttr);
	}

	/**
	 * Start Framework. Many works have been done in FrameworkActivator
	 * allready.
	 */
	public void start() {
		FwkExternalAgent.getInstance().regiesterFwkDelegateServlet(
				SERVLET_TYPE_HTTP, getJeeContainer());
	}

	public void stop() {
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
	public void setFrameworkProperty(String name, Object value) {
		if (value == null && name != null)
			frameworkProperties.remove(name);
		if (name != null)
			frameworkProperties.put(name, value);
	}

	/**
	 * @return OSGiJEEContainer instance.
	 */
	public OSGiJEEContainer getJeeContainer() {
		return jeeContainer;
	}

	/**
	 * Only for internal use, other component should not use this method.
	 * 
	 * @param jeeContainer
	 */
	public void setJeeContainer(OSGiJEEContainer jeeContainer) {
		if (this.jeeContainer != null)
			throw new IllegalArgumentException(
					"The singleton JEE Container has been set already.");
		this.jeeContainer = jeeContainer;
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.emrys.common.IComponentCore#addBundleActivatorEntry(
	 * java.lang.Long, com.EMRYS.components.common.ComponentActivator)
	 */
	public void addBundleActivatorEntry(Long bundleId,
			ComActivator componentActivator) {
		componentCore.addBundleActivatorEntry(bundleId, componentActivator);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.emrys.common.IComponentCore#getAllComponentActivators()
	 */
	public Collection<ComActivator> getAllComponentActivators() {
		return componentCore.getAllComponentActivators();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.emrys.common.IComponentCore#getBundleActivator(long)
	 */
	public ComActivator getBundleActivator(long bundleId) {
		return componentCore.getBundleActivator(bundleId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.emrys.common.IComponentCore#getInvalidCompnents()
	 */
	public ComActivator[] getInvalidCompnents() {
		return componentCore.getInvalidCompnents();
	}
}
