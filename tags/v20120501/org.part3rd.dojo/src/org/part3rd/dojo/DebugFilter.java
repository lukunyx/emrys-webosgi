package org.part3rd.dojo;

import java.io.IOException;
import java.net.URL;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class DebugFilter implements Filter {
	public void init(FilterConfig arg0) throws ServletException {

	}

	public void doFilter(ServletRequest req, ServletResponse resp,
			FilterChain chain) throws IOException, ServletException {
		if (req instanceof HttpServletRequest && isInDebugMode()) {
			// Note: some dojo js has no compressed file. Here check
			// whether the compressed file exists.
			HttpServletRequest httpReq = (HttpServletRequest) req;
			final String compressedFilePath = httpReq.getServletPath()
					+ ".uncompressed.js";
			URL url = httpReq.getSession().getServletContext().getResource(
					compressedFilePath);
			if (url != null) {
				req = new HttpServletRequestWrapper(httpReq) {
					@Override
					public String getServletPath() {
						return compressedFilePath;
					}
				};
			}
		}
		chain.doFilter(req, resp);
	}

	public void destroy() {

	}

	protected boolean isInDebugMode() {
		return Activator.Instance.isDebugging();
	}
}
