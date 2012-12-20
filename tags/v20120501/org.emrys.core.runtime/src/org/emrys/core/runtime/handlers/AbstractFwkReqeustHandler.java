/**
 * 
 */
package org.emrys.core.runtime.handlers;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.emrys.core.runtime.jeecontainer.IOSGiWebContainer;
import org.emrys.core.runtime.jeecontainer.OSGiJEEContainerHelper;


/**
 * @author LeoChang
 * 
 */
public abstract class AbstractFwkReqeustHandler implements IFwkRequestHandler {

	protected IOSGiWebContainer fwkContainer;
	protected OSGiJEEContainerHelper fwkContainerHelper;

	public AbstractFwkReqeustHandler(IOSGiWebContainer fwkContainer) {
		this.fwkContainer = fwkContainer;
		this.fwkContainerHelper = fwkContainer.getHelper();
	}

	protected IOSGiWebContainer getFwkContainer() {
		return fwkContainer;
	}

	protected OSGiJEEContainerHelper getFwkContainerHelper() {
		return fwkContainerHelper;
	}

	public void init() throws ServletException {
	}

	public int getPriority() {
		return 0;
	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

	}

	public void destroy() {

	}
}
