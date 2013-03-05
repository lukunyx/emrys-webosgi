package org.emrys.webosgi.launcher.internal.adapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * 因为要做到兼容servlet2.4咄1�75规范＄1�7
 * 这里将从服务器传递过来的ServletRequest做重新的封装，�1�7�是用所有的反射方法调用两个2
 * .5规范新增的方法，比如getContextPath等�1�7�1�7
 * 
 * @author Leo Chang
 * @version 2011-4-18
 */
public class HttpServletRequestAdapter implements IServletObjectWrapper {
	private final HttpServletRequest wrapperedHttpRequest;
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
			inputWrapper = new ServletInputStreamAdapter(wrapperedHttpRequest
					.getInputStream());
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
		return new RequestDispatcherAdapter(wrapperedHttpRequest
				.getRequestDispatcher(path));
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
			sessionWrapper = new HttpSessionAdapter(wrapperedHttpRequest
					.getSession());
		return sessionWrapper;
	}

	public HttpSessionAdapter getSession(boolean create) {
		// If not require to create one if not found current session, just
		// return null in this case.
		if (!create) {
			HttpSession s = wrapperedHttpRequest.getSession(false);
			if (s == null)
				return null;
		}
		return getSession();
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

	public void setCharacterEncoding(String env)
			throws UnsupportedEncodingException {
		wrapperedHttpRequest.setCharacterEncoding(env);
	}

	public Object getOriginalObject() {
		return wrapperedHttpRequest;
	}
}
