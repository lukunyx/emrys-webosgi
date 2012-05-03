/*******************************************************************************
 * Copyright (c) 2010 Hirisun Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Hirisun License v1.0
 * which accompanies this distribution, and is available at
 * http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.extension;

import java.io.InputStream;

/**
 * 
 * @author Leo Chang - Hirisun
 * @version 2010-10-11
 */
public interface ITldProvider {
	InputStream getTldContent();
}
