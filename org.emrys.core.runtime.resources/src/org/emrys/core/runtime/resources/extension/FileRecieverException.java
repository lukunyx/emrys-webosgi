/*******************************************************************************
 * Copyright (c) 2010 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.resources.extension;

/**
 * 
 * @author Leo Chang - EMRYS
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
