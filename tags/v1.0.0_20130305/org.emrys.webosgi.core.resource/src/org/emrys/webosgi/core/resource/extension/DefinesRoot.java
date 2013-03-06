package org.emrys.webosgi.core.resource.extension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;

/**
 * 
 * @author Leo Chang
 * @version 2010-10-11
 */
public class DefinesRoot {
	String id;
	String name;
	private IResourceVisitController visitControler;
	private List<BaseResource> resources;
	private final Map<String, IPublishedFileResolver> resResolvers = new HashMap<String, IPublishedFileResolver>();

	protected Bundle sourceBundle;

	public IResourceVisitController getVisitControler() {
		return visitControler;
	}

	public void setVisitControler(IResourceVisitController visitControler) {
		this.visitControler = visitControler;
	}

	public List<BaseResource> getResources() {
		if (resources == null)
			resources = new ArrayList<BaseResource>();
		return resources;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, IPublishedFileResolver> getResResolvers() {
		return resResolvers;
	}

	public Bundle getSourceBundle() {
		return sourceBundle;
	}

	public void setSourceBundle(Bundle sourceBundle) {
		this.sourceBundle = sourceBundle;
	}
}
