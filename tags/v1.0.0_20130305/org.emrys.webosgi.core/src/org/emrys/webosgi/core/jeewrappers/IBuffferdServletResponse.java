package org.emrys.webosgi.core.jeewrappers;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

/**
 * The wrapper class to {@link javax.serlvet.HttpServletResponse} or
 * {@link org.eclipse.equinox.servletbridge.HttpServletResponseAdapter}.
 * 
 * @author Leo Chang
 * @version 2011-3-29
 */
public interface IBuffferdServletResponse extends HttpServletResponse {
	public static final int RESULT_CANCEL = 5000;

	int getState();

	/**
	 * Flush out buffered Response Status to original Servlet Response.
	 * 
	 * @throws IOException
	 */
	void flushBufferStatus() throws IOException;

	String getRedirectLocation();
}
