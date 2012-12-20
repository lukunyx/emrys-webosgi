/*******************************************************************************
 * Copyright (c) 2011 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.adapter.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * 鍥犱负瑕佸仛鍒板吋瀹箂ervlet2.4鍜�5瑙勮寖锛�
 * 杩欓噷灏嗕粠鏈嶅姟鍣ㄤ紶閫掕繃鏉ョ殑ServletRequest鍋氶噸鏂扮殑灏佽锛岃�屾槸鐢ㄦ墍鏈夌殑鍙嶅皠鏂规硶璋冪敤涓や釜2.5瑙勮寖鏂板鐨勬柟娉曪紝姣斿getContextPath绛夈��
 * 
 * @author Leo Chang - EMRYS
 * @version 2011-4-18
 */
public class HttpServletRequestAdapter implements IServletObjectWrapper {
	private HttpServletRequest wrapperedHttpRequest;
	private ServletInputStreamAdapter inputWrapper;
	private HttpSessionAdapter sessionWrapper;

	public HttpServletRequestAdapter(HttpServletRequest req) {
		this.wrapperedHttpRequest = req;
	}

	public Object getAttribute(String name) {
		return wrapperedHttpRequest.getAttribute(name);
	}

	public Enumeration getAttributeNames() {
		return wrapperedHttpRequest.getAttributeNames();
	}

	public String getAuthType() {
		return wrapperedHttpRequest.getAuthType();
	}

	public String getCharacterEncoding() {
		return wrapperedHttpRequest.getCharacterEncoding();
	}

	public int getContentLength() {
		return wrapperedHttpRequest.getContentLength();
	}

	public String getContentType() {
		return wrapperedHttpRequest.getContentType();
	}

	public String getContextPath() {
		return wrapperedHttpRequest.getContextPath();
	}

	public CookieAdapter[] getCookies() {
		return CookieAdapter.adaptArray(wrapperedHttpRequest.getCookies());
	}

	public long getDateHeader(String name) {
		return wrapperedHttpRequest.getDateHeader(name);
	}

	public String getHeader(String name) {
		return wrapperedHttpRequest.getHeader(name);
	}

	public Enumeration getHeaderNames() {
		return wrapperedHttpRequest.getHeaderNames();
	}

	public Enumeration getHeaders(String name) {
		return wrapperedHttpRequest.getHeaders(name);
	}

	public ServletInputStreamAdapter getInputStream() throws IOException {
		if (inputWrapper == null)
			inputWrapper = new ServletInputStreamAdapter(wrapperedHttpRequest.getInputStream());
		return inputWrapper;
	}

	public int getIntHeader(String name) {
		return wrapperedHttpRequest.getIntHeader(name);
	}

	public String getLocalAddr() {
		return wrapperedHttpRequest.getLocalAddr();
	}

	public Locale getLocale() {
		return wrapperedHttpRequest.getLocale();
	}

	public Enumeration getLocales() {
		return wrapperedHttpRequest.getLocales();
	}

	public String getLocalName() {
		return wrapperedHttpRequest.getLocalName();
	}

	public int getLocalPort() {
		return wrapperedHttpRequest.getLocalPort();
	}

	public String getMethod() {
		return wrapperedHttpRequest.getMethod();
	}

	public String getParameter(String name) {
		return wrapperedHttpRequest.getParameter(name);
	}

	public Map getParameterMap() {
		return wrapperedHttpRequest.getParameterMap();
	}

	public Enumeration getParameterNames() {
		return wrapperedHttpRequest.getParameterNames();
	}

	public String[] getParameterValues(String name) {
		return wrapperedHttpRequest.getParameterValues(name);
	}

	public String getPathInfo() {
		return wrapperedHttpRequest.getPathInfo();
	}

	public String getPathTranslated() {
		return wrapperedHttpRequest.getPathTranslated();
	}

	public String getProtocol() {
		return wrapperedHttpRequest.getProtocol();
	}

	public String getQueryString() {
		return wrapperedHttpRequest.getQueryString();
	}

	public BufferedReader getReader() throws IOException {
		return wrapperedHttpRequest.getReader();
	}

	public String getRealPath(String path) {
		return wrapperedHttpRequest.getRealPath(path);
	}

	public String getRemoteAddr() {
		return wrapperedHttpRequest.getRemoteAddr();
	}

	public String getRemoteHost() {
		return wrapperedHttpRequest.getRemoteHost();
	}

	public int getRemotePort() {
		return wrapperedHttpRequest.getRemotePort();
	}

	public String getRemoteUser() {
		return wrapperedHttpRequest.getRemoteUser();
	}

	public RequestDispatcherAdapter getRequestDispatcher(String path) {
		return new RequestDispatcherAdapter(wrapperedHttpRequest.getRequestDispatcher(path));
	}

	public String getRequestedSessionId() {
		return wrapperedHttpRequest.getRequestedSessionId();
	}

	public String getRequestURI() {
		return wrapperedHttpRequest.getRequestURI();
	}

	public StringBuffer getRequestURL() {
		return wrapperedHttpRequest.getRequestURL();
	}

	public String getScheme() {
		return wrapperedHttpRequest.getScheme();
	}

	public String getServerName() {
		return wrapperedHttpRequest.getServerName();
	}

	public int getServerPort() {
		return wrapperedHttpRequest.getServerPort();
	}

	public String getServletPath() {
		return wrapperedHttpRequest.getServletPath();
	}

	public HttpSessionAdapter getSession() {
		if (sessionWrapper == null)
			sessionWrapper = new HttpSessionAdapter(wrapperedHttpRequest.getSession());
		return sessionWrapper;
	}

	public HttpSessionAdapter getSession(boolean create) {
		if (!create)
			return getSession();
		// If need create, update buffered sessionWrapper in the same time;
		sessionWrapper = new HttpSessionAdapter(wrapperedHttpRequest.getSession(create));
		return sessionWrapper;
	}

	public Principal getUserPrincipal() {
		return wrapperedHttpRequest.getUserPrincipal();
	}

	public boolean isRequestedSessionIdFromCookie() {
		return wrapperedHttpRequest.isRequestedSessionIdFromCookie();
	}

	public boolean isRequestedSessionIdFromUrl() {
		return wrapperedHttpRequest.isRequestedSessionIdFromUrl();
	}

	public boolean isRequestedSessionIdFromURL() {
		return wrapperedHttpRequest.isRequestedSessionIdFromURL();
	}

	public boolean isRequestedSessionIdValid() {
		return wrapperedHttpRequest.isRequestedSessionIdValid();
	}

	public boolean isSecure() {
		return wrapperedHttpRequest.isSecure();
	}

	public boolean isUserInRole(String role) {
		return wrapperedHttpRequest.isUserInRole(role);
	}

	public void removeAttribute(String name) {
		wrapperedHttpRequest.removeAttribute(name);
	}

	public void setAttribute(String name, Object o) {
		wrapperedHttpRequest.setAttribute(name, o);
	}

	public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
		wrapperedHttpRequest.setCharacterEncoding(env);
	}

	public Object getOriginalObject() {
		return wrapperedHttpRequest;
	}
}
