/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.emrys.webosgi.launcher.configurator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Manifest;

import org.emrys.webosgi.launcher.configurator.utils.EquinoxUtils;
import org.emrys.webosgi.launcher.configurator.utils.SimpleConfiguratorConstants;
import org.emrys.webosgi.launcher.configurator.utils.SimpleConfiguratorUtils;
import org.emrys.webosgi.launcher.configurator.utils.Utils;
import org.ops4j.pax.url.war.ServiceConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Jar;
import aQute.libg.version.Version;

/*
 * SimpleConfigurator provides ways to install bundles listed in a file
 * accessible by the specified URL and expect states for it in advance without
 * actual application.
 * In every methods of SimpleConfiguration object,
 * 1. A value will be gotten by @{link BundleContext#getProperty(key) with
 * {@link SimpleConfiguratorConstants#PROP_KEY_EXCLUSIVE_INSTALLATION} as a key.
 * 2. If it equals "true", it will do exclusive installation, which means that
 * the bundles will not be listed in the specified url but installed at the time
 * of the method call except SystemBundle will be uninstalled. Otherwise, no
 * uninstallation will not be done.
 */
public class SimpleConfiguratorImpl implements Configurator {

	private static URL configurationURL = null;
	private static Object configurationLock = new Object();

	private final BundleContext context;
	private ConfigApplier configApplier;
	private final Bundle bundle;

	public SimpleConfiguratorImpl(BundleContext context, Bundle bundle) {
		this.context = context;
		this.bundle = bundle;
	}

	private URL getConfigurationURL() throws IOException {
		String specifiedURL = context
				.getProperty(SimpleConfiguratorConstants.PROP_KEY_CONFIGURL);
		if (specifiedURL == null)
			specifiedURL = "file:"
					+ SimpleConfiguratorConstants.CONFIGURATOR_FOLDER + "/"
					+ SimpleConfiguratorConstants.CONFIG_LIST;

		try {
			// If it is not a file URL use it as is
			if (!specifiedURL.startsWith("file:"))
				return new URL(specifiedURL);
		} catch (MalformedURLException e) {
			return null;
		}

		try {
			// if it is an absolute file URL, use it as is
			boolean done = false;
			URL url = null;
			String file = specifiedURL;
			while (!done) {
				// TODO what is this while loop for? nested file:file:file:
				// urls?
				try {
					url = Utils.buildURL(file);
					file = url.getFile();
				} catch (java.net.MalformedURLException e) {
					done = true;
				}
			}
			if (url != null && new File(url.getFile()).isAbsolute())
				return url;

			// if it is an relative file URL, then resolve it against the
			// configuration area
			// TODO Support relative file URLs when not on Equinox
			URL[] configURL = EquinoxUtils.getConfigAreaURL(context);
			if (configURL != null) {
				File userConfig = new File(configURL[0].getFile(), url
						.getFile());
				if (configURL.length == 1)
					return userConfig.exists() ? userConfig.toURL() : null;

				File sharedConfig = new File(configURL[1].getFile(), url
						.getFile());
				if (!userConfig.exists())
					return sharedConfig.exists() ? sharedConfig.toURL() : null;

				if (!sharedConfig.exists())
					return userConfig.toURL();

				URI base = EquinoxUtils.getInstallLocationURI(context);

				URL sharedConfigURL = sharedConfig.toURL();
				List sharedBundles = SimpleConfiguratorUtils.readConfiguration(
						sharedConfigURL, base);

				URL userConfigURL = userConfig.toURL();
				List userBundles = SimpleConfiguratorUtils.readConfiguration(
						userConfigURL, base);

				return (userBundles.containsAll(sharedBundles)) ? userConfigURL
						: sharedConfigURL;
			}
		} catch (MalformedURLException e) {
			return null;
		}

		// Last resort
		try {
			return Utils.buildURL(specifiedURL);
		} catch (MalformedURLException e) {
			// Ignore
		}

		return null;
	}

	public void applyConfiguration(URL url, boolean excludeInstall)
			throws IOException {
		synchronized (configurationLock) {
			if (url == null)
				return;
			configurationURL = url;

			if (this.configApplier == null)
				configApplier = new ConfigApplier(context, bundle);
			configApplier.install(url, excludeInstall);
		}
	}

	public void applyConfiguration(URL url) throws IOException {
		applyConfiguration(url, isExclusiveInstallation());
	}

	private boolean isExclusiveInstallation() {
		String value = context
				.getProperty(SimpleConfiguratorConstants.PROP_KEY_EXCLUSIVE_INSTALLATION);
		if (value == null || value.trim().length() == 0)
			value = "true";
		return Boolean.valueOf(value).booleanValue();
	}

	public void applyConfiguration() throws IOException {
		synchronized (configurationLock) {
			configurationURL = getConfigurationURL();
			// if configured url is null, indicating this invoke is install wab
			// apps later. Do not use exclude install mode.
			boolean laterInstallWebapps = configurationURL == null;
			// Scan plugins/ directory and append all plugins at the head of the
			// configuration file.
			configurationURL = appendScanedPlugins(configurationURL);
			applyConfiguration(configurationURL, !laterInstallWebapps);
		}
	}

