/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.emrys.launcher.configurator;

import java.util.Dictionary;
import java.util.Hashtable;

import org.emrys.launcher.configurator.utils.EquinoxUtils;
import org.emrys.launcher.configurator.utils.SimpleConfiguratorConstants;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;


/**
 * At its start, SimpleConfigurator bundle does the followings.
 * 
 * 1. A value will be gotten by @{link BundleContext#getProperty(key)} with
 * {@link SimpleConfiguratorConstants#PROP_KEY_CONFIGURL} as a key. The value will be used for the
 * referal URL. Under the url, there must be a simple bundles list file to be installed with thier
 * start level and flag of marked as started.
 * 
 * 2. If the value is null, do nothing any more. 3. Otherwise, retrieve the bundles list from the
 * url and install, set start level of and start bundles, as specified.
 * 
 * 4. A value will be gotten by @{link BundleContext#getProperty(key)} with
 * {@link SimpleConfiguratorConstants#PROP_KEY_EXCLUSIVE_INSTALLATION} as a key.
 * 
 * 5. If it equals "false", it will do exclusive installation, which means that the bundles will not
 * be listed in the specified url but installed at the time of the method call except SystemBundle
 * will be uninstalled. Otherwise, no uninstallation will not be done.
 * 
 */
public class Activator implements BundleActivator
{
	public final static boolean DEBUG = true;
	private ServiceRegistration configuratorRegistration;
	private ServiceRegistration commandRegistration;

	public void start(BundleContext context) throws Exception
	{
		SimpleConfiguratorImpl bundleConfigurator = new SimpleConfiguratorImpl(context, context
				.getBundle());
		bundleConfigurator.applyConfiguration();

		Dictionary props = new Hashtable();
		props.put(Constants.SERVICE_VENDOR, "Eclipse"); //$NON-NLS-1$
		props.put(Constants.SERVICE_PID, SimpleConfiguratorConstants.TARGET_CONFIGURATOR_NAME);
		ServiceFactory configurationFactory = new SimpleConfiguratorFactory(context);
		configuratorRegistration = context.registerService(Configurator.class.getName(),
				configurationFactory, props);

		try {
			if (null != context.getBundle().loadClass(
					"org.eclipse.osgi.framework.console.CommandProvider")) //$NON-NLS-1$
				commandRegistration = EquinoxUtils.registerConsoleCommands(context);
		} catch (ClassNotFoundException e) {
			// CommandProvider is not available
			// Ok -- optional
		}

		if (DEBUG)
			System.out.println("registered Configurator"); //$NON-NLS-1$
	}

	public void stop(BundleContext context) throws Exception
	{
		if (configuratorRegistration != null) {
			configuratorRegistration.unregister();
			configuratorRegistration = null;
		}
		if (commandRegistration != null) {
			commandRegistration.unregister();
			commandRegistration = null;
		}
	}
}
