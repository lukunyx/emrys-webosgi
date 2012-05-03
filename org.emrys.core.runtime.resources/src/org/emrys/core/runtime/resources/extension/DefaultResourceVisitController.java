/*******************************************************************************
 * Copyright (c) 2010 Hirisun Corporation. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Hirisun License v1.0 which accompanies this
 * distribution, and is available at http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.resources.extension;

import javax.servlet.http.HttpServletRequest;

/**
 * The default Resource Visit controller.
 * 
 * @author Leo Chang - Hirisun
 * @version 2010-10-11
 */
public class DefaultResourceVisitController implements IResourceVisitController {
	public boolean canBrowseFolder(HttpServletRequest request) {
		return true;
	}

	public boolean canModify(HttpServletRequest request) {
		return true;
	}

	public boolean canRead(HttpServletRequest request) {
		return true;
	}

	public long getLastModifiedTimeMillis() {
		return 0L;
	}
}
