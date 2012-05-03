/*******************************************************************************
 * Copyright (c) 2011 Hirisun Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Hirisun License v1.0
 * which accompanies this distribution, and is available at
 * http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.common.license;

import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

/**
 * the LicenseType interface
 * 
 * @author Leo Chang - Hirisun
 * @version 2011-6-8
 */
public interface LicenseType {
	/**
	 * get the provider of this license type.
	 * 
	 * @return
	 */
	LicenseProvider getProvider();

	/**
	 * Get the identity name of this license type, should be 6 chars string. If less 6 chars, a 'n'
	 * char will be appended. Otherwise, the more chars will be removed in use.
	 * 
	 * @return
	 */
	String getTypeName();

	/**
	 * Get the display name on UI.
	 * 
	 * @param locale
	 * @return
	 */
	String getDisplayName(Locale locale);

	/**
	 * Get the description of this license type.
	 * 
	 * @param locale
	 * @return
	 */
	String getDescription(Locale locale);

	/**
	 * @return
	 */
	Enumeration<String> getAttributeNames();

	/**
	 * @param name
	 * @return
	 */
	LicenseAttribute getAttribute(String name);

	/**
	 * set the attribute value to given decodeLicense
	 * 
	 * @param name
	 * @param decodeLicense
	 * @return
	 */
	String getAttributeValue(String name, String decodeLicense);

	/**
	 * @return the Encryptor of this License Type
	 */
	Encryptor getEncryptor();

	/**
	 * @param code
	 * @return not null, dedicating this code is for this license type and can be parsed
	 *         successfully.
	 */
	String parseLicenseCode(String code);

	/**
	 * Whether this license is valid. If validate failed for any cause, a LicenseException will be
	 * threw indicating this license illegal. If no LicenseException occur, this license is valid
	 * then.
	 * 
	 */
	void validateLicense(License license) throws LicenseException;

	/**
	 * @param decodeContent
	 * @param attrNameActiveTime
	 * @return
	 */
	void setAttribute(StringBuffer decodeContent, String attName, String value);

	/**
	 * The util method for generating a new license of this type.
	 * 
	 * @param attrs
	 * @return
	 * @throws LicenseException
	 */
	String createNewLicense(Map<String, String> attrs) throws LicenseException;

	/**
	 * @return the License Declaring Content like the EPL.
	 */
	String getLicenseDeclareContent();
}
