package org.emrys.webosgi.core.jeewrappers;

import java.io.IOException;

import javax.servlet.ServletOutputStream;

import org.emrys.webosgi.launcher.internal.adapter.IServletObjectWrapper;
import org.emrys.webosgi.launcher.internal.adapter.ServletOutputStreamAdapter;

/**
 * ServletOutputStream Wrapper class not buffering the data, but directly write
 * into the original output stream to optimize performance. Before the flush
 * operation, we need to flush the buffered response state. After the state
 * flush operation, all modification of Response Status will be invalid
 * according to the Java EE standard.
 * 
 * @author Leo Chang
 * @version 2011-4-19
 */
public class ServletOutputStreamWrapper extends ServletOutputStream implements
		IServletObjectWrapper {
	private final ServletOutputStreamAdapter wrappedServletOutputStream;
	private final HttpServletResponseWrapper response;
	private boolean responseStatusFlushed;

	public ServletOutputStreamWrapper(ServletOutputStreamAdapter out,
			HttpServletResponseWrapper responseWrapper) {
		this.wrappedServletOutputStream = out;
		this.response = responseWrapper;
	}

	@Override
	public void close() throws IOException {
		wrappedServletOutputStream.close();
	}

	@Override
	public boolean equals(Object obj) {
		return wrappedServletOutputStream.equals(obj);
	}

	public Object getOriginalObject() {
		return wrappedServletOutputStream;
	}

	@Override
	public int hashCode() {
		return wrappedServletOutputStream.hashCode();
	}

	@Override
	public void print(boolean b) throws IOException {
		wrappedServletOutputStream.print(b);
	}

	@Override
	public void print(char c) throws IOException {
		wrappedServletOutputStream.print(c);
	}

	@Override
	public void print(double d) throws IOException {
		wrappedServletOutputStream.print(d);
	}

	@Override
	public void print(float f) throws IOException {
		wrappedServletOutputStream.print(f);
	}

	@Override
	public void print(int i) throws IOException {
		wrappedServletOutputStream.print(i);
	}

	@Override
	public void print(long l) throws IOException {
		wrappedServletOutputStream.print(l);
	}

	@Override
	public void print(String s) throws IOException {
		wrappedServletOutputStream.print(s);
	}

	@Override
	public void println() throws IOException {
		wrappedServletOutputStream.println();
	}

	@Override
	public void println(boolean b) throws IOException {
		wrappedServletOutputStream.println(b);
	}

	@Override
	public void println(char c) throws IOException {
		wrappedServletOutputStream.println(c);
	}

	@Override
	public void println(double d) throws IOException {
		wrappedServletOutputStream.println(d);
	}

	@Override
	public void println(float f) throws IOException {
		wrappedServletOutputStream.println(f);
	}

	@Override
	public void println(int i) throws IOException {
		wrappedServletOutputStream.println(i);
	}

	@Override
	public void println(long l) throws IOException {
		wrappedServletOutputStream.println(l);
	}

	@Override
	public void println(String s) throws IOException {
		wrappedServletOutputStream.println(s);
	}

	@Override
	public String toString() {
		return wrappedServletOutputStream.toString();
	}

	@Override
	public void flush() throws IOException {
		flushResponseWrapperStatus();
		wrappedServletOutputStream.flush();
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		flushResponseWrapperStatus();
		wrappedServletOutputStream.write(b, off, len);
	}

	@Override
	public void write(byte[] b) throws IOException {
		flushResponseWrapperStatus();
		wrappedServletOutputStream.write(b);
	}

	@Override
	public void write(int b) throws IOException {
		flushResponseWrapperStatus();
		wrappedServletOutputStream.write(b);
	}

	/**
	 * Before write or flush External Response buffer, flush ours buffered
	 * status at first.
	 * 
	 * @throws IOException
	 */
	protected void flushResponseWrapperStatus() throws IOException {
		if (!responseStatusFlushed) {
			responseStatusFlushed = true;
			response.flushBufferStatus();
			// Set the response's wrapper as committed. and it
			// wont allow any invoke to the wrapper's some methods like
			// sendError() and sendRedirect();
			response.setCommittedInternal(true);
		}
	}
}
