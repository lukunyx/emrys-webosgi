package org.emrys.webosgi.launcher.internal.adapter;

import java.io.IOException;

import javax.servlet.ServletInputStream;

/**
 * 
 * @author Leo Chang
 * @version 2011-4-19
 */
public class ServletInputStreamAdapter {
	private final ServletInputStream wrapperedServletInputStream;

	public ServletInputStreamAdapter(ServletInputStream servletInputStream) {
		this.wrapperedServletInputStream = servletInputStream;
	}

	public int available() throws IOException {
		return wrapperedServletInputStream.available();
	}

	public void close() throws IOException {
		wrapperedServletInputStream.close();
	}

	@Override
	public boolean equals(Object obj) {
		return wrapperedServletInputStream.equals(obj);
	}

	@Override
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

	@Override
	public String toString() {
		return wrapperedServletInputStream.toString();
	}
}
