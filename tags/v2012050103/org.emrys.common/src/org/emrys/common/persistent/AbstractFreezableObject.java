/*******************************************************************************
 * Copyright (c) 2011 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.common.persistent;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author Leo Chang - EMRYS
 * @version 2011-3-17
 */
public abstract class AbstractFreezableObject implements IFreezableObject {
	/**
	 * 
	 */
	private Map<String, String> deepUnfrozenData = null;
	/**
	 * 
	 */
	private Map<String, String> showllowUnfrozenData = null;

	/**
	 *@param deep
	 *            String
	 * @return Map
	 */
	public Map<String, String> getFreezableDataStore(boolean deep) {
		if (deep)
			return getDeepUnfrozenData();
		else
			return getShowllowUnfrozenData();
	}

	/**
	 *@param data
	 *            String
	 * @param deep
	 *            deep
	 */
	public void setUnfrozenData(Map<String, String> data, boolean deep) {
		if (deep) {
			deepUnfrozenData = data;
		} else {
			showllowUnfrozenData = data;
		}
	}

	/**
	 * 鏂规硶璇存槑锛�
	 * 
	 * @return map
	 */
	public Map<String, String> getDeepUnfrozenData() {
		if (deepUnfrozenData == null)
			deepUnfrozenData = new HashMap<String, String>();
		;
		return deepUnfrozenData;
	}

	public void setDeepUnfrozenData(Map<String, String> deepUnfrozenData) {
		this.deepUnfrozenData = deepUnfrozenData;
	}

	/**
	 * 鏂规硶璇存槑锛�
	 * 
	 * @return map
	 */
	public Map<String, String> getShowllowUnfrozenData() {
		if (showllowUnfrozenData == null)
			showllowUnfrozenData = new HashMap<String, String>();
		return showllowUnfrozenData;
	}

	public void setShowllowUnfrozenData(Map<String, String> showllowUnfrozenData) {
		this.showllowUnfrozenData = showllowUnfrozenData;
	}
}
