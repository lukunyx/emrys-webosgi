/*******************************************************************************
 * Copyright (c) 2010 Hirisun Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Hirisun License v1.0
 * which accompanies this distribution, and is available at
 * http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.resources.extension;

/**
 * 
 * @author Leo Chang - Hirisun
 * @version 2010-10-27
 */
public class FileRecieverException extends Exception {
	int errHttpCode = 700;

	public int getErrHttpCode() {
		return errHttpCode;
	}

	public void setErrHttpCode(int errHttpCode) {
		this.errHttpCode = errHttpCode;
	}
}
