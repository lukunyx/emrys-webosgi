/**
 * 
 */
package org.emrys.webosgi.common.log;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.emrys.webosgi.common.IComActivator;
import org.osgi.service.log.LogEntry;


/**
 * @author LeoChang
 * 
 */
public class LogUtil {

	public static String genLogLine(IStatus status) {
		// This maybe a bug in OSGi log Service interface, it's not
		// convenient to set
		// the plugin bundle and code to LogEntry type. Here append
		// these information
		// before message.
		StringBuffer message = new StringBuffer();
		// getPlugin() indicating log source or a plugin's symble
		// name.
		int severity = status.getSeverity();
		switch (severity) {
		case IComActivator.LOG_DEBUG:
			message.append("DEBUG:");
			break;
		case IComActivator.LOG_ERROR:
			message.append("ERROR:");
			break;
		case IComActivator.LOG_INFO:
			message.append("INFO:");
			break;
		case IComActivator.LOG_WARNING:
			message.append("WARNING:");
			break;

		case IComActivator.LOG_OPERATION:
			message.append("OPERATION:");
			break;
		}

		if (status.getPlugin() != null)
			message.append("[" + status.getPlugin() + "]");
		else {
			message.append("[unknow] ");
		}
		message.append("(level " + severity + " code " + status.getCode()
				+ ") ");
		message.append(status.getMessage());
		return message.toString();
	}

	public static String genLogLine(LogEntry log) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(log.getTime());
		StringBuffer sb = new StringBuffer();
		if (log.getMessage() != null) {
			sb.append(log.getMessage());
		}
		if (log.getException() != null) {
			try {
				sb.append(System.getProperty("tmpLine.separator"));
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				PrintWriter writer = new PrintWriter(bout);
				log.getException().printStackTrace(writer);
				writer.flush();
				sb.append(new String(bout.toByteArray(), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				// e.printStackTrace();
			}
		}

		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(calendar
				.getTime())
				+ " " + sb.toString();
	}

	public static LogEntryImpl parseLogEntry(String line) {
		try {
			Pattern p = Pattern
					.compile("([\\d]{1,8}-[\\d]{1,2}-[\\d]{1,2} [\\d]{1,2}:[\\d]{1,2}:[\\d]{1,2})[^\\[]*\\[([^\\]]+)\\][ \\s]*\\(level[ \\s]*([\\d]+) code[ \\s]*([\\d]+)\\)([ \\S]+)([\\s\\S]*)");
			Matcher matcher = p.matcher(line);
			if (matcher.find()) {
				String timeStr = matcher.group(1);
				String source = matcher.group(2);
				String levelStr = matcher.group(3);
				String codeStr = matcher.group(4);
				String message = matcher.group(5);
				String exceptionStr = null;
				if (matcher.groupCount() == 6)
					exceptionStr = matcher.group(6);
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
						.parse(timeStr));
				int level = Integer.parseInt(levelStr);
				int code = Integer.parseInt(codeStr);

				String newMessage = genLogLine(new Status(level, source, code,
						message, null));
				return new LogEntryImpl(
						level,
						code,
						source,
						newMessage,
						(exceptionStr != null && exceptionStr.length() > 0 ? new Exception(
								exceptionStr)
								: null), null, calendar.getTimeInMillis());
			}
		} catch (Exception e) {
			// e.printStackTrace();
		}
		return null;
	}
}
