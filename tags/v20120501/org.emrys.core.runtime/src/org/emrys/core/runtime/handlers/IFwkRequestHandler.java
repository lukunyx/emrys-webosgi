/**
 * 
 */
package org.emrys.core.runtime.handlers;

import java.io.IOException;

import javax.servlet.ServletException;

import org.emrys.core.runtime.jeewrappers.BundledHttpServletRequestWrapper;
import org.emrys.core.runtime.jeewrappers.HttpServletResponseWrapper;


/**
 * @author LeoChang
 * 
 */
public interface IFwkRequestHandler {

	public abstract void init() throws ServletException;

	public abstract void handle(BundledHttpServletRequestWrapper request,
			HttpServletResponseWrapper response, IFwkHandlerChain handlerChain)
			throws IOException, ServletException;

	public abstract void destroy();

	int getPriority();
}
