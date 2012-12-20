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
 * The License Attribute Interface.
 * 
 * @author Leo Chang - EMRYS
 * @version 2011-6-14
 */
public interface LicenseAttribute {
	/**
	 * @return the License Type's name(ID).
	 */
	public String getName();

	/**
	 * @param local
	 * @return the License Type's display name on UI.
	 */
	public String getDisplayName(Locale local);

	/**
	 * @return the byte count of this attribute will take in license code.
	 */
	public int getValueBytesCount();

	/**
	 * @return if this attribute is force required.
	 */
	public boolean isRequired();

	/**
	 * @return if this attribute is not original set in new license, but generated in runtime.
	 */
	public boolean isRuntimeGenerated();

	/**
	 * @return the defualt value of this attribute if not set.
	 */
	public String getDefaultValue();

	/**
	 * get the descrption of this license attribute.
	 * 
	 * @param local
	 * @return
	 */
	public String getDescription(Locale local);
}
