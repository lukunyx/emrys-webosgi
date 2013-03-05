/**
 * 
 */
package org.emrys.webosgi.core.handlers;

import java.io.IOException;

import javax.servlet.ServletException;

import org.emrys.webosgi.core.jeewrappers.BundledHttpServletRequestWrapper;
import org.emrys.webosgi.core.jeewrappers.HttpServletResponseWrapper;


/**
 * @author LeoChang
 * 
 */
public interface IFwkHandlerChain {
	public void start(BundledHttpServletRequestWrapper request,
			HttpServletResponseWrapper response) throws IOException,
			ServletException;

	public void handle(BundledHttpServletRequestWrapper request,
			HttpServletResponseWrapper response) throws IOException,
			ServletException;
}
