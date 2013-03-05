package org.emrys.webosgi.core.logger;

import java.util.Calendar;

import org.emrys.webosgi.common.util.BundleServiceUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;

/**
 * The web component's log entry.
 * 
 * @author Leo Chang
 * @version 2011-6-27
 */
public class LogEntryImpl implements LogEntry {
	int level;
	Bundle bundle;
	String source;
	String message;
	Throwable exception;
	long occurTime;
	private ServiceReference service;
	private int code;

	/**
	 * Constructor with some parameters.
	 * 
	 * @param level
	 *            log level
	 * @param code
	 *            log code
	 * @param sourceBundle
	 *            source bundle the log related
	 * @param message
	 *            message of log
	 * @param exception
	 *            exception, may be null
	 * @param service
	 *            the servie from which OSGi service, may by null
	 * @param occurTime
	 *            the time log generated
	 */
	public LogEntryImpl(int level, int code, String source, String message,
			Throwable exception, ServiceReference service) {
		init(level, code, source, message, exception, service, 0l);
	}

	/**
	 * Constructor with some parameters.
	 * 
	 * @param level
	 *            log level
	 * @param code
	 *            log code
	 * @param sourceBundle
	 *            source bundle the log related
	 * @param message
	 *            message of log
	 * @param exception
	 *            exception, may be null
	 * @param service
	 *            the servie from which OSGi service, may by null
	 * @param occurTime
	 *            the time log generated
	 */
	public LogEntryImpl(int level, int code, String source, String message,
			Throwable exception, ServiceReference service, long occurTime) {
		init(level, code, source, message, exception, service, occurTime);
	}

	/**
	 * init this instance with parameters.
	 * 
	 * @param level
	 *            log level
	 * @param code
	 *            log code
	 * @param sourceBundle
	 *            source bundle the log related
	 * @param message
	 *            message of log
	 * @param exception
	 *            exception, may be null
	 * @param service
	 *            the servie from which OSGi service, may by null
	 * @param occurTime
	 *            the time log generated
	 */
	private void init(int level, int code, String source, String message,
			Throwable exception, ServiceReference service, long occurTime) {
		this.level = level;
		this.source = source;
		this.message = message;
		this.exception = exception;
		this.service = service;
		this.code = code;
		if (occurTime == 0)
			this.occurTime = Calendar.getInstance().getTimeInMillis();
		else
			this.occurTime = occurTime;

		// try to parse bundle and extra source given source
		// [bundleSysmbolMame:extra source] extra source maybe none.
		if (source != null) {
			int index = source.indexOf(':');
			if (index < -1) {
				String bundleSysmbleName = source.substring(0, index);
				this.bundle = BundleServiceUtil
						.findBundleBySymbolName(bundleSysmbleName.trim());
			} else
				this.bundle = BundleServiceUtil.findBundleBySymbolName(source);
		}
	}

	public String getSource() {
		return source;
	}

	public Bundle getBundle() {
		return bundle;
	}

	public int getCode() {
		return code;
	}

	public Throwable getException() {
		return exception;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.log.LogEntry#getLevel()
	 */
	public int getLevel() {
		// TODO Auto-generated method stub
		return level;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.log.LogEntry#getMessage()
	 */
	public String getMessage() {
		// TODO Auto-generated method stub
		return message;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.log.LogEntry#getServiceReference()
	 */
	public ServiceReference getServiceReference() {
		return service;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.osgi.service.log.LogEntry#getTime()
	 */
	public long getTime() {
		return occurTime;
	}
}
