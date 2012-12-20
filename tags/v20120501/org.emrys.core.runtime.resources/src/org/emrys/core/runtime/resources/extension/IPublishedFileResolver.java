/*******************************************************************************
 * Copyright (c) 2010 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.resources.extension;

import java.io.File;

import javax.servlet.http.HttpServletRequest;

/**
 * The published resource file resolver.
 * 
 * @author Leo Chang - EMRYS
 * @version 2010-10-25
 */
public interface IPublishedFileResolver {
	/**
	 * Resolve and return the resource file.
	 * 
	 * @param req
	 *            the current servlet request.
	 * @param path
	 *            the path of resource to find.
	 * @param alias
	 *            the alias of resource.
	 * @param quick_ID
	 *            the quick visit id of this resource.
	 * @return
	 */
	File resolve(HttpServletRequest req, String path, String alias, String quick_ID);
}
