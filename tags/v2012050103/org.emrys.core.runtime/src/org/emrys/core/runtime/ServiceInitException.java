/*******************************************************************************
 * Copyright (c) 2010 EMRYS Corporation. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

/**
 * Service Init Exception. Juse extends from CoreException, no any additions.
 * 
 * @author Leo Chang - EMRYS
 * @version 2010-11-5
 */
public class ServiceInitException extends CoreException {
	public ServiceInitException(IStatus status) {
		super(status);
	}
}
