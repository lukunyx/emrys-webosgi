package org.emrys.webosgi.core.resource.extension;

import java.util.ArrayList;
import java.util.List;

/**
 * The folfer resource.
 * 
 * @author Leo Chang
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
