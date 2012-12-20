/*******************************************************************************
 * Copyright (c) 2011 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.adapter.internal;

import java.io.IOException;
import javax.servlet.*;

/**
 * 
 * @author Leo Chang - EMRYS
 * @version 2011-4-19
 */
public class RequestDispatcherAdapter {
	private RequestDispatcher wrapperedDispatcher;

	public RequestDispatcherAdapter(RequestDispatcher dispatcher) {
		this.wrapperedDispatcher = dispatcher;
	}

	public void forward(ServletRequest request, ServletResponse response) throws ServletException,
			IOException {
		wrapperedDispatcher.forward(request, response);
	}

	public void include(ServletRequest request, ServletResponse response) throws ServletException,
			IOException {
		wrapperedDispatcher.include(request, response);
	}
}
