package org.emrys.webosgi.launcher.internal.adapter;

import java.io.IOException;

import javax.servlet.ServletOutputStream;

/**
 * 
 * @author Leo Chang
 * @version 2011-4-18
 */
public class ServletOutputStreamAdapter implements IServletObjectWrapper {

	private final ServletOutputStream wrapperdServletOutputStream;

	public ServletOutputStreamAdapter(ServletOutputStream servletOutputStream) {
		wrapperdServletOutputStream = servletOutputStream;
	}

	public void close() throws IOException {
		wrapperdServletOutputStream.close();
	}

	@Override
	public boolean equals(Object obj) {
		return wrapperdServletOutputStream.equals(obj);
	}

	public void flush() throws IOException {
		wrapperdServletOutputStream.flush();
	}

	@Override
	public int hashCode() {
		return wrapperdServletOutputStream.hashCode();
	}

	public void print(boolean b) throws IOException {
		wrapperdServletOutputStream.print(b);
	}

	public void print(char c) throws IOException {
		wrapperdServletOutputStream.print(c);
	}

	public void print(double d) throws IOException {
		wrapperdServletOutputStream.print(d);
	}

	public void print(float f) throws IOException {
		wrapperdServletOutputStream.print(f);
	}

	public void print(int i) throws IOException {
		wrapperdServletOutputStream.print(i);
	}

	public void print(long l) throws IOException {
		wrapperdServletOutputStream.print(l);
	}

	public void print(String s) throws IOException {
		wrapperdServletOutputStream.print(s);
	}

	public void println() throws IOException {
		wrapperdServletOutputStream.println();
	}

	public void println(boolean b) throws IOException {
		wrapperdServletOutputStream.println(b);
	}

	public void println(char c) throws IOException {
		wrapperdServletOutputStream.println(c);
	}

	public void println(double d) throws IOException {
		wrapperdServletOutputStream.println(d);
	}

	public void println(float f) throws IOException {
		wrapperdServletOutputStream.println(f);
	}

	public void println(int i) throws IOException {
		wrapperdServletOutputStream.println(i);
	}

	public void println(long l) throws IOException {
		wrapperdServletOutputStream.println(l);
	}

	public void println(String s) throws IOException {
		wrapperdServletOutputStream.println(s);
	}

	@Override
	public String toString() {
		return wrapperdServletOutputStream.toString();
	}

	public void write(byte[] b, int off, int len) throws IOException {
		wrapperdServletOutputStream.write(b, off, len);
	}

	public void write(byte[] b) throws IOException {
		wrapperdServletOutputStream.write(b);
	}

	public void write(int b) throws IOException {
		wrapperdServletOutputStream.write(b);
	}

	public Object getOriginalObject() {
		return wrapperdServletOutputStream;
	}

}
