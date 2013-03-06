package org.emrys.webosgi.launcher.internal.adapter;

import javax.servlet.http.Cookie;

/**
 * Wrapper the ICookie interface back to Cookie type. This class is used to
 * receive the Cookie added from wrappered ServletResponse from framework.
 * 
 * @author Leo Chang
 * @version 2011-6-1
 */
public class CookieWrapper extends Cookie {

	private final ICookie cookie;

	@Override
	public Object clone() {
		return super.clone();
	}

	public CookieWrapper(ICookie cookie) {
		super(cookie.getName(), cookie.getValue());
		this.cookie = cookie;
	}

	public Object getOriginalObject() {
		return cookie;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof CookieWrapper)
			return cookie.equals(((CookieWrapper) obj).getOriginalObject());
		return cookie.equals(obj);
	}

	@Override
	public String getComment() {
		return cookie.getComment();
	}

	@Override
	public String getDomain() {
		return cookie.getDomain();
	}

	@Override
	public int getMaxAge() {
		return cookie.getMaxAge();
	}

	@Override
	public String getName() {
		return cookie.getName();
	}

	@Override
	public String getPath() {
		return cookie.getPath();
	}

	@Override
	public boolean getSecure() {
		return cookie.getSecure();
	}

	@Override
	public String getValue() {
		return cookie.getValue();
	}

	@Override
	public int getVersion() {
		return cookie.getVersion();
	}

	@Override
	public int hashCode() {
		return cookie.hashCode();
	}

	@Override
	public void setComment(String purpose) {
		cookie.setComment(purpose);
	}

	@Override
	public void setDomain(String pattern) {
		cookie.setDomain(pattern);
	}

	@Override
	public void setMaxAge(int expiry) {
		cookie.setMaxAge(expiry);
	}

	@Override
	public void setPath(String uri) {
		cookie.setPath(uri);
	}

	@Override
	public void setSecure(boolean flag) {
		cookie.setSecure(flag);
	}

	@Override
	public void setValue(String newValue) {
		cookie.setValue(newValue);
	}

	@Override
	public void setVersion(int v) {
		cookie.setVersion(v);
	}

	@Override
	public String toString() {
		return cookie.toString();
	}
}
