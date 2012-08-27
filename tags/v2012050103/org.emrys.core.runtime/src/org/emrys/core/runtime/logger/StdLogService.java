/*******************************************************************************
 * Copyright (c) 2011 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.logger;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import org.emrys.core.runtime.FwkActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;


/**
 * The Web log service provide the standard log interface to persist and read
 * out logs for specified times.
 * 
 * @author Leo Chang - EMRYS
 * @version 2011-6-27
 */
public class StdLogService implements LogService, LogReaderService {
	Set<LogListener> logListeners = new HashSet<LogListener>();
	BundleContext bundleContext = FwkActivator.getInstance().getBundle()
			.getBundleContext();

	private final static StdLogService stdLogService = new StdLogService();
	private final static PersistLogListener persistLogListener = new PersistLogListener();
	private final static LinkedList<LogReaderService> logReaders = new LinkedList<LogReaderService>();

	protected StdLogService() {
		// hide the constructor.
	}

	/**
	 * @return the singleton instance.
	 */
	public static StdLogService getInstance() {
		return stdLogService;
	}

	/**
	 * To listening the Log service
	 */
	private final static ServiceListener servlistener = new ServiceListener() {
		public void serviceChanged(ServiceEvent event) {
			BundleContext bc = event.getServiceReference().getBundle()
					.getBundleContext();
			LogReaderService lrs = (LogReaderService) bc.getService(event
					.getServiceReference());
			if (lrs != null) {
				if (event.getType() == ServiceEvent.REGISTERED) {
					logReaders.add(lrs);
					lrs.addLogListener(persistLogListener);
				} else if (event.getType() == ServiceEvent.UNREGISTERING) {
					lrs.removeLogListener(persistLogListener);
					logReaders.remove(lrs);
				}
			}
		}
	};

	/**
	 * Start the log service.
	 */
	public static void start() {
		BundleContext context = FwkActivator.getInstance().getBundle()
				.getBundleContext();

		// register this class's instance as a LogServie and LogReaderService.
		context
				.registerService(LogService.class.getName(), stdLogService,
						null);
		context.registerService(LogReaderService.class.getName(),
				stdLogService, null);

		// Get a list of all the registered LogReaderService, and add the
		// console listener
		ServiceTracker logReaderTracker = new ServiceTracker(context,
				org.osgi.service.log.LogReaderService.class.getName(), null);
		logReaderTracker.open();
		Object[] readers = logReaderTracker.getServices();
		if (readers != null) {
			for (int i = 0; i < readers.length; i++) {
				LogReaderService lrs = (LogReaderService) readers[i];
				logReaders.add(lrs);
				lrs.addLogListener(persistLogListener);
			}
		}

		logReaderTracker.close();

		// Add the ServiceListener, but with a filter so that we only receive
		// events related to
		// LogReaderService
		String filter = "(objectclass=" + LogReaderService.class.getName()
				+ ")";
		try {
			context.addServiceListener(servlistener, filter);
		} catch (InvalidSyntaxException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Stop the log service, this will remove all listeners.
	 */
	public static void stop() {
		for (Iterator<LogReaderService> i = logReaders.iterator(); i.hasNext();) {
			LogReaderService lrs = i.next();
			lrs.removeLogListener(persistLogListener);
			i.remove();
		}
	}

	public void log(int level, String message) {
		this.logged(new LogEntryImpl(level, 0, null, message, null, null));
	}

	public void log(int level, String message, Throwable exception) {
		this.logged(new LogEntryImpl(level, 0, null, message, exception, null));
	}

	public void log(ServiceReference sr, int level, String message) {
		this.logged(new LogEntryImpl(level, 0, null, message, null, sr));
	}

	public void log(ServiceReference sr, int level, String message,
			Throwable exception) {
		this.logged(new LogEntryImpl(level, 0, null, message, exception, sr));
	}

	public void log(Bundle bundle, ServiceReference sr, int level, int code,
			String message, Throwable exception) {
		this.logged(new LogEntryImpl(level, code, bundle.getSymbolicName(),
				message, exception, sr));
	}

	public void addLogListener(LogListener listener) {
		logListeners.add(listener);
	}

	public Enumeration<LogEntry> getLog() {
		return persistLogListener.getLogEnum();
	}

	public void removeLogListener(LogListener listener) {
		logListeners.remove(listener);
	}

	/**
	 * Log a log entry.
	 * 
	 * @param entry
	 */
	private void logged(LogEntry entry) {
		for (Iterator<LogListener> it = logListeners.iterator(); it.hasNext();) {
			LogListener listener = it.next();
			try {
				listener.logged(entry);
			} catch (Exception e) {
				// e.printStackTrace();
			}
		}
	}
}
