/*******************************************************************************
 * Copyright (c) 2010 Hirisun Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Hirisun License v1.0
 * which accompanies this distribution, and is available at
 * http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.resources.extension;

import java.util.List;

/**
 * The folder resource filter.
 * 
 * @author Leo Chang - Hirisun
 * @version 2010-10-11
 */
public class ResFilter {
	/**
	 * whether the publisher should include this resource.
	 */
	boolean included = true;
	/**
	 * 
	 */
	String pattern;

	/**
	 * @return the sub resources
	 */
	public List<DefinesRoot> getResources() {
		return null;
	}

	/**
	 * Return whether the publisher should include this resource.
	 * 
	 * @return
	 */
	public boolean isIncluded() {
		return included;
	}

	/**
	 * Set whether the publisher should include this resource.
	 * 
	 * @param included
	 */
	public void setIncluded(boolean included) {
		this.included = included;
	}

	/**
	 * @return the express pattern.
	 */
	public String getPattern() {
		return pattern;
	}

	/**
	 * Set the express pattern.
	 * 
	 * @param pattern
	 */
	public void setPattern(String pattern) {
		this.pattern = pattern;
	}
}
