/*******************************************************************************
 * Copyright (c) 2011 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.common.license;

/**
 * License Provider for specified license types.
 * 
 * @author Leo Chang - EMRYS
 * @version 2011-6-14
 */
public interface LicenseProvider {
	/**
	 * Try to get a valid license for the given LicenseConsumer
	 * 
	 * @param consumer
	 * @return
	 */
	License getLicense(LicenseConsumer consumer);

	/**
	 * Try to get a Valid License for a license type.
	 * 
	 * @param licenseTypeId
	 * @return
	 */
	License getValidLicense(String licenseTypeId);

	/**
	 * @return the priority of this provider if multiple providers exists for a license type.
	 */
	int getPriority();

	/**
	 * @return the supported license type ids this provider support.
	 */
	String[] getSupportedLicenseTypes();

	/**
	 * get the license type instance.
	 * 
	 * @param licenseTypeName
	 * @return
	 */
	LicenseType getLicenseType(String licenseTypeName);

	/**
	 * Validate whether the given encrypted License code is adapthable for this provider's license
	 * type.
	 * 
	 * @param encryptLicense
	 *            encrypted License code
	 * @return true if this encrypted License code is ok, otherwise, false.
	 */
	boolean acceptLicenseCode(String encryptLicense);
}
