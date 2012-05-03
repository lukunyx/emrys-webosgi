/*******************************************************************************
 * Copyright (c) 2011 Hirisun Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Hirisun License v1.0
 * which accompanies this distribution, and is available at
 * http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.jeeres;

import org.emrys.core.runtime.internal.FwkRuntime;
import org.emrys.core.runtime.jeecontainer.OSGiJEEContainer;

/**
 * The sortable and Cloned Servlet Object for Servlet Object such as servlet or
 * filter that may have many url pattern and should be sorted as mutiple
 * instances for Servlet Mapping and service invoke process.
 * 
 * @author Leo Chang - Hirisun
 * @version 2011-7-2
 */
public class ClonedExecutableServletObject<T> {
	OSGiJEEContainer jeeContainer = FwkRuntime.getInstance().getJeeContainer();
	private final int id;
	private final T oObj;

	public ClonedExecutableServletObject(int clonedObjectID,
			Object originalObject) {
		this.id = clonedObjectID;
		this.oObj = (T) originalObject;
	}

	/**
	 * Get the original Object that be cloned. It may be {@link ServletDelegate}
	 * or {@link FilterDelegate}.
	 * 
	 * @return
	 */
	public T getOriginalObj() {
		return oObj;
	}

	/**
	 * @return the ID of this cloned URL Parttern Object.
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return whether this cloned url pattern object is executed in the current
	 *         request thread.
	 */
	public boolean isExecuted() {
		return jeeContainer.getReqThreadVariants().containsKey(
				"URLMapObj" + oObj.hashCode());
	}

	/**
	 * Set the mark that whether this cloned url pattern object is executed in
	 * the current request thread.
	 */
	public void setExecuted() {
		jeeContainer.getReqThreadVariants().put("URLMapObj" + oObj.hashCode(),
				"");
	}

	/**
	 * refresh and clear the executed mark.
	 */
	public void refresh() {
		jeeContainer.getReqThreadVariants().remove(
				"URLMapObj" + oObj.hashCode());
	}
}
