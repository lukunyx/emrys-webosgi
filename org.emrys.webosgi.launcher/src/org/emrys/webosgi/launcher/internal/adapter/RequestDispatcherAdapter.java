package org.emrys.webosgi.launcher.internal.adapter;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * 
 * @author Leo Chang
 * @version 2011-4-19
 */
public class RequestDispatcherAdapter {
	private final RequestDispatcher wrapperedDispatcher;

	public RequestDispatcherAdapter(RequestDispatcher dispatcher) {
		this.wrapperedDispatcher = dispatcher;
	}

	public void forward(ServletRequest request, ServletResponse response)
			throws ServletException, IOException {
		wrapperedDispatcher.forward(request, response);
	}

	public void include(ServletRequest request, ServletResponse response)
			throws ServletException, IOException {
		wrapperedDispatcher.include(request, response);
	}
}
