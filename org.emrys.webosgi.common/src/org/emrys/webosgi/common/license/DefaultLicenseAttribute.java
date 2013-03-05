package org.emrys.webosgi.common.license;

import java.util.Locale;

/**
 * Default implement of LicenseAttribute interface.
 * 
 * @author Leo Chang
 * @version 2011-6-14
 */
public class DefaultLicenseAttribute implements LicenseAttribute {
	/**
	 * name of this license attribute
	 */
	private final String name;
	/**
	 * the default value
	 */
	private final String defaultValue;
	/**
	 * the description
	 */
	private final String description;
	/**
	 * dedicating whether this attribute is force required.
	 */
	private final boolean isRequired;
	/**
	 * the byte count of this attribute that takes in the license content.
	 */
	private final int valueBytesCount;
	/**
	 * the display name of this license on UI.
	 */
	private final String displayName;
	/**
	 * whether this attribute is generated in run-time.
	 */
	private final boolean isRuntimeGenerated;

	/**
	 * Constructor
	 * 
	 * @param name
	 *            the name of this attribute
	 * @param displayName
	 *            the display name of this attribute on UI
	 * @param defaultValue
	 *            the default value of this attribute if not set
	 * @param description
	 *            the description
	 * @param isRequired
	 *            whether this attribute is force required
	 * @param isRuntimeGenerated
	 *            whether this attribute is generated in run-time
	 * @param valueBytesCount
	 *            the byte count this attribute takes in the license content
	 */
	public DefaultLicenseAttribute(String name, String displayName,
			String defaultValue, String description, boolean isRequired,
			boolean isRuntimeGenerated, int valueBytesCount) {
		this.name = name;
		this.displayName = displayName;
		this.defaultValue = defaultValue;
		this.description = description;
		this.isRequired = isRequired;
		this.isRuntimeGenerated = isRuntimeGenerated;
		this.valueBytesCount = valueBytesCount;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public String getDescription(Locale local) {
		return description;
	}

	public String getDisplayName(Locale local) {
		return this.displayName;
	}

	public String getName() {
		return name;
	}

	public int getValueBytesCount() {
		// TODO Auto-generated method stub
		return valueBytesCount;
	}

	public boolean isRequired() {
		return isRequired;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.emrys.webosgi.common.license.LicenseAttribute#isRuntimeGenerated()
	 */
	public boolean isRuntimeGenerated() {
		return isRuntimeGenerated;
	}

}
