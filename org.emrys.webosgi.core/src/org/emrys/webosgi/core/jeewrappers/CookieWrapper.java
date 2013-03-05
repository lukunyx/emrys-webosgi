package org.emrys.webosgi.core.jeewrappers;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.Cookie;

import org.emrys.webosgi.launcher.internal.adapter.CookieAdapter;
import org.emrys.webosgi.launcher.internal.adapter.ICookie;

/**
 * 
 * @author Leo Chang
 * @version 2011-6-1
 */
public class CookieWrapper extends Cookie implements ICookie {

	private CookieAdapter cookieAdapter = null;
	private Cookie cookie = null;

	public static CookieWrapper[] adaptArray(CookieAdapter[] cookies) {
		if (cookies != null && cookies.length > 0) {
			List<CookieWrapper> result = new ArrayList<CookieWrapper>();
			for (CookieAdapter c : cookies) {
				result.add(new CookieWrapper(c));
			}
			return result.toArray(new CookieWrapper[result.size()]);
		}
		return new CookieWrapper[0];
	}

	public CookieWrapper(CookieAdapter cookieAdapter) {
		super(cookieAdapter.getName(), cookieAdapter.getValue());
		this.cookieAdapter = cookieAdapter;
	}

	public CookieWrapper(Cookie cookie) {
		super(cookie.getName(), cookie.getValue());
		this.cookie = cookie;
	}

	public Object getOriginalObject() {
		if (cookie != null)
			return cookie;
		return cookieAdapter;
	}

	@Override
	public boolean equals(Object obj) {
		Object res = null;
		if (cookie != null)
			res = cookie;
		else
			res = cookieAdapter;
		if (obj instanceof CookieWrapper)
			return res.equals(((CookieWrapper) obj).getOriginalObject());
		return res.equals(obj);
	}

	@Override
	public String getComment() {
		if (cookie != null)
			return cookie.getComment();
		return cookieAdapter.getComment();
	}

	@Override
	public String getDomain() {
		if (cookie != null)
			return cookie.getDomain();
		return cookieAdapter.getDomain();
	}

	@Override
	public int getMaxAge() {
		if (cookie != null)
			return cookie.getMaxAge();
		return cookieAdapter.getMaxAge();
	}

	@Override
	public String getName() {
		if (cookie != null)
			return cookie.getName();
		return cookieAdapter.getName();
	}

	@Override
	public String getPath() {
		if (cookie != null)
			return cookie.getPath();
		return cookieAdapter.getPath();
	}

	@Override
	public boolean getSecure() {
		if (cookie != null)
			return cookie.getSecure();
		return cookieAdapter.getSecure();
	}

	@Override
	public String getValue() {
		if (cookie != null)
			return cookie.getValue();
		return cookieAdapter.getValue();
	}

	@Override
	public int getVersion() {
		if (cookie != null)
			return cookie.getVersion();
		return cookieAdapter.getVersion();
	}

	@Override
	public int hashCode() {
		if (cookie != null)
			return cookie.hashCode();
		return cookieAdapter.hashCode();
	}

	@Override
	public void setComment(String purpose) {
		if (cookie != null)
			cookie.setComment(purpose);
		else
			cookieAdapter.setComment(purpose);
	}

	@Override
	public void setDomain(String pattern) {
		if (cookie != null)
			cookie.setDomain(pattern);
		else
			cookieAdapter.setDomain(pattern);
	}

	@Override
	public void setMaxAge(int expiry) {
		if (cookie != null)
			cookie.setMaxAge(expiry);
		else
			cookieAdapter.setMaxAge(expiry);
	}

	@Override
	public void setPath(String uri) {
		if (cookie != null)
			cookie.setPath(uri);
		else
			cookieAdapter.setPath(uri);
	}

	@Override
	public void setSecure(boolean flag) {
		if (cookie != null)
			cookie.setSecure(flag);
		else
			cookieAdapter.setSecure(flag);
	}

	@Override
	public void setValue(String newValue) {
		if (cookie != null)
			cookie.setValue(newValue);
		else
			cookieAdapter.setValue(newValue);
	}

	@Override
	public void setVersion(int v) {
		if (cookie != null)
			cookie.setVersion(v);
		else
			cookieAdapter.setVersion(v);
	}

	@Override
	public String toString() {
		if (cookie != null)
			return cookie.toString();
		else
			return cookieAdapter.toString();
	}
}
