/*******************************************************************************
 * Copyright (c) 2011 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.common.license;

/**
 * The license Consumer which requires a certain valid license to active.
 * 
 * @author Leo Chang - EMRYS
 * @version 2011-6-14
 */
public interface LicenseConsumer {
	String getLicenseConsumerID();

	/**
	 * Return the required license types for this consumer to active. If none, the default
	 * EPL(eclipse public license) license will be applied.
	 * 
	 * @return
	 */
	String[] getRequiredLicenseType();

	/**
	 * @return the valid current license.
	 */
	License getValidLicense();

	/**
	 * Set the valid license.
	 * 
	 * @param license
	 */
	void setValidLicense(License license);
}
