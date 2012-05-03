/*******************************************************************************
 * Copyright (c) 2011 Hirisun Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Hirisun License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.internal;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.emrys.common.log.LogUtil;
import org.emrys.core.runtime.FwkActivator;
import org.emrys.core.runtime.logger.StdLogService;
import org.osgi.service.log.LogEntry;


/**
 * Command provider to debug command that switch the debug mode of framework.
 * 
 * @author Leo Chang - Hirisun
 * @version 2011-6-28
 */
public class FrameworkCoreCmdService implements CommandProvider {
	/**
	 * Default debug mode.
	 */
	boolean debug = true;

	/**
	 * "debug" command operation to switch debug mode.
	 * 
	 * @param interpreter
	 */
	public void _debug(CommandInterpreter interpreter) {
		debug = !debug;
		FwkActivator.getInstance().switchDebugOption(debug);
		interpreter.println((debug ? "Opened" : "Closed") + " the debug mode.");
	}

	/**
	 * "vlog" command operation to display history logs.
	 * 
	 * @param interpreter
	 */
	public void _vlog(CommandInterpreter interpreter) {
		String dateFormatStr = "yyyy/MM/dd";
		Calendar today = Calendar.getInstance();
		today.setTime(new Date());

		Date startTime = null;
		Date stopTime = null;
		int daysAgoCount = 0;
		String codeFilterStr = null;

		String arg1 = interpreter.nextArgument();
		String arg2 = null;
		if (arg1 != null)
			arg2 = interpreter.nextArgument();
		String arg3 = null;
		if (arg2 != null)
			arg3 = interpreter.nextArgument();

		if (arg1 != null)
			try {
				daysAgoCount = Integer.parseInt(arg1);
			} catch (NumberFormatException e) {
				// e.printStackTrace();
			}

		if (daysAgoCount > 0 && arg2 != null) {
			codeFilterStr = arg2;
		}

		if (daysAgoCount == 0 && arg1 != null) {
			try {
				startTime = new SimpleDateFormat(dateFormatStr).parse(arg1);
			} catch (ParseException e) {
				// e.printStackTrace();
			}

			if (startTime != null && arg2 != null) {
				try {
					stopTime = new SimpleDateFormat(dateFormatStr).parse(arg2);
				} catch (ParseException e) {
					// e.printStackTrace();
				}
				if (stopTime == null)
					codeFilterStr = arg2;
				else if (arg3 != null)
					codeFilterStr = arg3;
			}
		}

		if (startTime == null) {
			stopTime = today.getTime();
			today.add(Calendar.DAY_OF_YEAR, daysAgoCount == 0 ? -1
					: -daysAgoCount);
			startTime = today.getTime();
		}

		Enumeration<LogEntry> logEnum = StdLogService.getInstance().getLog();
		while (logEnum.hasMoreElements()) {
			LogEntry logEntry = logEnum.nextElement();
			long logTime = logEntry.getTime();
			if (logTime >= startTime.getTime() && logTime <= stopTime.getTime()) {
				interpreter.println(LogUtil.genLogLine(logEntry));
				if (codeFilterStr != null) {
					// FIXME: Do log code filter...
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.osgi.framework.console.CommandProvider#getHelp()
	 */
	public String getHelp() {
		StringBuffer cmdHelpStr = new StringBuffer();
		cmdHelpStr.append("---WebOSGi Framework Commands---");
		cmdHelpStr
				.append("\n\tdebug - switch the debug mode. Debug logs will be invisible is not debug mode opened.");
		cmdHelpStr
				.append("\n\tvlog - [[-days \\d] | [[-start yyyyMMddHHmm] [-end yyyyMMddHHmm]]] [-source expr] [-code expr] list logs between starttime and stoptime, or some days count. starttime[yyyy/MM/dd] stop time[yyyy/MM/dd] or daysAgoCount[int], codefilter[int|int...|int]");
		cmdHelpStr.append("\n");
		return cmdHelpStr.toString();
	}
}
