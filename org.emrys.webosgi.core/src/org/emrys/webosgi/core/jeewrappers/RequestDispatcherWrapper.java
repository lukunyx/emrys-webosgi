package org.emrys.webosgi.core.jeewrappers;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.emrys.webosgi.launcher.internal.adapter.IServletObjectWrapper;
import org.emrys.webosgi.launcher.internal.adapter.RequestDispatcherAdapter;

/**
 * 
 * @author Leo Chang
 * @version 2011-4-19
 */
public class RequestDispatcherWrapper implements RequestDispatcher,
		IServletObjectWrapper {
	private final RequestDispatcherAdapter dispatcherAdapter;

	public RequestDispatcherWrapper(RequestDispatcherAdapter dispatcherAdapter) {
		this.dispatcherAdapter = dispatcherAdapter;
	}

	@Override
	public boolean equals(Object obj) {
		return dispatcherAdapter.equals(obj);
	}

	public void forward(ServletRequest request, ServletResponse response)
			throws ServletException, IOException {
		dispatcherAdapter.forward(request, response);
	}

	@Override
	public int hashCode() {
		return dispatcherAdapter.hashCode();
	}

	public void include(ServletRequest request, ServletResponse response)
			throws ServletException, IOException {
		dispatcherAdapter.include(request, response);
	}

	@Override
	public String toString() {
		return dispatcherAdapter.toString();
	}

	public Object getOriginalObject() {
		return dispatcherAdapter;
	}
}
