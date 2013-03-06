package org.emrys.webosgi.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

/**
 * Service Init Exception. Juse extends from CoreException, no any additions.
 * 
 * @author Leo Chang
 * @version 2010-11-5
 */
public class ServiceInitException extends CoreException {
	public ServiceInitException(IStatus status) {
		super(status);
	}
}
