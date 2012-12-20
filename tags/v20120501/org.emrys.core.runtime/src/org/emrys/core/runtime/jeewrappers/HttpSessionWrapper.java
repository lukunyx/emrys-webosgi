/*******************************************************************************
 * Copyright (c) 2011 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.jeewrappers;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import org.emrys.core.runtime.FwkActivator;
import org.emrys.core.runtime.jeecontainer.IBundledServletContext;
import org.osgi.framework.Bundle;

import org.emrys.core.adapter.internal.HttpSessionAdapter;
import org.emrys.core.adapter.internal.IServletObjectWrapper;

/**
 * The wrapper class for
 * {@link org.eclipse.equinox.servletbridge.HttpSessionAdapter}
 * 
 * @author Leo Chang - EMRYS
 * @version 2011-3-22
 */
public class HttpSessionWrapper implements HttpSession, IServletObjectWrapper {
	private static final String SESSION_ID_SEPERATOR = "___";
	private static long uniqueSessionIndex;
	private static Set<HttpSessionWrapper> wrappers = new HashSet<HttpSessionWrapper>();

	private final Timer invalideTimer = new Timer();

	/**
	 * The facade to wrapper a given http session.
	 * 
	 * @param session
	 * @param bundle
	 * @return
	 */
	public static synchronized HttpSessionWrapper getHttpSessionWrapper(
			Object session, Bundle bundle) {
		if (session == null || session instanceof HttpSessionWrapper) {
			HttpSessionWrapper sessionWrapper = (HttpSessionWrapper) session;
			// No need to set bundle again, for we maintain each session for
			// each bundle already.
			// sessionWrapper.setBundle(bundle);
			return sessionWrapper;
		}

		if (!(session instanceof HttpSessionAdapter))
			return null;

		Iterator<HttpSessionWrapper> it = wrappers.iterator();
		while (it.hasNext()) {
			HttpSessionWrapper w = it.next();
			// JIRA INDI-52:
			// Check if the current Session invalid, if true, remove
			// buffer.
			// FIXME: If not register HttpSessionListener to external Servlet
			// Container to listen the Session Destroy Event and release wapper
			// here, some invalid session may can not be release.
			if (w.isInvalid()) {
				it.remove();
				continue;
			}

			// if same bundle, either null or not.
			if (w.getBundle() == bundle) {
				if (((HttpSessionAdapter) w.getOriginalObject()).getId()
						.equals(((HttpSessionAdapter) session).getId())) {
					return w;
				}
			}
		}

		// If not found, create a new session for this bundle and buffer it.
		// No need to worry about the no use session and memory leak for
		// releaseHttpSessionWrapper method will be invoked from
		// BundledHttpServletReqeustWrapper case to create a new session with
		// unique session id suffix with the global index number.
		HttpSessionWrapper newWrapper = new HttpSessionWrapper(
				(HttpSessionAdapter) session, bundle);
		wrappers.add(newWrapper);
		return newWrapper;
	}

	/**
	 * @param sessionWraper
	 */
	protected static synchronized void releaseHttpSessionWrapper(
			HttpSessionWrapper sessionWraper) {
		wrappers.remove(sessionWraper);
	}

	Map<String, Object> attributes = new HashMap<String, Object>();
	// Any need to use WeakReference to HttpSessionAdapter?
	HttpSessionAdapter wrapperedSession;
	Bundle bundle;
	boolean isInvalid = false;
	private final String id;
	private final long createdTime;
	private TimerTask autoInvalidateTimerTask;
	private int interval;

	/**
	 * @param request
	 */
	protected HttpSessionWrapper(HttpSessionAdapter httpSessionAdapter,
			Bundle bundle) {
		this.wrapperedSession = httpSessionAdapter;
		this.bundle = bundle;
		// Generate unique id by suffixed the original id with a global index
		// number.
		uniqueSessionIndex++;
		id = httpSessionAdapter.getId() + SESSION_ID_SEPERATOR
				+ Long.toString(uniqueSessionIndex, 16);
		createdTime = System.currentTimeMillis();
		// Set valid timeout interval.
		IBundledServletContext ctx = (IBundledServletContext) getServletContext();
		setMaxInactiveInterval(ctx.getSessionTimeout());
	}

	/**
	 * Judge if the session has been invalidated.
	 * 
	 * @return
	 */
	public boolean isInvalid() {
		if (isInvalid)
			return true;
		try {
			// Check out if the Session invalid by setting a tmp attribute.
			wrapperedSession.setAttribute("ATTR_TMP7458169", null);
		} catch (IllegalStateException e) {
			// e.printStackTrace();
			return true;
		}

		return false;
	}

