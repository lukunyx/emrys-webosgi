/*******************************************************************************
 * Copyright (c) 2011 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.common.license;

import java.util.Locale;

/**
 * external license provider if any, such as hardware key in usb.
 * 
 * @author Leo Chang - EMRYS
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
