/*******************************************************************************
 * Copyright (c) 2010 Hirisun Corporation. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Hirisun License v1.0
 * which accompanies this distribution, and is available at
 * http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

/**
 * Service Init Exception. Juse extends from CoreException, no any additions.
 * 
 * @author Leo Chang - Hirisun
 * @version 2010-11-5
 */
public class ServiceInitException extends CoreException {
	public ServiceInitException(IStatus status) {
		super(status);
	}
}