	public Bundle getBundle() {
		return bundle;
	}

	public void setBundle(Bundle bundle) {
		this.bundle = bundle;
	}

	public Object getAttribute(String name) {
		if (name == null)
			throw new IllegalArgumentException(
					"The name of the desiring attribute cann't be null.");
		Object value = attributes.get(name);
		if (value != null)
			return value;
		return wrapperedSession.getAttribute(name);
	}

	public Enumeration<String> getAttributeNames() {
		final Iterator<String> akit = attributes.keySet().iterator();
		final Enumeration<String> wrapperedSessionAttrNames = wrapperedSession
				.getAttributeNames();
		return new Enumeration<String>() {
			public boolean hasMoreElements() {
				if (akit.hasNext())
					return true;
				return wrapperedSessionAttrNames.hasMoreElements();
			}

			public String nextElement() {
				if (akit.hasNext())
					return akit.next();

				return wrapperedSessionAttrNames.nextElement();
			}
		};
		// return wrapperedSession.getAttributeNames();
	}

	public long getCreationTime() {
		return createdTime;
	}

	public String getId() {
		return id;
	}

	public long getLastAccessedTime() {
		return wrapperedSession.getLastAccessedTime();
	}

	public int getMaxInactiveInterval() {
		return interval;
	}

	public ServletContext getServletContext() {
		return FwkActivator.getInstance().getJeeContainer().findServletContext(
				bundle);
	}

	public HttpSessionContext getSessionContext() {
		return wrapperedSession.getSessionContext();
	}

	public Object getValue(String name) {
		return getAttribute(name);
		// return wrapperedSession.getValue(name);
	}

	public String[] getValueNames() {
		Enumeration<String> names = this.getAttributeNames();
		List<String> result = new ArrayList<String>();
		while (names.hasMoreElements())
			result.add(names.nextElement());

		return result.toArray(new String[result.size()]);
		// return wrapperedSession.getValueNames();
	}

	public void invalidate() {
		// If a local session invalid, do not call wrapperedSession.invalidate()
		// method .We do not want any offend to other web bundle. OSGi JEE
		// container should maintain the life-cycle of a session itself. Here we
		// not need to release it from buffer, for this job has been done from
		// BundledHttpServletRequestWrapper.getSession() method if found a
		// obtained session is invalid.
		// releaseHttpSessionWrapper(this);
		isInvalid = true;
		// wrapperedSession.invalidate();
	}

	public boolean isNew() {
		return wrapperedSession.isNew();
	}

	public void putValue(String name, Object value) {
		setAttribute(name, value);
		// wrapperedSession.putValue(name, value);
	}

	public void removeAttribute(String name) {
		if (isInvalid())
			throw new IllegalStateException(
					"This session has been invalidated.");
		if (name != null)
			attributes.remove(name);
		// wrapperedSession.removeAttribute(name);
	}

	public void removeValue(String name) {
		removeAttribute(name);
		// wrapperedSession.removeValue(name);
	}

	public void setAttribute(String name, Object value) {
		if (isInvalid())
			throw new IllegalStateException(
					"This session has been invalidated.");

		if (name == null || name.length() == 0)
			throw new IllegalArgumentException(
					"The name of the value cann't be null or empty");
		if (value == null)
			removeAttribute(name);
		else
			attributes.put(name, value);
	}

	public void setMaxInactiveInterval(int interval) {
		// Set automatically-invalidating timer task.
		this.interval = interval;
		if (autoInvalidateTimerTask != null)
			autoInvalidateTimerTask.cancel();
		if (interval > 0) {
			autoInvalidateTimerTask = new TimerTask() {
				@Override
				public void run() {
					HttpSessionWrapper.this.invalidate();
				}
			};
			invalideTimer.schedule(autoInvalidateTimerTask, interval);
		}
	}

	public Object getOriginalObject() {
		return wrapperedSession;
	}

	@Override
	public boolean equals(Object obj) {
		// Regard 2 session wrappers equal only when its wrapped session adapter
		// equals and with same bundle.
		if (this == obj)
			return true;

		if (obj instanceof HttpSessionWrapper)
			return wrapperedSession.equals(((HttpSessionWrapper) obj)
					.getOriginalObject())
					&& bundle.equals(((HttpSessionWrapper) obj).getBundle());

		return false;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}
}
