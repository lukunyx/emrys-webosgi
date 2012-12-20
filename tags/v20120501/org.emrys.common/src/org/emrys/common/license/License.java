/*******************************************************************************
 * Copyright (c) 2011 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.common.license;

import java.util.Locale;

/**
 * The license interface.
 * 
 * @author Leo Chang - EMRYS
 * @version 2011-6-8
 */
public interface License {
	/**
	 * @return
	 */
	String getSource();

	/**
	 * get the unique id of this license
	 * 
	 * @return
	 */
	String getLicenseID();

	/**
	 * get the consummer's id using this license.
	 * 
	 * @return
	 */
	String[] getConsumerIDs();

	/**
	 * get the license type of this license.
	 * 
	 * @return the license type of this license.
	 */
	LicenseType getLicenseType();

	/**
	 * get the license attribute value of this license by the given attribute name defined in the
	 * license type.
	 * 
	 * @param name
	 *            license attribute name defined in this license type.
	 * @return
	 */
	String getAttributeValue(String name);

	/**
	 * @return the license code(content).
	 */
	String getLicenseCode();

	/**
	 * @return if this license is valid.
	 */
	boolean isValid();

	/**
	 * If this license not valid, get the cause.
	 * 
	 * @param local
	 * @return
	 */
	String getInvalidCause(Locale local);

	/**
	 * Check if this license is used and bind to a consumer.
	 * 
	 * @param consumerID
	 * @return
	 */
	boolean isBindedToConsummer(String consumerID);

	/**
	 * Active this license.
	 * 
	 * @param consumer
	 * @throws LicenseException
	 */
	void active(LicenseConsumer consumer) throws LicenseException;

	/**
	 * Save this license to the source.
	 */
	void save() throws Exception;
}
