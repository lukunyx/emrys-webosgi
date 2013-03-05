package org.emrys.webosgi.core.jeewrappers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestAttributeEvent;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.emrys.webosgi.common.ComActivator;
import org.emrys.webosgi.core.WebComActivator;
import org.emrys.webosgi.core.internal.FwkRuntime;
import org.emrys.webosgi.core.jeeres.ListenerInfo;
import org.emrys.webosgi.core.runtime.OSGiWebContainer;
import org.emrys.webosgi.launcher.internal.adapter.HttpServletRequestAdapter;
import org.emrys.webosgi.launcher.internal.adapter.IServletObjectWrapper;
import org.osgi.framework.Bundle;

/**
 * 
 * @author Leo Chang
 */
public class BundledHttpServletRequestWrapper implements HttpServletRequest,
		IServletObjectWrapper {
	/**
	 * servletRequest
	 */
	ServletRequest servletRequest;
	/**
	 * 
	 */
	HttpServletRequestAdapter httpServletRequestAdatper;
	/**
	 * 
	 */
	private Bundle bundle;
	/**
	 * 
	 */
	private String servletPath = null;
	/**
	 * 
	 */
	private String pathInfo;
	/**
	 * 
	 */
	private boolean servletPathSetted = false;
	/**
	 * 
	 */
	private boolean pathInfoSetted = false;
	/**
	 * 
	 */
	private ServletInputStreamWrapper inputWrapper;
	private HttpServletRequest httpServletRequest = null;
	private Map<String, Object> newParameters;
	private boolean isInclude = false;
	private boolean isDispatched;
	protected BundledHttpServletRequestWrapper topWrapper;
	private String newReqURI;
	private String newReqURL;
	/**
	 * 
	 */
	private static Set<BundledHttpServletRequestWrapper> wrappers = new HashSet<BundledHttpServletRequestWrapper>();

	/**
	 * Get HttpServlet Wrapper for a Request Object(optional types are
	 * {@link javax.servlet.http.HttpServletRequest} and
	 * {@link org.emrys.webosgi.launcher.internal.adapter.HttpServletRequestAdapter}
	 * . If this object is already a
	 * {@link org.emrys.webosgi.core.jeewrappers.BundledHttpServletRequestWrapper}
	 * object, return itself immediately. Note: if wrapper succeded, the Request
	 * Thread Variant - Top Request's bundle will be changed as well.
	 * 
	 * 
	 * @param req
	 *            req
	 * @param bundle
	 *            bundle
	 * @return the wrapper object
	 */
	public static synchronized BundledHttpServletRequestWrapper getHttpServletRequestWrapper(
			Object req, Bundle bundle) {
		if (req == null || req instanceof BundledHttpServletRequestWrapper)
			return (BundledHttpServletRequestWrapper) req;

		/* synchronized (wrappers) */{
			for (BundledHttpServletRequestWrapper w : wrappers) {
				if (w.getOriginalObject().equals(req)) {
					w.setBundle(bundle);
					return w;
				}
			}

			BundledHttpServletRequestWrapper newWrapper = null;
			if (req instanceof HttpServletRequestAdapter) {
				newWrapper = new BundledHttpServletRequestWrapper(
						(HttpServletRequestAdapter) req, bundle);
				newWrapper.topWrapper = null;
			}

			if (req instanceof HttpServletRequest) {
				newWrapper = new BundledHttpServletRequestWrapper(
						(HttpServletRequest) req, bundle);
				newWrapper.topWrapper = (BundledHttpServletRequestWrapper) FwkRuntime
						.getInstance().getWebContainer().getReqThreadVariants()
						.get(OSGiWebContainer.THREAD_V_REQUEST);
				newWrapper.topWrapper.setBundle(bundle);
			}

			if (newWrapper != null) {
				// Only buffer Top Response Wrapper and it's be released after
				// service() invoked in OSGiWebContainer.
				if (newWrapper.topWrapper == null)
					wrappers.add(newWrapper);
				return newWrapper;
			}
			return null;
		}
	}

	/**
	 * Release each Wrappered request after service completed in a Servlet.
	 * 
	 * @param req
	 *            r
	 */
	public static synchronized void releaseRequestWrapper(HttpServletRequest req) {
		wrappers.remove(req);
	}

	/**
	 * @param req
	 *            r
	 * @param bundleb
	 */
	protected BundledHttpServletRequestWrapper(HttpServletRequestAdapter req,
			Bundle bundle) {
		this.httpServletRequestAdatper = req;
		this.bundle = bundle;
	}

	protected BundledHttpServletRequestWrapper(HttpServletRequest req,
			Bundle bundle) {
		this.httpServletRequest = req;
		this.bundle = bundle;
	}

	public Bundle getBundle() {
		return bundle;
	}

	public void setBundle(Bundle bundle) {
		Bundle oldBundle = this.bundle;
		this.bundle = bundle;
		if (bundle != null && bundle != oldBundle) {
			HttpSession session = getSession();
			if (session instanceof HttpSessionWrapper) {
				// Reactive when request arrive and retrieve the session.
				((HttpSessionWrapper) session).reactive(this);
			}
		}
	}

	public Enumeration getAttributeNames() {
		if (httpServletRequest != null)
			return httpServletRequest.getAttributeNames();
		else
			return httpServletRequestAdatper.getAttributeNames();
	}

	public String getAuthType() {
		if (httpServletRequest != null)
			return httpServletRequest.getAuthType();
		else
			return httpServletRequestAdatper.getAuthType();
	}

	public String getContextPath() {
		if (httpServletRequest != null)
			return httpServletRequest.getSession().getServletContext()
					.getContextPath();
		else
			return getSession().getServletContext().getContextPath();
	}

	public Cookie[] getCookies() {
		if (httpServletRequest != null)
			return httpServletRequest.getCookies();
		else
			return CookieWrapper.adaptArray(httpServletRequestAdatper
					.getCookies());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.servlet.http.HttpServletRequest#getDateHeader(java.lang.String)
	 */
	public long getDateHeader(String name) {
		if (httpServletRequest != null)
			return httpServletRequest.getDateHeader(name);
		else
			return httpServletRequestAdatper.getDateHeader(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServletRequest#getHeader(java.lang.String)
	 */
	public String getHeader(String name) {
		if (httpServletRequest != null)
			return httpServletRequest.getHeader(name);
		else
			return httpServletRequestAdatper.getHeader(name);
	}

	public Enumeration getHeaderNames() {
		if (httpServletRequest != null)
			return httpServletRequest.getHeaderNames();
		else
			return httpServletRequestAdatper.getHeaderNames();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServletRequest#getHeaders(java.lang.String)
	 */
	public Enumeration getHeaders(String name) {
		if (httpServletRequest != null)
			return httpServletRequest.getHeaders(name);
		else
			return httpServletRequestAdatper.getHeaders(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServletRequest#getIntHeader(java.lang.String)
	 */
	public int getIntHeader(String name) {
		if (httpServletRequest != null)
			return httpServletRequest.getIntHeader(name);
		else
			return httpServletRequestAdatper.getIntHeader(name);
	}

	public String getMethod() {
		if (httpServletRequest != null)
			return httpServletRequest.getMethod();
		else
			return httpServletRequestAdatper.getMethod();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServletRequest#getPathInfo()
	 */
	public String getPathInfo() {
		if (pathInfoSetted)
			return pathInfo;

		if (httpServletRequest != null)
			return httpServletRequest.getPathInfo();
		else {
			// For weblogic9, if the request's url like http://host/webappname ,
			// the pathinfo will
			// be null, this may cause NPT excetpion later, here return empty
			// string in this case.
			// Tomcat or Jboss will set the pathinfo as empty string.
			pathInfo = httpServletRequestAdatper.getPathInfo();
			pathInfoSetted = true;
			// We no longer let the null path info as empty "" string according
			// to Tomcat6's behavior.
			// if(pathInfo==null)
			// return "";
			return pathInfo;
		}
	}

	/**
	 * Set new path info. Only for internal use.
	 * 
	 * @param pathInfo
	 *            p
	 */
	public void setPathInfo(String pathInfo) {
		// FIXME: It must not offended to wrapperd Request if path info changed?
		/*
		 * if (topWrapper != null) topWrapper.setPathInfo(pathInfo);
		 */
		this.pathInfoSetted = true;
		this.pathInfo = pathInfo;
	}

	public String getPathTranslated() {
		if (httpServletRequest != null)
			return httpServletRequest.getPathTranslated();
		else
			return httpServletRequestAdatper.getPathTranslated();
	}

	public String getQueryString() {
		if (httpServletRequest != null)
			return httpServletRequest.getQueryString();
		else
			return httpServletRequestAdatper.getQueryString();
	}

	public String getRemoteUser() {
		if (httpServletRequest != null)
			return httpServletRequest.getRemoteUser();
		else
			return httpServletRequestAdatper.getRemoteUser();
	}

	public String getRequestedSessionId() {
		if (httpServletRequest != null)
			return httpServletRequest.getRequestedSessionId();
		else
			return httpServletRequestAdatper.getRequestedSessionId();
	}

	public void setRequestURI(String newReqURI) {
		this.newReqURI = newReqURI;
	}

	public String getRequestURI() {
		// If forward dispathed, the original URI maybe changed.
		if (isDispatched && !isInclude && newReqURI != null)
			return newReqURI;

		if (httpServletRequest != null)
			return httpServletRequest.getRequestURI();
		else {
			return httpServletRequestAdatper.getRequestURI();
		}
	}

	public void setRequestURL(String newReqURL) {
		this.newReqURL = newReqURL;
	}

	public StringBuffer getRequestURL() {
		// If forward dispathed, the original URL maybe changed.
		if (isDispatched && !isInclude && newReqURL != null)
			return new StringBuffer(newReqURL);

		if (httpServletRequest != null)
			return httpServletRequest.getRequestURL();
		else
			return httpServletRequestAdatper.getRequestURL();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServletRequest#getServletPath()
	 */
	public String getServletPath() {
		if (servletPathSetted)
			return servletPath == null ? "" : servletPath;
		if (httpServletRequest != null)
			return httpServletRequest.getServletPath();
		else
			return httpServletRequestAdatper.getServletPath();
	}

	/**
	 * Set new servelt path. This method only for internal use.
	 * 
	 * @param servletPath
	 *            s
	 */
	public void setServletPath(String servletPath) {
		// FIXME: It must not offended to wrapperd Request if servlet path
		// changed?
		/*
		 * if (topWrapper != null) topWrapper.setServletPath(servletPath);
		 */
		this.servletPathSetted = true;
		this.servletPath = servletPath;
	}

	public HttpSession getSession() {
		return getSession(true);
	}

	public HttpSession getSession(boolean create) {
		HttpSession session = null;
		if (httpServletRequest != null)
			session = httpServletRequest.getSession(create);
		else {
			session = HttpSessionWrapper.getHttpSessionWrapper(
					httpServletRequestAdatper.getSession(create), bundle);
		}
		// JIRA INDI-52: not check session invalid here, move the code to
		// HttpSessionWrapper.getHttpSessionWrapper() method.
		return session;
	}

	public Principal getUserPrincipal() {
		if (httpServletRequest != null)
			return httpServletRequest.getUserPrincipal();
		else
			return httpServletRequestAdatper.getUserPrincipal();
	}

	public boolean isRequestedSessionIdFromCookie() {
		if (httpServletRequest != null)
			return httpServletRequest.isRequestedSessionIdFromCookie();
		else
			return httpServletRequestAdatper.isRequestedSessionIdFromCookie();
	}

	public boolean isRequestedSessionIdFromUrl() {
		if (httpServletRequest != null)
			return httpServletRequest.isRequestedSessionIdFromUrl();
		else
			return httpServletRequestAdatper.isRequestedSessionIdFromUrl();
	}

	public boolean isRequestedSessionIdFromURL() {
		if (httpServletRequest != null)
			return httpServletRequest.isRequestedSessionIdFromURL();
		else
			return httpServletRequestAdatper.isRequestedSessionIdFromURL();
	}

	public boolean isRequestedSessionIdValid() {
		if (httpServletRequest != null)
			return httpServletRequest.isRequestedSessionIdValid();
		else
			return httpServletRequestAdatper.isRequestedSessionIdValid();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.http.HttpServletRequest#isUserInRole(java.lang.String)
	 */
	public boolean isUserInRole(String role) {
		if (httpServletRequest != null)
			return httpServletRequest.isUserInRole(role);
		else
			return httpServletRequestAdatper.isUserInRole(role);
	}

	public Object getOriginalObject() {
		if (httpServletRequest != null)
			return httpServletRequest;
		else
			return httpServletRequestAdatper;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletRequest#getAttribute(java.lang.String)
	 */
	public Object getAttribute(String name) {
		if (httpServletRequest != null)
			return httpServletRequest.getAttribute(name);
		else
			return httpServletRequestAdatper.getAttribute(name);
	}

	public String getCharacterEncoding() {
		if (httpServletRequest != null)
			return httpServletRequest.getCharacterEncoding();
		else
			return httpServletRequestAdatper.getCharacterEncoding();
	}

	public int getContentLength() {
		if (httpServletRequest != null)
			return httpServletRequest.getContentLength();
		else
			return httpServletRequestAdatper.getContentLength();
	}

	public String getContentType() {
		if (httpServletRequest != null)
			return httpServletRequest.getContentType();
		else
			return httpServletRequestAdatper.getContentType();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletRequest#getInputStream()
	 */
	public ServletInputStream getInputStream() throws IOException {
		if (httpServletRequest != null)
			return httpServletRequest.getInputStream();
		else {
			if (inputWrapper == null)
				inputWrapper = new ServletInputStreamWrapper(
						httpServletRequestAdatper.getInputStream());
			return inputWrapper;
		}
	}

	public String getLocalAddr() {
		if (httpServletRequest != null)
			return httpServletRequest.getLocalAddr();
		else
			return httpServletRequestAdatper.getLocalAddr();
	}

	public String getLocalName() {
		if (httpServletRequest != null)
			return httpServletRequest.getLocalName();
		else
			return httpServletRequestAdatper.getLocalName();
	}

	public int getLocalPort() {
		if (httpServletRequest != null)
			return httpServletRequest.getLocalPort();
		else
			return httpServletRequestAdatper.getLocalPort();
	}

	public Locale getLocale() {
		if (httpServletRequest != null)
			return httpServletRequest.getLocale();
		else
			return httpServletRequestAdatper.getLocale();
	}

	public Enumeration getLocales() {
		if (httpServletRequest != null)
			return httpServletRequest.getLocales();
		else
			return httpServletRequestAdatper.getLocales();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletRequest#getParameter(java.lang.String)
	 */
	public String getParameter(String name) {
		String value = null;
		if (newParameters != null && newParameters.containsKey(name)) {
			Object values = newParameters.get(name);
			if (values.getClass().isArray()) {
				Object[] objs = (Object[]) values;
				if (objs.length > 0)
					return objs[0].toString();
			}
		}

		if (httpServletRequest != null)
			value = httpServletRequest.getParameter(name);
		else
			value = httpServletRequestAdatper.getParameter(name);

		// In domino server, /path?abc=1&efg, the value obtained by
		// getParameter() method is null, however, in tomcat, the value is "".
		// This cause some JEE application can not run properly. We do our parse
		// with the query string.
		if (value == null) {
			String queryStr = getQueryString();
			if (!StringUtils.isEmpty(queryStr)) {
				int idx = queryStr.indexOf(name);
				if (idx > -1) {
					String[] segs = queryStr.split("&");
					for (String s : segs)
						if (s.equals(name))
							return "";
				}
			}
		}
		return value;
	}

	public Map getParameterMap() {
		Map map = null;
		if (httpServletRequest != null)
			map = httpServletRequest.getParameterMap();
		else {
			map = httpServletRequestAdatper.getParameterMap();
			if (newParameters != null) {
				map.putAll(newParameters);
			}
		}

		return map;
	}

	public Enumeration getParameterNames() {
		final Iterator it = getParameterMap().keySet().iterator();
		return new Enumeration() {
			public boolean hasMoreElements() {
				return it.hasNext();
			}

			public Object nextElement() {
				return it.next();
			}
		};
	}

	public String[] getParameterValues(String name) {
		Object values = getParameterMap().get(name);
		if (values != null && values.getClass().isArray()) {
			Object[] objs = (Object[]) values;
			String[] result = new String[objs.length];

			for (int i = 0; i < objs.length; i++) {
				result[i] = objs[i].toString();
			}
			return result;
		}
		return null;
	}

	public String getProtocol() {
		if (httpServletRequest != null)
			return httpServletRequest.getProtocol();
		else
			return httpServletRequestAdatper.getProtocol();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletRequest#getReader()
	 */
	public BufferedReader getReader() throws IOException {
		if (httpServletRequest != null)
			return httpServletRequest.getReader();
		else
			return httpServletRequestAdatper.getReader();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletRequest#getRealPath(java.lang.String)
	 */
	public String getRealPath(String path) {
		if (httpServletRequest != null)
			return httpServletRequest.getRealPath(path);
		else
			return httpServletRequestAdatper.getRealPath(path);
	}

	public String getRemoteAddr() {
		if (httpServletRequest != null)
			return httpServletRequest.getRemoteAddr();
		else
			return httpServletRequestAdatper.getRemoteAddr();
	}

	public String getRemoteHost() {
		if (httpServletRequest != null)
			return httpServletRequest.getRemoteHost();
		else
			return httpServletRequestAdatper.getRemoteHost();
	}

	public int getRemotePort() {
		if (httpServletRequest != null)
			return httpServletRequest.getRemotePort();
		else
			return httpServletRequestAdatper.getRemotePort();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletRequest#getRequestDispatcher(java.lang.String)
	 */
	public RequestDispatcher getRequestDispatcher(String path) {
		if (httpServletRequest != null)
			return httpServletRequest.getRequestDispatcher(path);
		else {
			// dispathcer get from ServletContext, the given path is relative
			// to the context root, and if get from ServletRequest, it's
			// relative to the current servlet path of that request if not
			// started with "/".
			if (!path.startsWith("/")) {
				String servletPath = getServletPath();
				if (servletPath == null || servletPath.length() == 0
						|| servletPath == "/")
					path = "/" + path;
				else {
					// Remove the last segment of current servlet path and apped
					// the target path to search target resource relative the
					// current servlet.
					path = servletPath.substring(0, servletPath
							.lastIndexOf('/'))
							+ "/" + path;
				}
			}
			return this.getSession().getServletContext().getRequestDispatcher(
					path);
		}
	}

	public String getScheme() {
		if (httpServletRequest != null)
			return httpServletRequest.getScheme();
		else
			return httpServletRequestAdatper.getScheme();
	}

	public String getServerName() {
		if (httpServletRequest != null)
			return httpServletRequest.getServerName();
		else
			return httpServletRequestAdatper.getServerName();
	}

	public int getServerPort() {
		if (httpServletRequest != null)
			return httpServletRequest.getServerPort();
		else
			return httpServletRequestAdatper.getServerPort();
	}

	public boolean isSecure() {
		if (httpServletRequest != null)
			return httpServletRequest.isSecure();
		else
			return httpServletRequestAdatper.isSecure();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletRequest#removeAttribute(java.lang.String)
	 */
	public void removeAttribute(String name) {
		if (httpServletRequest != null)
			httpServletRequest.removeAttribute(name);
		else {
			Object oValue = httpServletRequestAdatper.getAttribute(name);
			httpServletRequestAdatper.removeAttribute(name);
			// Process Request Attribute removed event.
			if (oValue == null) {
				ServletContext ctx = this.getSession().getServletContext();
				ServletRequestAttributeListener[] listeners = findReqAttrListeners();
				for (int i = 0; i < listeners.length; i++) {
					listeners[i]
							.attributeRemoved(new ServletRequestAttributeEvent(
									ctx, this, name, oValue));
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletRequest#setAttribute(java.lang.String,
	 * java.lang.Object)
	 */
	public void setAttribute(String name, Object o) {
		if (httpServletRequest != null)
			httpServletRequest.setAttribute(name, o);
		else {
			Object oValue = httpServletRequestAdatper.getAttribute(name);
			httpServletRequestAdatper.setAttribute(name, o);

			// Process Request Attribute change event.
			ServletContext ctx = this.getSession().getServletContext();
			ServletRequestAttributeListener[] listeners = findReqAttrListeners();
			for (int i = 0; i < listeners.length; i++) {
				if (oValue != null) {
					if (o != null)
						listeners[i]
								.attributeReplaced(new ServletRequestAttributeEvent(
										ctx, this, name, o));
					else
						listeners[i]
								.attributeRemoved(new ServletRequestAttributeEvent(
										ctx, this, name, o));
				} else if (o != null) {
					listeners[i]
							.attributeAdded(new ServletRequestAttributeEvent(
									ctx, this, name, o));
				}
			}
		}
	}

	private ServletRequestAttributeListener[] findReqAttrListeners() {
		if (bundle != null) {
			ComActivator activator = FwkRuntime.getInstance()
					.getBundleActivator(bundle.getBundleId());
			if (activator instanceof WebComActivator) {
				List<ServletRequestAttributeListener> result = new ArrayList<ServletRequestAttributeListener>();
				Collection<ListenerInfo> listeners = ((WebComActivator) activator)
						.getBundleServletContext().getListeners();
				for (Iterator<ListenerInfo> it = listeners.iterator(); it
						.hasNext();) {
					ListenerInfo listener = it.next();
					if (listener instanceof ServletRequestAttributeListener) {
						result.add((ServletRequestAttributeListener) listener);
					}
				}
				return result
						.toArray(new ServletRequestAttributeListener[result
								.size()]);
			}
		}
		return new ServletRequestAttributeListener[0];
	}

	public void setCharacterEncoding(String env)
			throws UnsupportedEncodingException {
		if (httpServletRequest != null)
			httpServletRequest.setCharacterEncoding(env);
		else
			httpServletRequestAdatper.setCharacterEncoding(env);
	}

	public boolean isInclude() {
		if (topWrapper != null)
			return topWrapper.isInclude();
		return isInclude;
	}

	public void setInclude(boolean isInclude) {
		if (topWrapper != null)
			topWrapper.setInclude(isInclude);
		this.isInclude = isInclude;
	}

	public boolean isDispatched() {
		if (topWrapper != null)
			return topWrapper.isDispatched();
		return isDispatched;
	}

	public void setDispatched(boolean isDispatched) {
		if (topWrapper != null)
			topWrapper.setDispatched(isDispatched);
		this.isDispatched = isDispatched;
	}

	public BundledHttpServletRequestWrapper getTopWrapper() {
		// If no top wrapper assigneed, this wrapper itself is a top one.
		if (topWrapper == null)
			return this;
		return topWrapper;
	}

	@Override
	public boolean equals(Object obj) {
		if (httpServletRequest != null)
			return httpServletRequest.equals(obj);
		else {
			if (obj instanceof BundledHttpServletRequestWrapper)
				return httpServletRequestAdatper
						.equals(((BundledHttpServletRequestWrapper) obj)
								.getOriginalObject());
			return httpServletRequestAdatper.equals(obj);
		}
	}

	@Override
	public int hashCode() {
		if (httpServletRequest != null)
			return httpServletRequest.hashCode();
		else
			return httpServletRequestAdatper.hashCode();
	}

	public Map<String, Object> getNewParameters() {
		// Merge top Reqeust wrapper's additional parameters.
		if (newParameters == null) {
			if (topWrapper != null)
				newParameters = new MergedMapWrapper<String, Object>(topWrapper
						.getNewParameters());
			else
				newParameters = new HashMap<String, Object>();
		}
		return newParameters;
	}

	/**
	 * Delegate Map to top request wrapper's additional parameters, but not
	 * delegate the modification of the current map.
	 * 
	 * @author LeoChang
	 * 
	 * @param <K>
	 * @param <V>
	 */
	private static class MergedMapWrapper<K, V> extends HashMap<K, V> implements
			Map<K, V> {
		private static final long serialVersionUID = -4026765728438567532L;
		private Map<K, V> delegate;

		public MergedMapWrapper(Map<K, V> delegate) {
			if (delegate == null)
				throw new IllegalArgumentException(
						"Argument delegate can not be null.");
			this.delegate = delegate;
		}

		public Map<K, V> getDelegate() {
			// May be obtain dynimically.
			return delegate;
		}

		@Override
		public Object clone() {
			MergedMapWrapper cloned = (MergedMapWrapper) super.clone();
			cloned.delegate = getDelegate();
			return cloned;
		}

		@Override
		public boolean containsKey(Object paramObject) {
			boolean result = super.containsKey(paramObject);
			if (!result)
				result = getDelegate().containsKey(paramObject);
			return result;
		}

		@Override
		public boolean containsValue(Object paramObject) {
			boolean result = super.containsValue(paramObject);
			if (!result)
				result = getDelegate().containsValue(paramObject);
			return result;
		}

		@Override
		public Set<Entry<K, V>> entrySet() {
			Set<Entry<K, V>> result = new HashSet<Entry<K, V>>();
			result.addAll(getDelegate().entrySet());
			result.addAll(super.entrySet());
			return result;
		}

		@Override
		public V get(Object paramObject) {
			V result = super.get(paramObject);
			if (result == null)
				result = getDelegate().get(paramObject);
			return result;
		}

		@Override
		public boolean isEmpty() {
			return super.isEmpty() && getDelegate().isEmpty();
		}

		@Override
		public Set<K> keySet() {
			Set<K> result = new HashSet<K>();
			result.addAll(getDelegate().keySet());
			result.addAll(super.keySet());
			return result;
		}

		@Override
		public int size() {
			return super.size() + getDelegate().size();
		}

		@Override
		public Collection<V> values() {
			Collection<V> result = new ArrayList<V>();
			result.addAll(super.values());
			result.addAll(getDelegate().values());
			return result;
		}

		@Override
		public boolean equals(Object paramObject) {
			return super.equals(paramObject)
					&& getDelegate().equals(
							((MergedMapWrapper) paramObject).getDelegate());
		}

		@Override
		public String toString() {
			return super.toString() + " " + getDelegate().toString();
		}
	}

	/**
	 * Parse new parameters. NOTE: ?arg1=abc&arg2=abc&arg3&arg2=123
	 * 
	 * @param queryStr
	 */
	public void parseNewParameters(String queryStr) {
		String[] tmps = queryStr.split("&");

		for (int i = 0; i < tmps.length; i++) {
			String tmpSeg = tmps[i];
			if (StringUtils.isEmpty(tmpSeg))
				continue;

			String name = null;
			// If name exists, the null value should regarded as empty
			// string by default.
			String value = "";
			int idx = tmpSeg.indexOf('=');
			if (idx > 0) {
				name = tmpSeg.substring(0, idx);
				if (idx < tmpSeg.length() - 1)
					value = tmpSeg.substring(idx + 1);
			} else if (idx == 0)
				continue;
			else
				name = tmpSeg;

			Object buffered = getNewParameters().get(name);
			// Not check value empty, we regard the empty or null string
			// as a value.
			// if (!StringUtils.isEmpty(value))
			if (buffered != null && !buffered.getClass().isArray())
				continue;

			if (buffered == null)
				buffered = new String[0];

			// Copy the old parameter values.
			int length = Array.getLength(buffered);
			String[] newValues = new String[length + 1];
			for (int j = 0; j < length; j++) {
				String ele = (String) Array.get(buffered, j);
				// Not check value repeat.
				// if (ele.equals(value))
				newValues[j] = ele;
			}
			// Append this value to the last of the values array.
			newValues[newValues.length - 1] = value;

			getNewParameters().put(name, newValues);
		}

	}
}
