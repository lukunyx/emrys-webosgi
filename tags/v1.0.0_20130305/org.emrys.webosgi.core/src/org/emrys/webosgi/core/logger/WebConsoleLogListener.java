package org.emrys.webosgi.core.logger;

import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;

/**
 * 
 * @author Leo Chang
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
