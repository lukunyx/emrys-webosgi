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
public interface IFwkHandlerChain {
	public void start(BundledHttpServletRequestWrapper request,
			HttpServletResponseWrapper response) throws IOException,
			ServletException;

	public void handle(BundledHttpServletRequestWrapper request,
			HttpServletResponseWrapper response) throws IOException,
			ServletException;
}