	private URL appendScanedPlugins(URL configurationURL) throws IOException {
		URI base = EquinoxUtils.getInstallLocationURI(context);
		File newCfgFile = File.createTempFile("bundles", ".info");
		PrintWriter printer = new PrintWriter(newCfgFile, "UTF-8");

		try {
			List<String> bundleSymbleNames = new ArrayList<String>();
			// If the bundles information URL not given, the framework is
			// embedded from a OSGi runtime, and we only need to scan web
			// bundles from WebApps dir.
			if (configurationURL != null) {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(configurationURL.openStream(),
								"UTF-8"));
				try {
					String line = null;
					while ((line = reader.readLine()) != null) {
						if (!line.trim().startsWith("#")) {
							String[] segs = line.split(",");
							if (segs.length > 2)
								bundleSymbleNames.add(segs[0] + "," + segs[1]);
						}
						printer.println(line);
					}
				} finally {
					reader.close();
				}

				// Scan the default plugins root. /plugins
				scanPlugins(base, "plugins", bundleSymbleNames, printer);
				scanPlugins(base, "deploy", bundleSymbleNames, printer);
			}
			// Scan for .war web application.
			scanWebApps(base, "webapps", bundleSymbleNames, printer);
			printer.flush();
		} finally {

			printer.flush();
			printer.close();
		}
		return newCfgFile.toURI().toURL();
	}

	private void scanWebApps(URI base, String path,
			List<String> bundleSymbolNames, PrintWriter printer)
			throws MalformedURLException, UnsupportedEncodingException {
		URL baseFileURL = new URL(URLDecoder.decode(base.toURL()
				.toExternalForm(), "UTF-8"));
		File fwkInstallDir = new File(baseFileURL.getFile());
		File webAppsDir = new File(fwkInstallDir, path);
		if (!webAppsDir.exists())
			return;

		File[] webAppFiles = webAppsDir.listFiles();
		for (File f : webAppFiles) {
			// Unlike bundle file, the symbolic name and version should be
			// parsed from its MANISFEST.MF file at first.
			String symbolName = null;
			String version = "0.0.0";

			try {
				Jar jar = new Jar(f);
				Manifest manifest = jar.getManifest();
				symbolName = manifest.getMainAttributes().getValue(
						Analyzer.BUNDLE_SYMBOLICNAME);
				version = manifest.getMainAttributes().getValue(
						Analyzer.BUNDLE_VERSION);
				version = new Version(version).toString();
			} catch (Exception e) {
				// e.printStackTrace();
				symbolName = null;
				version = "0.0.0";
			}

			if (symbolName == null || symbolName.length() == 0) {
				String name = f.getName();
				if (f.isFile()) {
					int index = name.lastIndexOf('.');
					if (index > -1 && index > 0)
						name = name.substring(0, index);
				}
				// Use war's file name as wab symblic name.
				symbolName = f.getName();
				symbolName = symbolName.replaceAll("[^a-zA-Z_0-9.-]", "_")
						.replaceAll("(^|\\.)(\\d+)", "$1_$2");
				int index = name.indexOf("_");
				if (index > -1 && index != name.length() - 1) {
					version = name.substring(index + 1);
				}
			}

			// If the bundle with the same symble name and version was
			// configured in specified bundles.info file, skip it.
			if (bundleSymbolNames.contains(symbolName + "," + version))
				continue;
			bundleSymbolNames.add(symbolName + "," + version);

			StringBuffer sb = new StringBuffer();
			sb.append(symbolName);
			sb.append(",");
			sb.append(version);
			sb.append(",");
			sb.append(ServiceConstants.PROTOCOL_WEB_BUNDLE + ":"
					+ URLDecoder.decode(f.toURI().toURL().toString(), "UTF-8"));
			sb.append(",");
			sb.append(4);
			sb.append(",");
			sb.append("false");

			printer.println(sb.toString());
		}
	}

	private void scanPlugins(URI base, String path,
			List<String> bundleSymbleNames, PrintWriter printer)
			throws MalformedURLException, UnsupportedEncodingException {
		URL baseFileURL = new URL(URLDecoder.decode(base.toURL()
				.toExternalForm(), "UTF-8"));
		File fwkInstallDir = new File(baseFileURL.getFile());
		File pluginsDir = new File(fwkInstallDir, path);
		if (!pluginsDir.exists())
			return;

		File[] pluginFiles = pluginsDir.listFiles();
		for (File f : pluginFiles) {
			String name = f.getName();
			if (f.isFile()) {
				int index = name.lastIndexOf('.');
				if (index > -1 && index > 0)
					name = name.substring(0, index);
			}

			String symbleName = name;
			String version = "0.0.0";
			int index = name.indexOf("_");
			if (index > -1 && index != name.length() - 1) {
				symbleName = name.substring(0, index);
				version = name.substring(index + 1);
			}
			// If the bundle with the same symble name and version was
			// configured in specified bundles.info file, skip it.
			if (bundleSymbleNames.contains(symbleName + "," + version))
				continue;
			bundleSymbleNames.add(symbleName + "," + version);

			StringBuffer sb = new StringBuffer();
			sb.append(symbleName);
			sb.append(",");
			sb.append(version);
			sb.append(",");
			sb.append(path + "/" + f.getName());
			sb.append(",");
			sb.append(4);
			sb.append(",");
			sb.append("false");

			printer.println(sb.toString());
		}
	}

	public URL getUrlInUse() {
		synchronized (configurationLock) {
			return configurationURL;
		}
	}
}
