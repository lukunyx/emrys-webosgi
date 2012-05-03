/*******************************************************************************
 * Copyright (c) 2011 Hirisun Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Hirisun License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.emrys.common.IComActivator;
import org.emrys.common.log.LogUtil;
import org.emrys.common.util.FileUtil;
import org.emrys.core.runtime.FwkActivator;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogService;


/**
 * 
 * @author Leo Chang - Hirisun
 * @version 2011-6-27
 */
public class OperationEncryptLogListener implements LogListener {
	private File currentLogFile;

	public void logged(LogEntry log) {
		// FIXME:Need asynchorinized process???
		if (IComActivator.LOG_OPERATION != log.getLevel())
			return;

		try {
			synchronized (this) {
				currentLogFile = getLogFile(0);
				FileUtil.createFile(currentLogFile, false);
				FileWriter fw = new FileWriter(currentLogFile, true);

				String logLine = LogUtil.genLogLine(log);
				fw.append(logLine);
				fw.append(System.getProperty("line.separator"));
				fw.close();

				// Only in debug mode, print all error and debug information.
				if ((FwkActivator.getInstance().isDebugging()
						&& (log.getLevel() == LogService.LOG_ERROR) || log
						.getLevel() == LogService.LOG_DEBUG)
						|| log.getLevel() == LogService.LOG_WARNING
						|| log.getLevel() == LogService.LOG_INFO) {
					if (log.getLevel() == LogService.LOG_ERROR)
						System.err.println(logLine);
					else
						System.out.println(logLine);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private File getLogFile(int daysAfter) throws IOException {
		Calendar time = Calendar.getInstance();
		time.setTime(new Date());
		time.add(Calendar.DAY_OF_YEAR, daysAfter);
		IPath logsFile = new Path(FwkActivator.getInstance()
				.getComponentWorkspaceRoot().getAbsolutePath()).append(".logs/"
				+ new SimpleDateFormat("yyyy").format(time.getTime()) + "/"
				+ new SimpleDateFormat("yyyyMMdd").format(time.getTime())
				+ "_oper.log");
		return logsFile.toFile();
	}

	/**
	 * @return
	 */
	public Enumeration<LogEntry> getLogEnum() {
		return new Enumeration<LogEntry>() {
			private int days = 0;
			private String currentLogEntry;
			private BufferedReader reader;
			private String tmpLine;

			public boolean hasMoreElements() {
				currentLogEntry = null;
				try {
					if (reader == null) {
						File logfile = OperationEncryptLogListener.this
								.getLogFile(days);
						if (!logfile.exists())
							return false;
						InputStreamReader fileReader = new InputStreamReader(
								new FileInputStream(logfile), "UTF-8");
						reader = new BufferedReader(fileReader);
						days--;
					}

					if (tmpLine != null
							|| (tmpLine = reader.readLine()) != null) {
						Pattern p = Pattern
								.compile("[\\d]{1,8}-[\\d]{1,2}-[\\d]{1,2} [\\d]{1,2}:[\\d]{1,2}:[\\d]{1,2}");
						Matcher matcher = p.matcher(tmpLine);
						if (matcher.find()) {
							currentLogEntry = tmpLine;
							while ((tmpLine = reader.readLine()) != null) {
								matcher = p.matcher(tmpLine);
								if (matcher.find())
									break;
								currentLogEntry += System
										.getProperty("line.separator")
										+ tmpLine;
								tmpLine = null;
							}
							return true;
						}
					} else {
						reader.close();
						reader = null;
						return hasMoreElements();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				return false;
			}

			public LogEntry nextElement() {
				LogEntry element = null;
				if (currentLogEntry != null) {
					element = LogUtil.parseLogEntry(currentLogEntry);
				}
				if (element != null) {
					return element;
				} else if (hasMoreElements())
					return nextElement();
				throw new java.util.NoSuchElementException();
			}
		};
	}
}
