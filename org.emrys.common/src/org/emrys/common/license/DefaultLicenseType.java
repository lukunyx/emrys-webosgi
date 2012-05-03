/*******************************************************************************
 * Copyright (c) 2011 Hirisun Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Hirisun License v1.0
 * which accompanies this distribution, and is available at
 * http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.common.license;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

/**
 * 
 * @author Leo Chang - Hirisun
 * @version 2011-6-14
 */
public class DefaultLicenseType implements LicenseType {
	/**
	 * the license type ID
	 */
	public static final String TYPE_COMMUNITY_ID = "Community License";

	/**
	 * the common time format string for license time attribute
	 */
	public static final String TIME_FORMATE = "yyyyMMddHHmmss";

	/**
	 * Attribute name: license type
	 */
	public static final String ATTR_NAME_LICENSE_TYPE = "license-type";

	/**
	 * The maximum days interval between create date and active date. Attribute
	 * name: max days count before active this license, if timeout, the license
	 * will expires.
	 */
	public static final String ATTR_NAME_MAX_ACTIVE_INTERVAL = "max-days-before-active";
	/**
	 * Attribute name: create time
	 */
	public static final String ATTR_NAME_CREATE_TIME = "create-time";
	/**
	 * Attribute name: active time
	 */
	public static final String ATTR_NAME_ACTIVE_TIME = "active-time";
	/**
	 * Attribute name: valid days
	 */
	public static final String ATTR_NAME_VALID_TIME = "valid-days";
	/**
	 * Attribute name: user name
	 */
	public static final String ATTR_NAME_USER_NAME = "user-name";
	/**
	 * Attribute name: user mail
	 */
	public static final String ATTR_NAME_USER_MAIL = "user-mail";
	/**
	 * Attribute name: last use time
	 */
	public static final String ATTR_NAME_LAST_USE_TIME = "last-use-name";
	/**
	 * Attribute name: apply code
	 */
	public static final String ATTR_NAME_REQ_CODE = "apply-code";
	/**
	 * the constant variant: eternal license, that valid for ever.
	 */
	public static final String VALID_ETERNITY = "eternal";

	/**
	 * DES Encrypt password.
	 */
	protected static final String DES_ENCRYPTPWD = "19821006";

	/**
	 * Attributes
	 */
	protected Map<String, LicenseAttribute> attributes;
	/**
	 * Attribute names
	 */
	protected List<String> propNames;
	/**
	 * License provider
	 */
	private final LicenseProvider provider;

	/**
	 * encryptor
	 */
	private Encryptor encryptor;

