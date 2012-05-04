/*******************************************************************************
 * Copyright (c) 2010 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.resources.extension;

/**
 * The base resource class representing a resource which can be published. Usually single file or
 * folder.
 * 
 * @author Leo Chang - EMRYS
 * @version 2010-10-11
 */
public class BaseResource {
	/**
	 * the resource's path
	 */
	String path;
	/**
	 * the resource's alias
	 */
	String alias;
	/**
	 * the resource's quickID
	 */
	String quickID;
	/**
	 * the resource's resolverID
	 */
	String resolverID;
	/**
	 * the resource's visit controller.
	 */
	IResourceVisitController visitControler;
	/**
	 * the resource's resolver.
	 */
	IPublishedFileResolver resolver;

	/**
	 * @return
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @param path
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * @return
	 */
	public String getAlias() {
		return alias;
	}

	/**
	 * @param alias
	 */
	public void setAlias(String alias) {
		this.alias = alias;
	}

	/**
	 * @return
	 */
	public IResourceVisitController getVisitControler() {
		return visitControler;
	}

	/**
	 * @param visitControler
	 */
	public void setVisitControler(IResourceVisitController visitControler) {
		this.visitControler = visitControler;
	}

	/**
	 * @return
	 */
	public String getQuickID() {
		return quickID;
	}

	/**
	 * @param quickID
	 */
	public void setQuickID(String quickID) {
		this.quickID = quickID;
	}

	/**
	 * @return
	 */
	public String getResolverID() {
		return resolverID;
	}

	/**
	 * @param resolverID
	 */
	public void setResolverID(String resolverID) {
		this.resolverID = resolverID;
	}

	/**
	 * @return
	 */
	public IPublishedFileResolver getResolver() {
		return resolver;
	}

	/**
	 * @param resolver
	 */
	public void setResolver(IPublishedFileResolver resolver) {
		this.resolver = resolver;
	}
}
