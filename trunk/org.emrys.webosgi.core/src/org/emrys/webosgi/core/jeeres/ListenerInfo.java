package org.emrys.webosgi.core.jeeres;

import java.util.EventListener;

import javax.servlet.ServletException;

/**
 * 
 * @author Leo Chang
 * @version 2011-3-22
 */
public class ListenerInfo extends AbstractBundledServletObject {
	private EventListener listener;
	public String className;

	public EventListener getListener() throws ServletException {
		if (listener == null) {
			try {
				Class clazz = getBundleContext().getBundle().loadClass(
						className);
				listener = (EventListener) clazz.newInstance();
			} catch (Exception e) {
				e.printStackTrace();
				throw new ServletException("Init servlet context listener: "
						+ className + "from bundle: "
						+ getBundleContext().getBundle().getBundleId()
						+ " failed.", e);
			}
		}
		return listener;
	}

	@Override
	public boolean isInitialized() {
		return true;
	}
}
