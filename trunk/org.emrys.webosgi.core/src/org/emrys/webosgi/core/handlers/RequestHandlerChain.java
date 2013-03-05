/**
 * 
 */
package org.emrys.webosgi.core.handlers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;

import org.emrys.webosgi.core.internal.FwkRuntime;
import org.emrys.webosgi.core.jeewrappers.BundledHttpServletRequestWrapper;
import org.emrys.webosgi.core.jeewrappers.HttpServletResponseWrapper;
import org.emrys.webosgi.core.service.IOSGiWebContainer;


/**
 * @author LeoChang
 * 
 */
public class RequestHandlerChain implements IFwkHandlerChain {

	/**
	 * Thead local variant name: handler iterator of current request
	 */
	private static final String V_REQ_THREAD_HANDLDER = "req_handlers";
	private final List<IFwkRequestHandler> handlers = new ArrayList<IFwkRequestHandler>();
	private boolean needSort;
	private final IOSGiWebContainer fwkContainer;

	public RequestHandlerChain() {
		fwkContainer = FwkRuntime.getInstance().getWebContainer();
	}

	public void start(BundledHttpServletRequestWrapper request,
			HttpServletResponseWrapper response) throws IOException,
			ServletException {
		// Clear handlers variant, coz the reqeuest may be dispatched in
		// service side.
		fwkContainer.getReqThreadVariants().put(V_REQ_THREAD_HANDLDER, null);
		handle(request, response);
	}

	public void handle(BundledHttpServletRequestWrapper request,
			HttpServletResponseWrapper response) throws IOException,
			ServletException {
		if (needSort) {
			synchronized (this) {
				if (needSort) {
					sortHandlers();
					needSort = false;
				}
			}
		}

		Iterator<IFwkRequestHandler> it = (Iterator<IFwkRequestHandler>) fwkContainer
				.getReqThreadVariants().get(V_REQ_THREAD_HANDLDER);
		if (it == null) {
			it = handlers.iterator();
			fwkContainer.getReqThreadVariants().put(V_REQ_THREAD_HANDLDER, it);
		}

		if (it.hasNext()) {
			IFwkRequestHandler handler = it.next();
			handler.handle(request, response, this);
		}
	}

	private void sortHandlers() {
		Collections.sort(handlers, new Comparator<IFwkRequestHandler>() {
			public int compare(IFwkRequestHandler o1, IFwkRequestHandler o2) {
				return o1.getPriority() < o2.getPriority() ? -1 : 1;
			}
		});
	}

	public boolean addHandler(IFwkRequestHandler handler) {
		if (handler == null)
			throw new IllegalArgumentException("Handler to add cann't be null.");

		boolean result = false;
		if (!handlers.contains(handler)) {
			handlers.add(handler);
			needSort = true;
			result = true;
		}
		return result;
	}

	public void addHandlers(Collection<IFwkRequestHandler> handlers) {
		for (IFwkRequestHandler handler : handlers) {
			addHandler(handler);
		}
	}
}
