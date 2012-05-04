/*******************************************************************************
 * Copyright (c) 2011 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.jeewrappers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletOutputStream;

import org.emrys.core.adapter.internal.IServletObjectWrapper;
import org.emrys.core.adapter.internal.ServletOutputStreamAdapter;

/**
 * ServletOutputStream Wrapper class not buffering the data, but directly write
 * inot the original output stream to optimize performance. Before the firest
 * write operation, we need to flush the buffered response state. After the
 * state flush operation, all modification of Response Status will be invalid
 * according to the Java EE standard.
 * 
 * @author Leo Chang - EMRYS
 * @version 2011-4-19
 */
public class ServletOutputStreamWrapper extends ServletOutputStream implements
		IServletObjectWrapper {
	private static List<ServletOutputStreamWrapper> wrappers = new ArrayList<ServletOutputStreamWrapper>();
	private final ServletOutputStreamAdapter wrappedServletOutputStream;
	private final HttpServletResponseWrapper response;

	public ServletOutputStreamWrapper(ServletOutputStreamAdapter out,
			HttpServletResponseWrapper responseWrapper) {
		this.wrappedServletOutputStream = out;
		this.response = responseWrapper;
	}

	private boolean isResponseStatusBufferCommitted;

	public boolean isDataCommitted() {
		return isResponseStatusBufferCommitted;
	}

	@Override
	public void write(int b) throws IOException {
		// Write data into original Servlet Response OutputStream will cause the
		// commit stata change, so before this, we need to flush the buffered
		// state of our Response Wrapper.
		if (!isResponseStatusBufferCommitted) {
			isResponseStatusBufferCommitted = true;
			response.flushBufferStatus();
		}
		wrappedServletOutputStream.write(b);
	}

	@Override
	public void close() throws IOException {
		wrappedServletOutputStream.close();
	}

	@Override
	public boolean equals(Object obj) {
		return wrappedServletOutputStream.equals(obj);
	}

	@Override
	public void flush() throws IOException {
		wrappedServletOutputStream.flush();
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
	public void write(byte[] b, int off, int len) throws IOException {
		if (!isResponseStatusBufferCommitted) {
			isResponseStatusBufferCommitted = true;
			response.flushBufferStatus();
		}
		wrappedServletOutputStream.write(b, off, len);
	}

	@Override
	public void write(byte[] b) throws IOException {
		if (!isResponseStatusBufferCommitted) {
			isResponseStatusBufferCommitted = true;
			response.flushBufferStatus();
		}
		wrappedServletOutputStream.write(b);
	}
}
