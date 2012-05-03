/*******************************************************************************
 * Copyright (c) 2011 Hirisun Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Hirisun License v1.0
 * which accompanies this distribution, and is available at
 * http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.common.license;

/**
 * The Encryptor interface
 * 
 * @author Leo Chang - Hirisun
 * @version 2011-6-14
 */
public interface Encryptor {
	/**
	 * Set the License type this encryptor for.
	 * 
	 * @param licenseType
	 */
	void setLicenseType(LicenseType licenseType);

	/**
	 * encrypt the given content.
	 * 
	 * @param source
	 *            the source content to encrypt
	 * 
	 * @return the result.
	 */
	String encrypt(String source) throws Exception;

	/**
	 * decrypt the given source content and return.
	 * 
	 * @param source
	 *            given source content to decrypted
	 * @return decrypted result.
	 */
	String decrypt(String source) throws Exception;
}
