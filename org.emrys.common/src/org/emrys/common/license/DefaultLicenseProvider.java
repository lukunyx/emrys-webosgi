/*******************************************************************************
 * Copyright (c) 2011 Hirisun Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Hirisun License v1.0
 * which accompanies this distribution, and is available at
 * http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.common.license;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.emrys.common.CommonActivator;
import org.emrys.common.util.FileUtil;


/**
 * The default community license type provider.
 * 
 * @author Leo Chang - Hirisun
 * @version 2011-6-14
 */
public class DefaultLicenseProvider implements LicenseProvider {
	public final License getLicense(LicenseConsumer consumer) {
		String[] requiredLicenseTypes = consumer.getRequiredLicenseType();
		if (requiredLicenseTypes == null || requiredLicenseTypes.length == 0)
			return null;

		for (int i = 0; i < requiredLicenseTypes.length; i++) {
			List<String> supportedTypes = Arrays.asList(this.getSupportedLicenseTypes());
			if (supportedTypes.contains(requiredLicenseTypes[i])) {
				LicenseType type = this.getLicenseType(requiredLicenseTypes[i]);
				if (type != null) {
					License license = restoreLicense(type);
					if (license != null)
						return license;
				}
			}
		}
		return null;
	}

	/**
	 * Restore license.
	 * 
	 * @param type
	 * @return
	 */
	private License restoreLicense(LicenseType type) {
		// Select external license provider if any, such as hardware key in usb.
		String licenseContent = null;
		File licenseFile = null;
		if (this instanceof ExternalLicenseProvider)
			licenseContent = ((ExternalLicenseProvider) this).getLicenseContent();
		if (licenseContent == null) {
			try {
				licenseFile = LicenseManager.getInstance().getLatestLicenseFile(type.getTypeName());
				if (licenseFile != null)
					licenseContent = FileUtil.getContent(licenseFile, "ISO8859-1").toString();
			} catch (Exception e) {
				// e1.printStackTrace();
			}
		}
		if (licenseContent == null)
			return null;

		// trim the blank char from licenseContent is necessary.
		License license = createLicense(type, licenseContent.trim());
		if (licenseFile != null && license instanceof DefaultLicense) {
			((DefaultLicense) license).setSourceFilePath(licenseFile.getAbsolutePath());
		}
		return license;
	}

	/**
	 * @param type
	 * @param string
	 * @return
	 */
	protected License createLicense(LicenseType type, String encryptLicense) {
		return new DefaultLicense(type, encryptLicense);
	}

	public String[] getSupportedLicenseTypes() {
		return new String[] { DefaultLicenseType.TYPE_COMMUNITY_ID };
	}

	public LicenseType getLicenseType(String licenseTypeName) {
		if (DefaultLicenseType.TYPE_COMMUNITY_ID.equals(licenseTypeName))
			return new DefaultLicenseType(this);
		return null;
	}

	public int getPriority() {
		return 0;
	}

	public boolean acceptLicenseCode(String code) {
		String[] types = this.getSupportedLicenseTypes();
		for (int i = 0; i < types.length; i++) {
			LicenseType type = this.getLicenseType(types[i]);
			if (type.parseLicenseCode(code) != null) {
				try {
					DefaultLicense license = new DefaultLicense(type, code);
					if (license.isValid()) {
						license.save();
						return true;
					}
					CommonActivator.getInstance().log(
							"License validate failed:" + license.getInvalidCause(null), 0, false);
				} catch (Exception e) {
					// e.printStackTrace();
					CommonActivator.getInstance().log(e);
				}
			}
		}
		return false;
	}

	public License getValidLicense(String licenseTypeId) {
		LicenseType licenseType = getLicenseType(licenseTypeId);
		if (licenseType != null)
			return restoreLicense(licenseType);
		return null;
	}
}
