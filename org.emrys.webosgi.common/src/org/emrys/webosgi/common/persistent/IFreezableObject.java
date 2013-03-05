package org.emrys.webosgi.common.persistent;

import java.util.Map;

/**
 * 
 * @author Leo Chang
 * @version 2011-3-17
 */
public interface IFreezableObject extends org.eclipse.core.runtime.IAdaptable {
	/**
	 * 方法说明：
	 * 
	 * @return String
	 */
	String getSessionId();

	/**
	 * 方法说明：
	 * 
	 * @param deep
	 *            boolean
	 * @return map
	 */
	Map<String, String> getFreezableDataStore(boolean deep);

	/**
	 * 方法说明：
	 * 
	 * @param data
	 *            map
	 * @param deep
	 *            boolean
	 */
	void setUnfrozenData(Map<String, String> data, boolean deep);
}
