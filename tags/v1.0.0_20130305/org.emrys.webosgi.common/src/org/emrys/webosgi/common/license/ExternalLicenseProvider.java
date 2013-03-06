package org.emrys.webosgi.common.license;

import java.util.Locale;

/**
 * external license provider if any, such as hardware key in usb.
 * 
 * @author Leo Chang
 * @version 2011-6-21
 */
public interface ExternalLicenseProvider {
	/**
	 * Get the license content from external.
	 * 
	 * @return
	 */
	String getLicenseContent();

	/**
	 * The description or tooltips of this external license provider.
	 * 
	 * @return
	 */
	String getExternalSourceDescription(Locale locale);
}
