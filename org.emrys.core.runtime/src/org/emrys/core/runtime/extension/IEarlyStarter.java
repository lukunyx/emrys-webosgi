/*******************************************************************************
 * Copyright (c) 2010 Hirisun Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Hirisun License v1.0
 * which accompanies this distribution, and is available at
 * http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.extension;

/**
 * 
 * @author Leo Chang - Hirisun
 * @version 2010-11-2
 */
public interface IEarlyStarter {
	public static final int WHEN_AFTER_FWK_STARTED = 0;
	public static final int WHEN_BEFORE_FWK_START = 1;
	public static final int WHEN_BEFORE_JEE_START = 2;
	public static final int WHEN_AFTER_JEE_STARTED = 3;

	/**
	 * Start Extra process after the whole OSGi initialized, started and
	 * workspace opened.
	 */
	void start();
}
