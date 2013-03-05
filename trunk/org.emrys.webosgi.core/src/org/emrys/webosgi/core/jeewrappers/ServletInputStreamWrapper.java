package org.emrys.webosgi.core.jeewrappers;

import java.io.IOException;

import javax.servlet.ServletInputStream;

import org.emrys.webosgi.launcher.internal.adapter.IServletObjectWrapper;
import org.emrys.webosgi.launcher.internal.adapter.ServletInputStreamAdapter;

/**
 * 
 * @author Leo Chang
 * @version 2011-4-19
 */
public class ServletInputStreamWrapper extends ServletInputStream implements
		IServletObjectWrapper {
	private final ServletInputStreamAdapter inputAdatpter;

	public ServletInputStreamWrapper(ServletInputStreamAdapter inputAdatpter) {
		this.inputAdatpter = inputAdatpter;
	}

	@Override
	public int available() throws IOException {
		return inputAdatpter.available();
	}

	@Override
	public void close() throws IOException {
		inputAdatpter.close();
	}

	@Override
	public boolean equals(Object obj) {
		return inputAdatpter.equals(obj);
	}

	@Override
	public int hashCode() {
		return inputAdatpter.hashCode();
	}

	@Override
	public void mark(int readlimit) {
		inputAdatpter.mark(readlimit);
	}

	@Override
	public boolean markSupported() {
		return inputAdatpter.markSupported();
	}

	@Override
	public int read() throws IOException {
		return inputAdatpter.read();
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return inputAdatpter.read(b, off, len);
	}

	@Override
	public int read(byte[] b) throws IOException {
		return inputAdatpter.read(b);
	}

	@Override
	public int readLine(byte[] b, int off, int len) throws IOException {
		return inputAdatpter.readLine(b, off, len);
	}

	@Override
	public void reset() throws IOException {
		inputAdatpter.reset();
	}

	@Override
	public long skip(long n) throws IOException {
		return inputAdatpter.skip(n);
	}

	@Override
	public String toString() {
		return inputAdatpter.toString();
	}

	public Object getOriginalObject() {
		return inputAdatpter;
	}
}
