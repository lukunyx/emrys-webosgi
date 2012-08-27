/*******************************************************************************
 * Copyright (c) 2011 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.common.license;

/**
 * License Exception wrappring the Common Exception
 * 
 * @author Leo Chang - EMRYS
 * @version 2011-6-8
 */
public class LicenseException extends Exception {

	/**
	 * Constructor
	 */
	public LicenseException() {
		super();
	}

	/**
	 * @param message
	 * @param cause
	 */
	public LicenseException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 */
	public LicenseException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public LicenseException(Throwable cause) {
		super(cause);
	}
}
