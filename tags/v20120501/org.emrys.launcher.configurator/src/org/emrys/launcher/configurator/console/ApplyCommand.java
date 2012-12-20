/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.emrys.launcher.configurator.console;

import java.io.IOException;
import java.net.URL;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.emrys.launcher.configurator.Configurator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;


/**
 * An OSGi console command to apply a configuration
 */
public class ApplyCommand {

	private URL configURL;
	private CommandInterpreter interpreter;
	private BundleContext context;

	public ApplyCommand(CommandInterpreter interpreter, BundleContext context, URL configURL) {
		this.interpreter = interpreter;
		this.context = context;
		this.configURL = configURL;
	}

	/**
	 * Runs the apply console command
	 */
	public void run() {
		ServiceTracker tracker = new ServiceTracker(context, Configurator.class.getName(), null);
		tracker.open();
		Configurator configurator = (Configurator) tracker.getService();
		if (configurator != null) {
			try {
				if (configURL != null)
					configurator.applyConfiguration(configURL);
				else
					configurator.applyConfiguration();
				
				if (configurator.getUrlInUse() == null)
					interpreter.println("Config URL not set.");
			} catch (IOException e) {
				interpreter.println(e.getMessage());
			}
		} else {
			interpreter.println("No configurator registered"); //$NON-NLS-1$
		}
		tracker.close();
	}
}
