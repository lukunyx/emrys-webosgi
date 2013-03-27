package org.emrys.webosgi.core.jeewrappers;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.emrys.webosgi.core.internal.FwkRuntime;
import org.emrys.webosgi.core.runtime.OSGiWebContainer;
import org.emrys.webosgi.launcher.internal.adapter.HttpServletResponseAdapter;
import org.emrys.webosgi.launcher.internal.adapter.IServletObjectWrapper;

/**
 * The wrapper class to {@link javax.serlvet.HttpServletResponse} or
 * {@link org.eclipse.equinox.servletbridge.HttpServletResponseAdapter}. If
 * wappered object is {#code HttpServletResponseAdapter}, the response will be
 * buffered util the {@link #flushBufferStatus()} is invoked. Otherwise, this
 * class is a delegate class of {@code javax.serlvet.HttpServletResponse}.
 * 
 * @author Leo Chang
 */
public class HttpServletResponseWrapper implements HttpServletResponse,
		IServletObjectWrapper {
	public static final int RESULT_CANCEL = 5000;

	HttpServletResponseAdapter wrappedResponseAdapter;
	private int state = HttpServletResponse.SC_OK;
	private String errMsg;

	private HttpServletResponse wrappedResponse = null;

	protected HttpServletResponseWrapper topWrapper;
	private String redirectLocation;
	private ServletOutputStream outputStreamWrapper;
	private boolean isInclude;
	private PrintWriter printWriter;
	private boolean wrapperCommited;
	private Boolean isWriterObtained;

	private static Set<HttpServletResponseWrapper> wrappers = new HashSet<HttpServletResponseWrapper>();

	public static synchronized HttpServletResponseWrapper getHttpServletResponseWrapper(
			Object res) {
		if (res == null || res instanceof HttpServletResponseWrapper)
			return (HttpServletResponseWrapper) res;

		/* synchronized (wrappers) */{
			for (HttpServletResponseWrapper w : wrappers) {
				if (w.getOriginalObject().equals(res)) {
					return w;
				}
			}

			HttpServletResponseWrapper newWrapper = null;

			if (res instanceof HttpServletResponseAdapter) {
				newWrapper = new HttpServletResponseWrapper(
						(HttpServletResponseAdapter) res);
				newWrapper.topWrapper = null;
			}

			if (res instanceof HttpServletResponse) {
				newWrapper = new HttpServletResponseWrapper(
						(HttpServletResponse) res);
				HttpServletResponseWrapper topWrapper = (HttpServletResponseWrapper) FwkRuntime
						.getInstance().getWebContainer().getReqThreadVariants()
						.get(OSGiWebContainer.THREAD_V_RESPONSE);
				newWrapper.topWrapper = topWrapper;

				// FIXME: The initiallized state value may not synchronized with
				// the wrappered
				// response if the wrappered response suppressed the method
				// inheriting and
				// delivering state to top Thread Local Req. Err Msg has the
				// same problem.
				newWrapper.state = topWrapper.getState();
				newWrapper.errMsg = topWrapper.getErrorMessage();
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
	 */
	public static synchronized void releaseResponseWrapper(
			HttpServletResponse response) {
		wrappers.remove(response);
	}

	protected HttpServletResponseWrapper(HttpServletResponseAdapter res) {
		this.wrappedResponseAdapter = res;
	}

	/**
	 * @param res
	 */
	protected HttpServletResponseWrapper(HttpServletResponse res) {
		this.wrappedResponse = res;
	}

	public String getErrorMessage() {
		return errMsg;
	}

	public void addCookie(Cookie cookie) {
		if (wrappedResponse != null)
			wrappedResponse.addCookie(cookie);
		else {
			wrappedResponseAdapter.addCookie(new CookieWrapper(cookie));
		}
	}

	public void addDateHeader(String name, long date) {
		if (wrappedResponse != null)
			wrappedResponse.addDateHeader(name, date);
		else
			wrappedResponseAdapter.addDateHeader(name, date);
	}

	public void addHeader(String name, String value) {
		if (wrappedResponse != null)
			wrappedResponse.addHeader(name, value);
		else
			wrappedResponseAdapter.addHeader(name, value);
	}

	public void addIntHeader(String name, int value) {
		if (wrappedResponse != null)
			wrappedResponse.addIntHeader(name, value);
		else
			wrappedResponseAdapter.addIntHeader(name, value);
	}

	public boolean containsHeader(String name) {
		if (wrappedResponse != null)
			return wrappedResponse.containsHeader(name);
		else
			return wrappedResponseAdapter.containsHeader(name);
	}

	public String encodeRedirectUrl(String url) {
		if (wrappedResponse != null)
			return wrappedResponse.encodeRedirectUrl(url);
		else
			return wrappedResponseAdapter.encodeRedirectUrl(url);
	}

	public String encodeRedirectURL(String url) {
		if (wrappedResponse != null)
			return wrappedResponse.encodeRedirectURL(url);
		else
			return wrappedResponseAdapter.encodeRedirectURL(url);
	}

	public String encodeUrl(String url) {
		if (wrappedResponse != null)
			return wrappedResponse.encodeUrl(url);
		else
			return wrappedResponseAdapter.encodeUrl(url);
	}

	public String encodeURL(String url) {
		if (wrappedResponse != null)
			return wrappedResponse.encodeURL(url);
		else
			return wrappedResponseAdapter.encodeURL(url);
	}

	public void flushBuffer() throws IOException {
		// According to Servlet Spec 2.5, here we no need to check if the
		// Response committed. We do nothing in this case.
		/*if (isCommitted())
			throw new IllegalStateException(
					"This servlet response has been commited.");*/

		flushBufferInternal();
	}

	public void flushBufferInternal() throws IOException {
		// If this response is dispatched in include mode or the external
		// Response has commited, do nothing.
		if (isInclude || (isCommitted() && !isCommittedInternal()))
			return;

		if (wrappedResponse != null) {
			wrappedResponse.flushBuffer();
		} else {
			// because we wrapped the parent Servlet writer with buffered
			// PrintWriter, we need to flush content.
			this.flushBufferStatus();
			if (isWriterObtained != null && isWriterObtained)
				this.getWriter().flush();
			else
				this.getOutputStream().flush();
			// this.getOutputStream().flush();
			// wrappedResponseAdapter.flushBuffer();
		}
		setCommittedInternal(true);
	}

	protected void flushBufferStatus() throws IOException {
		if (wrappedResponse != null) {
			throw new IOException(
					"This method not allowed. Use flushBuffer() method for instead.");
		}

		// If this request is dispatched and in include mode, not do any status
		// output. Let the first request process phase to handle.
		if (state == SC_MOVED_TEMPORARILY) {
			// flush redirect status
			wrappedResponseAdapter.sendRedirect(redirectLocation);
		} else if (state != SC_OK) {
			if (errMsg != null)
				wrappedResponseAdapter.sendError(state, errMsg);
			else {
				wrappedResponseAdapter.setStatus(state);
			}
		} else {
			wrappedResponseAdapter.setStatus(SC_OK);
			// Do not flush content, because we do it at the service's end
			// by call flushBuffer() method later.
			// wrappedResponseAdapter.flushBuffer();
		}
	}

	public int getBufferSize() {
		if (wrappedResponse != null)
			return wrappedResponse.getBufferSize();
		else
			return wrappedResponseAdapter.getBufferSize();
	}

	public String getCharacterEncoding() {
		if (wrappedResponse != null)
			return wrappedResponse.getCharacterEncoding();
		else
			return wrappedResponseAdapter.getCharacterEncoding();
	}

	public String getContentType() {
		if (wrappedResponse != null)
			return wrappedResponse.getContentType();
		else
			return wrappedResponseAdapter.getContentType();
	}

	public Locale getLocale() {
		if (wrappedResponse != null)
			return wrappedResponse.getLocale();
		else
			return wrappedResponseAdapter.getLocale();
	}

	protected ServletOutputStream getOutputStreamInternal() throws IOException {
		if (wrappedResponse != null)
			return wrappedResponse.getOutputStream();
		else {
			if (outputStreamWrapper == null)
				outputStreamWrapper = new ServletOutputStreamWrapper(
						wrappedResponseAdapter.getOutputStream(), this);
			return outputStreamWrapper;
		}
	}

	public ServletOutputStream getOutputStream() throws IOException {
		// Either getWriter() or getOutputStream() may be called to write
		// the body, not both.
		if (isWriterObtained != null && isWriterObtained)
			throw new IllegalStateException(
					"Either getWriter() or getOutputStream() can be invoked.");
		else
			isWriterObtained = false;

		return getOutputStreamInternal();
	}

	public PrintWriter getWriter() throws IOException {
		// Either getWriter() or getOutputStream() may be called to write
		// the body, not both.
		if (isWriterObtained != null && !isWriterObtained)
			throw new IllegalStateException(
					"Either getWriter() or getOutputStream() can be invoked.");
		else
			isWriterObtained = true;

		if (wrappedResponse != null)
			return wrappedResponse.getWriter();
		else {
			if (printWriter == null) {
				// Use wrapped Response OutputStream to construct a no
				// auto-flush writer. We expect the writer to flush by user's
				// invoke or at last by framework.
				printWriter = new PrintWriter(new OutputStreamWriter(
						getOutputStreamInternal(), getCharacterEncoding()),
						false);
			}
			return printWriter;
		}
	}

	protected void setCommittedInternal(boolean committed) {
		wrapperCommited = committed;
	}

	public boolean isCommittedInternal() {
		return wrapperCommited;
	}

	public boolean isCommitted() {
		if (isCommittedInternal())
			return true;
		if (wrappedResponse != null)
			return wrappedResponse.isCommitted();
		else
			return wrappedResponseAdapter.isCommitted();
	}

	public void resetStatus() {
		setStatus(HttpServletResponse.SC_OK);
		errMsg = null;
	}

	public void reset() {
		if (wrappedResponse != null)
			wrappedResponse.reset();
		if (wrappedResponseAdapter != null)
			wrappedResponseAdapter.reset();

		setStatus(HttpServletResponse.SC_OK);
		errMsg = null;
	}

	public void resetBuffer() {
		if (wrappedResponse != null)
			wrappedResponse.resetBuffer();
		else
			wrappedResponseAdapter.resetBuffer();
	}

	protected boolean isInclude() {
		// Do not allow get include state from response, this state should be
		// obtained from request wrapper.
		if (topWrapper != null)
			return topWrapper.isInclude();
		return isInclude;
	}

	public void setInclude(boolean isInclude) {
		if (topWrapper != null)
			topWrapper.setInclude(isInclude);
		this.isInclude = isInclude;
	}

	public void sendError(int sc) throws IOException {
		// We mark the not null (Even empty) message string as a error status.
		// See flush status method.
		sendError(sc, "");
	}

	public void sendError(int sc, String msg) throws IOException {
		if (isCommitted())
			throw new IllegalStateException(
					"The servlet response has been committed.");

		if (wrappedResponse != null)
			wrappedResponse.sendError(sc, msg);
		// If the status is RESULT_CANCEL, we regarded it a internal mark, not a
		// error status.
		if (RESULT_CANCEL == sc) {
			setStatus(sc);
		} else {
			setStatus(sc, msg);
			setCommittedInternal(true);
		}
	}

	public void sendRedirect(String location) throws IOException {
		if (isCommitted())
			throw new IllegalStateException(
					"This servlet response has been commited.");
		if (wrappedResponse != null)
			wrappedResponse.sendRedirect(location);
		else {
			// Buffer the redirect operation and flush later.
			redirectLocation = location;
			// wrappedResponseAdapter.sendRedirect(location);
			// Set the status as SC_MOVED_TEMPORARILY 302, not
			// SC_MOVED_PERMANENTLY 301
			setStatus(SC_MOVED_TEMPORARILY);
		}
		setCommittedInternal(true);
	}

	public void setBufferSize(int size) {
		if (wrappedResponse != null)
			wrappedResponse.setBufferSize(size);
		else
			wrappedResponseAdapter.setBufferSize(size);
	}

	public void setCharacterEncoding(String charset) {
		if (wrappedResponse != null)
			wrappedResponse.setCharacterEncoding(charset);
		else
			wrappedResponseAdapter.setCharacterEncoding(charset);
	}

	public void setContentLength(int len) {
		if (wrappedResponse != null)
			wrappedResponse.setContentLength(len);
		else
			wrappedResponseAdapter.setContentLength(len);
	}

	public void setContentType(String type) {
		if (wrappedResponse != null)
			wrappedResponse.setContentType(type);
		else
			wrappedResponseAdapter.setContentType(type);
	}

	public void setDateHeader(String name, long date) {
		if (wrappedResponse != null)
			wrappedResponse.setDateHeader(name, date);
		else
			wrappedResponseAdapter.setDateHeader(name, date);
	}

	public void setHeader(String name, String value) {
		if (wrappedResponse != null)
			wrappedResponse.setHeader(name, value);
		else
			wrappedResponseAdapter.setHeader(name, value);
	}

	public void setIntHeader(String name, int value) {
		if (wrappedResponse != null)
			wrappedResponse.setIntHeader(name, value);
		else
			wrappedResponseAdapter.setIntHeader(name, value);
	}

	public void setLocale(Locale loc) {
		if (wrappedResponse != null)
			wrappedResponse.setLocale(loc);
		else
			wrappedResponseAdapter.setLocale(loc);
	}

	public void setStatus(int sc, String sm) {
		if (wrappedResponse != null)
			wrappedResponse.setStatus(sc, sm);
		errMsg = sm;
		setStatus(sc);
	}

	public void setStatus(int sc) {
		// According to Servlet spec 2.5, here we not need to check if response
		// committed.
		if (wrappedResponse != null)
			wrappedResponse.setStatus(sc);
		state = sc;
		if (sc == HttpServletResponse.SC_OK)
			this.errMsg = null;
	}

	public int getState() {
		return state;
	}

	public Object getOriginalObject() {
		if (wrappedResponse != null)
			return wrappedResponse;
		else
			return wrappedResponseAdapter;
	}

	public String getRedirectLocation() {
		return redirectLocation;
	}

	public HttpServletResponseWrapper getTopWrapper() {
		return topWrapper;
	}

}
