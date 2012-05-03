/*******************************************************************************
 * Copyright (c) 2011 Hirisun Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Hirisun License v1.0
 * which accompanies this distribution, and is available at
 * http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.adapter.internal;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.Cookie;

/**
 * 
 * @author Leo Chang - Hirisun
 * @version 2011-6-1
 */
public class CookieAdapter implements IServletObjectWrapper {

	private Cookie cookie;

	public static CookieAdapter[] adaptArray(Cookie[] cookies) {
		if (cookies != null && cookies.length > 0) {
			List<CookieAdapter> result = new ArrayList<CookieAdapter>();
			for (Cookie c : cookies) {
				result.add(new CookieAdapter(c));
			}
			return result.toArray(new CookieAdapter[result.size()]);
		}
		return new CookieAdapter[0];
	}

	public CookieAdapter(Cookie cookie) {
		this.cookie = cookie;
	}

	public Object getOriginalObject() {
		return cookie;
	}

	public Object clone() {
		return cookie.clone();
	}

	public boolean equals(Object obj) {
		if (obj instanceof CookieAdapter)
			cookie.equals(((CookieAdapter) obj).getOriginalObject());
		return cookie.equals(obj);
	}

	public String getComment() {
		return cookie.getComment();
	}

	public String getDomain() {
		return cookie.getDomain();
	}

	public int getMaxAge() {
		return cookie.getMaxAge();
	}

	public String getName() {
		return cookie.getName();
	}

	public String getPath() {
		return cookie.getPath();
	}

	public boolean getSecure() {
		return cookie.getSecure();
	}

	public String getValue() {
		return cookie.getValue();
	}

	public int getVersion() {
		return cookie.getVersion();
	}

	public int hashCode() {
		return cookie.hashCode();
	}

	public void setComment(String purpose) {
		cookie.setComment(purpose);
	}

	public void setDomain(String pattern) {
		cookie.setDomain(pattern);
	}

	public void setMaxAge(int expiry) {
		cookie.setMaxAge(expiry);
	}

	public void setPath(String uri) {
		cookie.setPath(uri);
	}

	public void setSecure(boolean flag) {
		cookie.setSecure(flag);
	}

	public void setValue(String newValue) {
		cookie.setValue(newValue);
	}

	public void setVersion(int v) {
		cookie.setVersion(v);
	}

	public String toString() {
		return cookie.toString();
	}
}
