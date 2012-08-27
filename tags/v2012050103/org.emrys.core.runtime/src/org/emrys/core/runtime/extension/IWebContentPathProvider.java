package org.emrys.core.runtime.extension;

/*******************************************************************************
 * Copyright (c) 2010 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/

/**
 * the interface of the WebContent Path Provider.
 * 
 * @author Leo Chang - EMRYS
 * @version 2010-10-13
 */
public interface IWebContentPathProvider {
	String[] getWebContentBundlePath();
}
