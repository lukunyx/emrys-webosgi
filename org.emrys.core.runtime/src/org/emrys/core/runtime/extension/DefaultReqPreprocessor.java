/*******************************************************************************
 * Copyright (c) 2010 Hirisun Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Hirisun License v1.0
 * which accompanies this distribution, and is available at
 * http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.extension;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * DefaultReqPreprocessor
 * 
 * @author Leo Chang - Hirisun
 * @version 2010-11-2
 */
public class DefaultReqPreprocessor implements IReqProcessor {
	public int process(ServletRequest request, ServletResponse response) {
		return RESULT_CONTINUE;
	}
}
