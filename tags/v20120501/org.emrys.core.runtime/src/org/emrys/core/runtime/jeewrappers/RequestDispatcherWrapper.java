/*******************************************************************************
 * Copyright (c) 2011 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.jeewrappers;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.emrys.core.adapter.internal.IServletObjectWrapper;
import org.emrys.core.adapter.internal.RequestDispatcherAdapter;

/**
 * 
 * @author Leo Chang - EMRYS
 * @version 2011-4-19
 */
public class RequestDispatcherWrapper implements RequestDispatcher, IServletObjectWrapper {
	private final RequestDispatcherAdapter dispatcherAdapter;

	public RequestDispatcherWrapper(RequestDispatcherAdapter dispatcherAdapter) {
		this.dispatcherAdapter = dispatcherAdapter;
	}

	@Override
	public boolean equals(Object obj) {
		return dispatcherAdapter.equals(obj);
	}

	public void forward(ServletRequest request, ServletResponse response) throws ServletException,
			IOException {
		dispatcherAdapter.forward(request, response);
	}

	@Override
	public int hashCode() {
		return dispatcherAdapter.hashCode();
	}

	public void include(ServletRequest request, ServletResponse response) throws ServletException,
			IOException {
		dispatcherAdapter.include(request, response);
	}

	@Override
	public String toString() {
		return dispatcherAdapter.toString();
	}

	public Object getOriginalObject() {
		return dispatcherAdapter;
	}
}
