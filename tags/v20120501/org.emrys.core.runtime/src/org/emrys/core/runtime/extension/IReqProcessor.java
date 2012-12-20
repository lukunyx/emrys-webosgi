/*******************************************************************************
 * Copyright (c) 2010 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.extension;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * 
 * @author Leo Chang - EMRYS
 * @version 2010-11-2
 */
public interface IReqProcessor {
	public static final int RESULT_CONTINUE = 0;
	public static final int RESULT_OK = 1;
	public static final int RESULT_BREAK_OTHERS = 2;

	/**
	 * Process request or response of it before servlet or after. Here, user can redirect the url,
	 * modify http head, cookie,etc. before servlet. As well, process the response content is also
	 * possible.
	 * 
	 * @param request
	 * @param response
	 * @return
	 */
	int process(ServletRequest request, ServletResponse response);
}
