/*******************************************************************************
 * Copyright (c) 2011 Hirisun Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Hirisun License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.logger;

import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;

/**
 * 
 * @author Leo Chang - Hirisun
 * @version 2011-6-27
 */
public class WebConsoleLogListener implements LogListener {
	public void logged(LogEntry log) {
		/*
		 * if (log.getMessage() != null)
		 * System.out.println(LogUtil.genLogLine(log));
		 */
	}
}
