/*******************************************************************************
 * Copyright (c) 2011 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.adapter.internal;

import java.io.IOException;
import javax.servlet.ServletInputStream;

/**
 * 
 * @author Leo Chang - EMRYS
 * @version 2011-4-19
 */
public class ServletInputStreamAdapter {
	private ServletInputStream wrapperedServletInputStream;

	public ServletInputStreamAdapter(ServletInputStream servletInputStream) {
		this.wrapperedServletInputStream = servletInputStream;
	}

	public int available() throws IOException {
		return wrapperedServletInputStream.available();
	}

	public void close() throws IOException {
		wrapperedServletInputStream.close();
	}

	public boolean equals(Object obj) {
		return wrapperedServletInputStream.equals(obj);
	}

	public int hashCode() {
		return wrapperedServletInputStream.hashCode();
	}

	public void mark(int readlimit) {
		wrapperedServletInputStream.mark(readlimit);
	}

	public boolean markSupported() {
		return wrapperedServletInputStream.markSupported();
	}

	public int read() throws IOException {
		return wrapperedServletInputStream.read();
	}

	public int read(byte[] b, int off, int len) throws IOException {
		return wrapperedServletInputStream.read(b, off, len);
	}

	public int read(byte[] b) throws IOException {
		return wrapperedServletInputStream.read(b);
	}

	public int readLine(byte[] b, int off, int len) throws IOException {
		return wrapperedServletInputStream.readLine(b, off, len);
	}

	public void reset() throws IOException {
		wrapperedServletInputStream.reset();
	}

	public long skip(long n) throws IOException {
		return wrapperedServletInputStream.skip(n);
	}

	public String toString() {
		return wrapperedServletInputStream.toString();
	}
}
