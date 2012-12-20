/*******************************************************************************
 * Copyright (c) 2011 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.jeeres;

import java.util.EventListener;

import javax.servlet.ServletException;

/**
 * 
 * @author Leo Chang - EMRYS
 * @version 2011-3-22
 */
public class ListenerInfo extends AbstractBundledServletObject {
	private EventListener listener;
	public String className;

	public EventListener getListener() throws ServletException {
		if (listener == null) {
			try {
				Class clazz = getBundleContext().getBundle().loadClass(className);
				listener = (EventListener) clazz.newInstance();
			} catch (Exception e) {
				e.printStackTrace();
				throw new ServletException("Init servlet context listener: " + className
						+ "from bundle: " + getBundleContext().getBundle().getBundleId()
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
