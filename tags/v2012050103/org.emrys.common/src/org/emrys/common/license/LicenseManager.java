/*******************************************************************************
 * Copyright (c) 2011 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.common.license;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.emrys.common.CommonActivator;
import org.emrys.common.util.FileUtil;


/**
 * The sigleton instanced License Manager for manage the licenses in the framework.
 * 
 * @author Leo Chang - EMRYS
 * @version 2011-6-14
 */
public class LicenseManager {
	/**
	 * buffer list of registered license providers.
	 */
	private final List<LicenseProvider> providers = new ArrayList<LicenseProvider>();

	/**
	 * the singleton instance of this LicenseManager
	 */
	private static LicenseManager instance;

	/**
	 * @return the singleton instance of this LicenseManager
	 */
	public static LicenseManager getInstance() {
		if (instance == null)
			instance = new LicenseManager();
		return instance;
	}

	/**
	 * Invisible Constructor.
	 */
	protected LicenseManager() {
	}

	/**
	 * Check the license for a consumer
	 * 
	 * @param consumer
	 *            the give cosumer to check the license for.
	 * @return if has valid license available.
	 */
	public boolean checkLicense(LicenseConsumer consumer) {
		License[] licenses = getLicenses(consumer, true, true);
		if (licenses.length > 0) {
			try {
				licenses[0].active(consumer);
				return true;
			} catch (LicenseException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	/**
	 * @param consumer
	 *            for which the license be generated.
	 * @param onlyFirst
	 *            only find the first license.
	 * @param valid
	 *            only find valid license for this consumer, otherwise, find invlid.
	 * @return
	 */
	public License[] getLicenses(LicenseConsumer consumer, boolean onlyFirst, boolean valid) {
		String[] reqLicenseTypes = consumer.getRequiredLicenseType();
		if (reqLicenseTypes == null || reqLicenseTypes.length == 0)
			return new License[] { epl };

		sortProviders();
		List<License> result = new ArrayList<License>();
		for (Iterator<LicenseProvider> it = providers.iterator(); it.hasNext();) {
			License license = it.next().getLicense(consumer);
			if (license == null)
				continue;
			if (valid && license.isValid()) {
				if (onlyFirst)
					return new License[] { license };
				else
					result.add(license);
			} else if (!valid && !license.isValid()) {
				if (onlyFirst)
					return new License[] { license };
				else
					result.add(license);
			}
		}
		return result.toArray(new License[result.size()]);
	}

	/**
	 * Sort the providers by their priority.
	 */
	private void sortProviders() {
		Collections.sort(providers, new Comparator<LicenseProvider>() {
			public int compare(LicenseProvider o1, LicenseProvider o2) {
				return (o1.getPriority() > o2.getPriority()) ? -1 : 1;
			}
		});
	}

	/**
	 * Register License Provider for its supported license types.
	 * 
	 * @param provider
	 */
	public void registerLicenseProvider(LicenseProvider provider) {
		providers.add(provider);
	}

	/**
	 * @return all available license types.
	 */
	public Collection<LicenseType> getAllLicenseTypes() {
		Set<LicenseType> result = new HashSet<LicenseType>();
		for (Iterator<LicenseProvider> it = providers.iterator(); it.hasNext();) {
			LicenseProvider provider = it.next();
			String[] supportLicenseTypes = provider.getSupportedLicenseTypes();
			for (int i = 0; i < supportLicenseTypes.length; i++) {
				result.add(provider.getLicenseType(supportLicenseTypes[i]));
			}
		}
		return result;
	}

	/**
	 * Try to find a valid license for the given license type.
	 * 
	 * @return
	 */
	public LicenseType findLicenseType(String licenseTypeID) {
		sortProviders();
		for (Iterator<LicenseProvider> it = providers.iterator(); it.hasNext();) {
			LicenseProvider provider = it.next();
			String[] supportLicenseTypes = provider.getSupportedLicenseTypes();
			for (int i = 0; i < supportLicenseTypes.length; i++) {
				if (supportLicenseTypes[i].equals(licenseTypeID)) {
					LicenseType type = provider.getLicenseType(licenseTypeID);
					if (type != null) {
						return type;
					}
				}
			}
		}
		return null;
	}

	/**
	 * get the license storage root path in the server's file system.
	 * 
	 * @param licenseType
	 * @return
	 */
	public IPath getLicenseRoot(String licenseType) {
		return new Path(CommonActivator.getInstance().getComponentWorkspaceRoot().getAbsolutePath())
				.append("licenses").append(licenseType);
	}

	/**
	 * Save the license to repository.
	 * 
	 * @param licenseContent
	 * @param licenseType
	 * @param fileName
	 * @return
	 * @throws Exception
	 */
	public String saveLicense(String licenseContent, String licenseType, String fileName)
			throws Exception {
		ByteArrayInputStream bin = new ByteArrayInputStream(licenseContent.getBytes("ISO8859-1"));
		IPath root = getLicenseRoot(licenseType);
		File licenseFile = root.append(fileName != null ? fileName : genLicenseFileName(root))
				.toFile();
		FileUtil.createFile(licenseFile, false);
		FileUtil.writeToFile(bin, licenseFile);
		return licenseFile.getAbsolutePath();
	}

	/**
	 * Get the license storage file's name to save.
	 * 
	 * @param licenseRoot
	 * @return
	 */
	private String genLicenseFileName(IPath licenseRoot) {
		return new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
	}

	/**
	 * Get the license storage file.
	 * 
	 * @param licenseType
	 * @return
	 */
	public File getLatestLicenseFile(String licenseType) {
		SimpleDateFormat formater = new SimpleDateFormat("yyyyMMddHHmmss");
		File root = getLicenseRoot(licenseType).toFile();
		if (!root.exists())
			return null;
		File[] subFiles = root.listFiles();
		String tarFileName = null;
		Date tmpDate = null;
		for (int i = 0; i < subFiles.length; i++) {
			if (subFiles[i].isFile()) {
				String name = subFiles[i].getName();
				try {
					Date date = formater.parse(name);
					if (tmpDate == null) {
						tmpDate = date;
						tarFileName = name;
					} else if (date.after(tmpDate)) {
						tmpDate = date;
						tarFileName = name;
					}
				} catch (ParseException e) {
					// e.printStackTrace();
				}
			}
		}

		if (tarFileName != null) {
			return getLicenseRoot(licenseType).append(tarFileName).toFile();
		}

		return null;
	}

	/**
	 * The default EPL license instance if License Consumer not specifing required license type.
	 */
	public static License epl = new EPL();

	/**
	 * The default EPL license type
	 * 
	 * @author Leo Chang - EMRYS
	 * @version 2011-6-1
	 */
	public static class EPL implements License {
		public void active(LicenseConsumer consumer) {
			consumer.setValidLicense(this);
		}

		public String getAttributeValue(String name) {
			return null;
		}

		public String[] getConsumerIDs() {
			return null;
		}

		public String getInvalidCause(Locale local) {
			return null;
		}

		public String getLicenseCode() {
			return null;
		}

		public LicenseType getLicenseType() {
			return null;
		}

		public boolean isBindedToConsummer(String consumerID) {
			return false;
		}

		public boolean isValid() {
			return true;
		}

		public String getLicenseID() {
			return "epl";
		}

		public String getSource() {
			return null;
		}

		public void save() {
			// Do nothing...
		}
	}
}
