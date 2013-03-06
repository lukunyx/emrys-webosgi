package org.emrys.webosgi.launcher.internal.adapter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.http.HttpServletResponse;

/**
 * 为了将服务器的Servlet库版本与WebOSGi内部使用版本分离，这里对服务器传递过来的HttpServletResponse类型进行wrapper,
 * 实现类加载的隔离〄1�7
 * 
 * @author Leo Chang
 * @version 2011-4-18
 */
public class HttpServletResponseAdapter implements IServletObjectWrapper {
	private final HttpServletResponse wrapperedServletResponse;

	public HttpServletResponseAdapter(HttpServletResponse response) {
		wrapperedServletResponse = response;
	}

	public void addCookie(ICookie cookie) {
		wrapperedServletResponse.addCookie(new CookieWrapper(cookie));
	}

	public void addDateHeader(String name, long date) {
		wrapperedServletResponse.addDateHeader(name, date);
	}

	public void addHeader(String name, String value) {
		wrapperedServletResponse.addHeader(name, value);
	}

	public void addIntHeader(String name, int value) {
		wrapperedServletResponse.addIntHeader(name, value);
	}

	public boolean containsHeader(String name) {
		return wrapperedServletResponse.containsHeader(name);
	}

	public String encodeRedirectUrl(String url) {
		return wrapperedServletResponse.encodeRedirectUrl(url);
	}

	public String encodeRedirectURL(String url) {
		return wrapperedServletResponse.encodeRedirectURL(url);
	}

	public String encodeUrl(String url) {
		return wrapperedServletResponse.encodeUrl(url);
	}

	public String encodeURL(String url) {
		return wrapperedServletResponse.encodeURL(url);
	}

	public void flushBuffer() throws IOException {
		wrapperedServletResponse.flushBuffer();
	}

	public int getBufferSize() {
		return wrapperedServletResponse.getBufferSize();
	}

	public String getCharacterEncoding() {
		return wrapperedServletResponse.getCharacterEncoding();
	}

	public String getContentType() {
		return wrapperedServletResponse.getContentType();
	}

	public Locale getLocale() {
		return wrapperedServletResponse.getLocale();
	}

	/**
	 * 为了对BridgeServlet和OSGiJEEContainer的servlet规范版本隔离，这里将ServletOutputStream
	 * ServletOutputStream.getOutputStream()方法返回值改成我们的封装类�1�7�1�7
	 * 
	 * @throws IOException
	 */
	public ServletOutputStreamAdapter getOutputStream() throws IOException {
		return new ServletOutputStreamAdapter(wrapperedServletResponse
				.getOutputStream());
	}

	public PrintWriter getWriter() throws IOException {
		return wrapperedServletResponse.getWriter();
	}

	public boolean isCommitted() {
		return wrapperedServletResponse.isCommitted();
	}

	public void reset() {
		wrapperedServletResponse.reset();
	}

	public void resetBuffer() {
		wrapperedServletResponse.resetBuffer();
	}

	public void sendError(int sc, String msg) throws IOException {
		wrapperedServletResponse.sendError(sc, msg);
	}

	public void sendError(int sc) throws IOException {
		wrapperedServletResponse.sendError(sc);
	}

	public void sendRedirect(String location) throws IOException {
		wrapperedServletResponse.sendRedirect(location);
	}

	public void setBufferSize(int size) {
		wrapperedServletResponse.setBufferSize(size);
	}

	public void setCharacterEncoding(String charset) {
		wrapperedServletResponse.setCharacterEncoding(charset);
	}

	public void setContentLength(int len) {
		wrapperedServletResponse.setContentLength(len);
	}

	public void setContentType(String type) {
		wrapperedServletResponse.setContentType(type);
	}

	public void setDateHeader(String name, long date) {
		wrapperedServletResponse.setDateHeader(name, date);
	}

	public void setHeader(String name, String value) {
		wrapperedServletResponse.setHeader(name, value);
	}

	public void setIntHeader(String name, int value) {
		wrapperedServletResponse.setIntHeader(name, value);
	}

	public void setLocale(Locale loc) {
		wrapperedServletResponse.setLocale(loc);
	}

	public void setStatus(int sc, String sm) {
		wrapperedServletResponse.setStatus(sc, sm);
	}

	public void setStatus(int sc) {
		wrapperedServletResponse.setStatus(sc);
	}

	public Object getOriginalObject() {
		return wrapperedServletResponse;
	}
}
