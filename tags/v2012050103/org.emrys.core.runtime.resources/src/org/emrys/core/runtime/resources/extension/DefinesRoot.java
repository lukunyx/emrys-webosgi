/*******************************************************************************
 * Copyright (c) 2010 EMRYS Corporation. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the EMRYS License v1.0 which accompanies this
 * distribution, and is available at http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.resources.extension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;

/**
 * 
 * @author Leo Chang - EMRYS
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
