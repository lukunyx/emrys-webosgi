/*******************************************************************************
 * Copyright (c) 2011 Hirisun Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Hirisun License v1.0
 * which accompanies this distribution, and is available at
 * http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.jeewrappers;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

/**
 * The wrapper class to {@link javax.serlvet.HttpServletResponse} or
 * {@link org.eclipse.equinox.servletbridge.HttpServletResponseAdapter}.
 * 
 * @author Leo Chang - Hirisun
 * @version 2011-3-29
 */
public interface IBuffferdServletResponse extends HttpServletResponse {
	public static final int RESULT_CANCEL = 5000;

	int getState();

	/**
	 * Flush out buffered Response Status to original Servlet Response.
	 * 
	 * @throws IOException
	 */
	void flushBufferStatus() throws IOException;

	String getRedirectLocation();
}
