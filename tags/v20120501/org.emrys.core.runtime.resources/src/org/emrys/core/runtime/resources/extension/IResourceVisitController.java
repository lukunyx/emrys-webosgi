/*******************************************************************************
 * Copyright (c) 2010 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.resources.extension;

import javax.servlet.http.HttpServletRequest;

/**
 * The resource visite authority controller.
 * 
 * @author Leo Chang - EMRYS
 * @version 2010-10-11
 */
public interface IResourceVisitController {
	/**
	 * whether the current client user has authority to read the resource.
	 * 
	 * @param requestUrl
	 * @return
	 */
	boolean canRead(HttpServletRequest requestUrl);

	/**
	 * whether the current client user has authority to modify the resource.
	 * 
	 * @param requestUrl
	 * @return
	 */
	boolean canModify(HttpServletRequest requestUrl);

	/**
	 * Whether the user has authority to browse the folder. If no, the response
	 * will return 404 http error code, instead of return the filder's
	 * information.
	 * 
	 * @param req
	 * @return
	 */
	boolean canBrowseFolder(HttpServletRequest req);

	/**
	 * Last-Modified-Time by which to judge wheter the sub resource should be
	 * not update to http client. If return zero, indicate the time framework
	 * started.
	 * 
	 * @return the last modified time in millis.
	 */
	long getLastModifiedTimeMillis();
}
