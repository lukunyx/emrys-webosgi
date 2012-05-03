/*******************************************************************************
 * Copyright (c) 2011 Hirisun Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Hirisun License v1.0
 * which accompanies this distribution, and is available at
 * http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.common.license;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.emrys.common.CommonActivator;


/**
 * Default Communism License type with some attributes.
 * 
 * @author Leo Chang - Hirisun
 * @version 2011-6-14
 */
public class DefaultLicense implements License {
	/**
	 * License type of this license
	 */
	private final LicenseType licenseType;
	/**
	 * Cosummers' id that this license binding.
	 */
	private final HashSet<String> consummerIDs;
	/**
	 * the license content.
	 */
	private String content;
	/**
	 * if this license invalid, this is the cause string.
	 */
	private String invalidCause;
	/**
	 * decypted license content.
	 */
	private String decodeContent;
	/**
	 * the source file of this license.
	 */
	private String source;

	/**
	 * @param type
	 */
	public DefaultLicense(LicenseType type, String content) {
		this.licenseType = type;
		consummerIDs = new HashSet<String>();
		this.content = content;
		decodeContent = type.parseLicenseCode(content);
	}

	public String getAttributeValue(String name) {
		return this.licenseType.getAttributeValue(name, decodeContent);
	}

	public String getInvalidCause(Locale local) {
		return invalidCause;
	}

	public String getLicenseCode() {
		return content;
	}

	public LicenseType getLicenseType() {
		return licenseType;
	}

	public boolean isValid() {
		try {
			licenseType.validateLicense(this);
			return true;
		} catch (LicenseException e) {
			// e.printStackTrace();
			CommonActivator.getInstance().log(e);
			invalidCause = e.toString();
		}
		return false;
	}

	public String[] getConsumerIDs() {
		return consummerIDs.toArray(new String[consummerIDs.size()]);
	}

	public boolean bindToConsumer(String consumerID) {
		return consummerIDs.add(consumerID);
	}

	public boolean isBindedToConsummer(String consumerID) {
		return consummerIDs.contains(consumerID);
	}

	public void active(LicenseConsumer consumer) throws LicenseException {
		consumer.setValidLicense(this);
		if (this.getAttributeValue(DefaultLicenseType.ATTR_NAME_ACTIVE_TIME).length() == 0) {
			StringBuffer tmp = new StringBuffer(decodeContent);
			// Set active and last use time.
			licenseType.setAttribute(tmp, DefaultLicenseType.ATTR_NAME_ACTIVE_TIME,
					new SimpleDateFormat(DefaultLicenseType.TIME_FORMATE).format(new Date()));
			licenseType.setAttribute(tmp, DefaultLicenseType.ATTR_NAME_LAST_USE_TIME,
					new SimpleDateFormat(DefaultLicenseType.TIME_FORMATE).format(new Date()));
			decodeContent = tmp.toString();
			try {
				content = this.getLicenseType().getEncryptor().encrypt(decodeContent);
				save();
			} catch (Exception e) {
				// e.printStackTrace();
				throw new LicenseException("active the license failed.", e);
			}
		}
		this.bindToConsumer(consumer.getLicenseConsumerID());
	}

	public String getLicenseID() {
		return this.getAttributeValue(DefaultLicenseType.ATTR_NAME_CREATE_TIME);
	}

	public String getSource() {
		return source;
	}

	public void setSourceFilePath(String path) {
		source = path;
	}

	public void save() throws Exception {
		String fileName = null;
		if (source != null) {
			try {
				IPath path = new Path(source);
				fileName = path.lastSegment();
			} catch (Exception e) {
				// e.printStackTrace();
			}
		}
		source = LicenseManager.getInstance().saveLicense(this.getLicenseCode(),
				this.getLicenseType().getTypeName(), fileName);
	}
}
