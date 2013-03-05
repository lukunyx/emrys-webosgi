package org.emrys.webosgi.common.license;

/**
 * The license Consumer which requires a certain valid license to active.
 * 
 * @author Leo Chang
 * @version 2011-6-14
 */
public interface LicenseConsumer {
	String getLicenseConsumerID();

	/**
	 * Return the required license types for this consumer to active. If none,
	 * the default EPL(eclipse public license) license will be applied.
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