	/**
	 * the util main method to generate a new license of this type.
	 * 
	 * @param args
	 * @throws LicenseException
	 */
	public static void main(String[] args) throws Exception {
		Map<String, String> attrs = new HashMap<String, String>();
		attrs.put(ATTR_NAME_LICENSE_TYPE, TYPE_COMMUNITY_ID);
		attrs.put(ATTR_NAME_REQ_CODE, "192493727");
		attrs.put(ATTR_NAME_CREATE_TIME, new SimpleDateFormat(TIME_FORMATE).format(new Date()));
		attrs.put(ATTR_NAME_VALID_TIME, "900");
		attrs.put(ATTR_NAME_USER_NAME, "developer");
		attrs.put(ATTR_NAME_USER_MAIL, "developer@hirisun.com");
		String encryptLicense = new DefaultLicenseType(null).createNewLicense(attrs);
		System.out.println("Internal created license:" + encryptLicense);
		DesUtils des = new DesUtils(DES_ENCRYPTPWD);
		try {
			String decryptLicense = des.decrypt(encryptLicense);
			System.out.println("Decrypted license:" + decryptLicense);
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println();
	}

	public DefaultLicenseType(LicenseProvider provider) {
		this.provider = provider;
		attributes = new HashMap<String, LicenseAttribute>();
		propNames = new ArrayList();
		attributes.put(ATTR_NAME_LICENSE_TYPE, new DefaultLicenseAttribute(ATTR_NAME_LICENSE_TYPE,
				"License Type", this.getTypeName(),
				"This attribute delicating the type of License", true, false, 20));
		propNames.add(ATTR_NAME_LICENSE_TYPE);

		attributes.put(ATTR_NAME_CREATE_TIME,
				new DefaultLicenseAttribute(ATTR_NAME_CREATE_TIME, "Created Date", "",
						"The date of this license applied and created", true, false, 14));
		propNames.add(ATTR_NAME_CREATE_TIME);

		attributes.put(ATTR_NAME_ACTIVE_TIME, new DefaultLicenseAttribute(ATTR_NAME_ACTIVE_TIME,
				"Active Date", "", "The date of this license be actived", true, true, 14));
		propNames.add(ATTR_NAME_ACTIVE_TIME);

		attributes.put(ATTR_NAME_LAST_USE_TIME, new DefaultLicenseAttribute(
				ATTR_NAME_LAST_USE_TIME, "Last Use Date", "",
				"The date of this license be actived", true, true, 14));
		propNames.add(ATTR_NAME_LAST_USE_TIME);

		attributes.put(ATTR_NAME_VALID_TIME, new DefaultLicenseAttribute(ATTR_NAME_VALID_TIME,
				"valid days", "90", "The Days befor expire", true, false, 7));
		propNames.add(ATTR_NAME_VALID_TIME);

		attributes.put(ATTR_NAME_USER_NAME,
				new DefaultLicenseAttribute(ATTR_NAME_USER_NAME, "User Name", "anonymous",
						"The user name who applied this license", true, false, 20));
		propNames.add(ATTR_NAME_USER_NAME);

		attributes.put(ATTR_NAME_USER_MAIL, new DefaultLicenseAttribute(ATTR_NAME_USER_MAIL,
				"User Email", "none", "The user email who applied this license", false, false, 50));
		propNames.add(ATTR_NAME_USER_MAIL);

		attributes.put(ATTR_NAME_MAX_ACTIVE_INTERVAL, new DefaultLicenseAttribute(
				ATTR_NAME_MAX_ACTIVE_INTERVAL, "Max Days before active", "7",
				"The user email who applied this license", false, false, 3));
		propNames.add(ATTR_NAME_MAX_ACTIVE_INTERVAL);

		attributes.put(ATTR_NAME_REQ_CODE, new DefaultLicenseAttribute(ATTR_NAME_REQ_CODE,
				"Applly Code", null, "Applly Code", true, false, 20));
		propNames.add(ATTR_NAME_REQ_CODE);
	}

	public LicenseAttribute getAttribute(String name) {
		return attributes.get(name);
	}

	public void setAttribute(StringBuffer rawLicense, String attrName, String value) {
		int start = this.getAttrBeginIndex(attrName);
		int length = this.getAttribute(attrName).getValueBytesCount();
		if (length > value.length())
			length = value.length();
		rawLicense.replace(start, start + length, value.substring(0, length));
	}

	public String getAttributeValue(String name, String decodeLicense) {
		try {
			LicenseAttribute attr = this.getAttribute(name);
			if (attr == null)
				return null;

			int beginIndex = getAttrBeginIndex(name);
			int endIndex = beginIndex + attr.getValueBytesCount();
			String value = decodeLicense.substring(beginIndex, endIndex);
			if (value.trim().length() == 0)
				value = attr.getDefaultValue();
			return value.trim();
		} catch (Throwable t) {
			// t.printStackTrace();
		}
		return null;
	}

	/**
	 * @param name
	 * @param decodeLicense
	 * @return
	 */
	private int getAttrBeginIndex(String name) {
		Enumeration<String> names = this.getAttributeNames();
		int index = 0;
		while (names.hasMoreElements()) {
			String tmpName = names.nextElement();
			if (tmpName.equals(name)) {
				return index;
			} else
				index += this.getAttribute(tmpName).getValueBytesCount();
		}
		return 0;
	}

	public String getDescription(Locale locale) {
		return "community version only for none commercial use.";
	}

	public String getDisplayName(Locale locale) {
		return "Community Version";
	}

	public Encryptor getEncryptor() {
		if (encryptor == null) {
			encryptor = new Encryptor() {
				public String decrypt(String source) throws Exception {
					DesUtils des = new DesUtils(DES_ENCRYPTPWD);
					String result = des.decrypt(source);
					return result;
				}

				public String encrypt(String source) throws Exception {
					DesUtils des = new DesUtils(DES_ENCRYPTPWD);
					String result = des.encrypt(source);
					return result;
				}

				public void setLicenseType(LicenseType licenseType) {
					// ignore input LicenseType
				}
			};
		}

		return encryptor;
	}

	public Enumeration<String> getAttributeNames() {
		final Iterator<String> it = propNames.iterator();
		return new Enumeration() {
			public boolean hasMoreElements() {
				return it.hasNext();
			}

			public Object nextElement() {
				return it.next();
			}
		};
	}

	public String getTypeName() {
		return TYPE_COMMUNITY_ID;
	}

	public void validateLicense(License license) throws LicenseException {
		// Default license type not validate actually.
		if (license.getLicenseType().getTypeName().equals(this.getTypeName())) {
			// if this license has not be actived, it's valid.
			String validTimeStr = license.getAttributeValue(ATTR_NAME_VALID_TIME);
			if (validTimeStr.equals(VALID_ETERNITY))
				return;

			// If validTimeStr end with ddddd'M', this indicates that the numner
			// ddddd is months
			// count.
			int validDays = 0;
			int validMonths = 0;
			try {
				validDays = Integer.parseInt(validTimeStr);
			} catch (NumberFormatException e1) {
				// e1.printStackTrace();
				if (validTimeStr.charAt(validTimeStr.length() - 1) == 'M')
					throw new LicenseException("Valid days or months of the license is illegal.");
				else {
					try {
						validMonths = Integer.parseInt(validTimeStr.substring(0, validTimeStr
								.length() - 2));
					} catch (Throwable t) {
						throw new LicenseException(
								"Valid days or months of the license is illegal.");
					}
				}
			}

			String createDateStr = license.getAttributeValue(ATTR_NAME_CREATE_TIME);

			Date createDate = null;
			try {
				createDate = new SimpleDateFormat(DefaultLicenseType.TIME_FORMATE)
						.parse(createDateStr);
			} catch (ParseException e) {
				// e.printStackTrace();
				throw new LicenseException("The create date is invalid.");
			}

			String activeDateStr = license.getAttributeValue(ATTR_NAME_ACTIVE_TIME);

			String maxDaysBeforeActive;
			if (activeDateStr.length() == 0) {
				maxDaysBeforeActive = license.getAttributeValue(ATTR_NAME_MAX_ACTIVE_INTERVAL);
				if (maxDaysBeforeActive.length() != 0) {
					int freshDays = 0;
					try {
						freshDays = Integer.parseInt(maxDaysBeforeActive);
					} catch (NumberFormatException e) {
						// e.printStackTrace();
					}

					Calendar latestActiveDate = Calendar.getInstance();
					latestActiveDate.setTime(createDate);
					latestActiveDate.add(Calendar.DAY_OF_YEAR, freshDays);
					if (latestActiveDate.getTime().before(new Date()))
						throw new LicenseException(
								"Can not active this license. Your license has been out of fresh days("
										+ freshDays + "d).");

				}
				return;
			} else {
				try {
					Date activeDate = new SimpleDateFormat(TIME_FORMATE).parse(activeDateStr);
					Calendar expireDate = Calendar.getInstance();
					expireDate.setTime(activeDate);
					if (validDays > 0)
						expireDate.add(Calendar.DAY_OF_YEAR, validDays);
					else
						expireDate.add(Calendar.MONTH, validMonths);

					if (expireDate.getTime().before(new Date()))
						throw new LicenseException("The license has been expired at "
								+ new SimpleDateFormat("yyyy/MM/dd").format(expireDate.getTime()));
				} catch (ParseException e) {
					// e.printStackTrace();
					throw new LicenseException("Invalid license for invalid active date.");
				}
			}
		} else {
			throw new LicenseException("Wrong license type to valide by the current license type");
		}
	}

	public String parseLicenseCode(String code) {
		String decodeContent = null;
		try {
			decodeContent = getEncryptor().decrypt(code);
		} catch (Exception e) {
			// e.printStackTrace();
		}
		if (decodeContent == null)
			return null;

		if (this.getTypeName().equals(getAttributeValue(ATTR_NAME_LICENSE_TYPE, decodeContent)))
			return decodeContent;

		return null;
	}

	private int getRawLicesenLength() {
		int length = 0;
		Iterator<LicenseAttribute> it = attributes.values().iterator();
		while (it.hasNext()) {
			LicenseAttribute attr = it.next();
			length += attr.getValueBytesCount();
		}
		return length;
	}

	public String createNewLicense(Map<String, String> attrs) throws LicenseException {
		// set the empty license attribute with blank char.
		char[] defaultContent = new char[this.getRawLicesenLength()];
		Arrays.fill(defaultContent, ' ');
		StringBuffer rawLicense = new StringBuffer();
		rawLicense.append(defaultContent);

		Enumeration<String> attrNames = this.getAttributeNames();
		while (attrNames.hasMoreElements()) {
			String name = attrNames.nextElement();
			String value = attrs.get(name);
			LicenseAttribute attr = this.getAttribute(name);
			if (value != null)
				this.setAttribute(rawLicense, name, value);
			else {
				// if not give a value, try to use default value.
				if (attr.getDefaultValue() != null)
					this.setAttribute(rawLicense, name, attr.getDefaultValue());
				else if (attr.isRequired() && !attr.isRuntimeGenerated()) {
					throw new LicenseException("The attribute " + name
							+ "needed to create new license.");
				}
			}
		}

		try {
			return getEncryptor().encrypt(rawLicense.toString());
		} catch (Exception e) {
			throw new LicenseException(e);
		}
	}

	public LicenseProvider getProvider() {
		return provider;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.emrys.common.license.LicenseType#getLicenseDeclareContent
	 * ()
	 */
	public String getLicenseDeclareContent() {
		return "http://www.eclipse.com/legal/epl-v10.html";
	}
}
