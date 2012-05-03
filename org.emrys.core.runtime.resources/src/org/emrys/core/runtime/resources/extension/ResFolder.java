/*******************************************************************************
 * Copyright (c) 2010 Hirisun Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Hirisun License v1.0
 * which accompanies this distribution, and is available at
 * http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.resources.extension;

import java.util.ArrayList;
import java.util.List;

/**
 * The folfer resource.
 * 
 * @author Leo Chang - Hirisun
 * @version 2010-10-11
 */
public class ResFolder extends BaseResource {
	/**
	 * The folder resource fillters.
	 */
	private List<ResFilter> filters;
	/**
	 * the sub resources.
	 */
	private ArrayList<BaseResource> resources;

	/**
	 * @return the sub resources.
	 */
	public List<BaseResource> getResources() {
		if (resources == null)
			resources = new ArrayList<BaseResource>();
		return resources;
	}

	/**
	 * @return the folder resource filters.
	 */
	public List<ResFilter> getFilter() {
		if (filters == null)
			filters = new ArrayList<ResFilter>();
		return filters;
	}
}
