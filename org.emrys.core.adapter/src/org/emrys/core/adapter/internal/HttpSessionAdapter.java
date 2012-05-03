/*******************************************************************************
 * Copyright (c) 2011 Hirisun Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Hirisun License v1.0
 * which accompanies this distribution, and is available at
 * http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.adapter.internal;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

/**
 * 
 * @author Leo Chang - Hirisun
 * @version 2011-4-19
 */
public class HttpSessionAdapter implements IServletObjectWrapper {
	private static Set<HttpSessionAdapter> wrappers = new HashSet<HttpSessionAdapter>();

	/**
	 * @param session
	 * @return HttpSessionAdapter
	 */
	public static HttpSessionAdapter getAdapter(HttpSession session) {
		if (session != null) {
			for (HttpSessionAdapter w : wrappers) {
				if (session.equals(((HttpSessionAdapter) w).getOriginalObject())) {
					return w;
				}
			}
			HttpSessionAdapter newWrapper = new HttpSessionAdapter(session);
			wrappers.add(newWrapper);
			return newWrapper;
		}
		return null;
	}

	private HttpSession wrapperedSession;

	public HttpSessionAdapter(HttpSession session) {
		this.wrapperedSession = session;
	}

	public Object getAttribute(String name) {
		return wrapperedSession.getAttribute(name);
	}

	public Enumeration getAttributeNames() {
		return wrapperedSession.getAttributeNames();
	}

	public long getCreationTime() {
		return wrapperedSession.getCreationTime();
	}

	public String getId() {
		return wrapperedSession.getId();
	}

	public long getLastAccessedTime() {
		return wrapperedSession.getLastAccessedTime();
	}

	public int getMaxInactiveInterval() {
		return wrapperedSession.getMaxInactiveInterval();
	}

	public ServletContext getServletContext() {
		return wrapperedSession.getServletContext();
	}

	public HttpSessionContext getSessionContext() {
		return wrapperedSession.getSessionContext();
	}

	public Object getValue(String name) {
		return wrapperedSession.getValue(name);
	}

	public String[] getValueNames() {
		return wrapperedSession.getValueNames();
	}

	public void invalidate() {
		wrapperedSession.invalidate();
	}

	public boolean isNew() {
		return wrapperedSession.isNew();
	}

	public void putValue(String name, Object value) {
		wrapperedSession.putValue(name, value);
	}

	public void removeAttribute(String name) {
		wrapperedSession.removeAttribute(name);
	}

	public void removeValue(String name) {
		wrapperedSession.removeValue(name);
	}

	public void setAttribute(String name, Object value) {
		wrapperedSession.setAttribute(name, value);
	}

	public void setMaxInactiveInterval(int interval) {
		wrapperedSession.setMaxInactiveInterval(interval);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.equinox.servletbridge.IServletObjectWrapper#getOriginalObject()
	 */
	public Object getOriginalObject() {
		// TODO Auto-generated method stub
		return wrapperedSession;
	}
}
