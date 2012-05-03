package org.emrys.core.runtime.extension;

/*******************************************************************************
 * Copyright (c) 2010 Hirisun Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Hirisun License v1.0
 * which accompanies this distribution, and is available at
 * http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/

/**
 * the interface of the WebContent Path Provider.
 * 
 * @author Leo Chang - Hirisun
 * @version 2010-10-13
 */
public interface IWebContentPathProvider {
	String[] getWebContentBundlePath();
}
