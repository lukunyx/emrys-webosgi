/*******************************************************************************
 * Copyright (c) 2011 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.common.persistent;

import java.util.Map;

/**
 * 
 * @author Leo Chang - EMRYS
 * @version 2011-3-17
 */
public interface IFreezableObject extends org.eclipse.core.runtime.IAdaptable {
	/**
	 * 方法说明＄1�7
	 * 
	 * @return String
	 */
	String getSessionId();

	/**
	 * 方法说明＄1�7
	 * 
	 * @param deep
	 *            boolean
	 * @return map
	 */
	Map<String, String> getFreezableDataStore(boolean deep);

	/**
	 * 方法说明＄1�7
	 * 
	 * @param data
	 *            map
	 * @param deep
	 *            boolean
	 */
	void setUnfrozenData(Map<String, String> data, boolean deep);
}
