/**
 * 
 */
package org.emrys.core.runtime;

import java.util.Map;

import org.emrys.common.IComponentCore;
import org.emrys.core.runtime.jeecontainer.OSGiJEEContainer;


/**
 * @author Leo Chang
 * 
 */
public interface IFwkRuntime extends IComponentCore, IFwkConstants {

	void init(Map<String, Object> fwkAttr);

	void start();

	void stop();

	Object getFrameworkAttribute(String name);

	void setFrameworkProperty(String name, Object value);

	OSGiJEEContainer getJeeContainer();

	String getHostWebBundleSymbleName();

	WebComActivator getHostBundleActivator();
}
