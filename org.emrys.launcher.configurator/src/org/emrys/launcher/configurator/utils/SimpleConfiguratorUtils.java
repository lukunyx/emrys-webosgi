/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.emrys.launcher.configurator.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

import org.emrys.core.launcher.internal.FwkExternalAgent;
import org.emrys.core.launcher.internal.IFwkEnvConstants;

public class SimpleConfiguratorUtils {

	private static final String UNC_PREFIX = "//";
	private static final String VERSION_PREFIX = "#version=";
	public static final Version COMPATIBLE_VERSION = new Version(1, 0, 0);
	public static final VersionRange VERSION_TOLERANCE = new VersionRange(
			COMPATIBLE_VERSION, true, new Version(2, 0, 0), false);

	private static final String FILE_SCHEME = "file";
	private static final String REFERENCE_PREFIX = "reference:";
	private static final String FILE_PREFIX = "file:";
	private static final String COMMA = ",";
	private static final String ENCODED_COMMA = "%2C";

	public static List readConfiguration(URL url, URI base) throws IOException {
		List bundles = new ArrayList();

		BufferedReader r = null;
		try {
			// Fix the bug when the bundle's reference url has some none ascii
			// char, like Chinese
			// character, the bundle url will be failed to parse.
			r = new BufferedReader(new InputStreamReader(url.openStream(),
					"UTF-8"));
		} catch (IOException e) {
			// if the exception is a FNF we return an empty bundle list
			if (e instanceof FileNotFoundException)
				return bundles;
			throw e;
		}
		try {
			String line;
			while ((line = r.readLine()) != null) {
				line = line.trim();
				// ignore any comment or empty lines
				if (line.length() == 0)
					continue;

				if (line.startsWith("#")) {//$NON-NLS-1$
					parseCommentLine(line);
					continue;
				}

				BundleInfo bundleInfo = parseBundleInfoLine(line, base);
				if (bundleInfo != null)
					bundles.add(bundleInfo);
			}
		} finally {
			try {
				r.close();
			} catch (IOException ex) {
				// ignore
			}
		}
		return bundles;
	}

	public static void parseCommentLine(String line) {
		// version
		if (line.startsWith(VERSION_PREFIX)) {
			String version = line.substring(VERSION_PREFIX.length()).trim();
			if (!VERSION_TOLERANCE.isIncluded(new Version(version)))
				throw new IllegalArgumentException("Invalid version: "
						+ version);
		}
	}

	public static BundleInfo parseBundleInfoLine(String line, URI base) {
		// symbolicName,version,location,startLevel,markedAsStarted
		StringTokenizer tok = new StringTokenizer(line, COMMA);
		int numberOfTokens = tok.countTokens();
		if (numberOfTokens < 5)
			throw new IllegalArgumentException(
					"Line does not contain at least 5 tokens: " + line);

		String symbolicName = tok.nextToken().trim();
		String version = tok.nextToken().trim();
		String bundleUri = tok.nextToken().trim();
		URI location = parseLocation(bundleUri);
		int startLevel = Integer.parseInt(tok.nextToken().trim());
		boolean markedAsStarted = Boolean.valueOf(tok.nextToken())
				.booleanValue();
		BundleInfo result = new BundleInfo(symbolicName, version, location,
				startLevel, markedAsStarted);
		if (!location.isAbsolute())
			result.setBaseLocation(base);
		return result;
	}

	public static URI parseLocation(String location) {
		// Special process for WebOSGi framework. Because Host web application
		// is regarded as a
		// normal bundle, web want this bundle's uri is ../../plugins.
		if (location.equalsIgnoreCase("#{"
				+ IFwkEnvConstants.ATTR_FWK_WEBAPP_DEPLOY_PATH + "}")) {
			String hostWebLocation = (String) FwkExternalAgent.getInstance()
					.getFwkEvnAttribute(
							IFwkEnvConstants.ATTR_FWK_WEBAPP_DEPLOY_PATH);
			if (hostWebLocation != null) {
				File hostWebRootDir = new File(hostWebLocation);
				if (hostWebRootDir.exists())
					return hostWebRootDir.toURI();
			}
		}

		// decode any commas we previously encoded when writing this line
		int encodedCommaIndex = location.indexOf(ENCODED_COMMA);
		while (encodedCommaIndex != -1) {
			location = location.substring(0, encodedCommaIndex) + COMMA
					+ location.substring(encodedCommaIndex + 3);
			encodedCommaIndex = location.indexOf(ENCODED_COMMA);
		}

		if (File.separatorChar != '/') {
			int colon = location.indexOf(':');
			String scheme = colon < 0 ? null : location.substring(0, colon);
			if (scheme == null || scheme.equals(FILE_SCHEME))
				location = location.replace(File.separatorChar, '/');
			// if the file is a UNC path, insert extra leading // if needed to
			// make a valid URI (see
			// bug 207103)
			if (scheme == null) {
				if (location.startsWith(UNC_PREFIX)
						&& !location.startsWith(UNC_PREFIX, 2))
					location = UNC_PREFIX + location;
			} else {
				// insert UNC prefix after the scheme
				if (location.startsWith(UNC_PREFIX, colon + 1)
						&& !location.startsWith(UNC_PREFIX, colon + 3))
					location = location.substring(0, colon + 3)
							+ location.substring(colon + 1);
			}
		}

		try {
			URI uri = new URI(location);
			if (!uri.isOpaque())
				return uri;
		} catch (URISyntaxException e1) {
			// this will catch the use of invalid URI characters (e.g. spaces,
			// etc.)
			// ignore and fall through
		}

		try {
			return URIUtil.fromString(location);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Invalid location: " + location);
		}
	}

	public static void transferStreams(InputStream source,
			OutputStream destination) throws IOException {
		source = new BufferedInputStream(source);
		destination = new BufferedOutputStream(destination);
		try {
			byte[] buffer = new byte[8192];
			while (true) {
				int bytesRead = -1;
				if ((bytesRead = source.read(buffer)) == -1)
					break;
				destination.write(buffer, 0, bytesRead);
			}
		} finally {
			try {
				source.close();
			} catch (IOException e) {
				// ignore
			}
			try {
				destination.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}

	// This will produce an unencoded URL string
	public static String getBundleLocation(BundleInfo bundle,
			boolean useReference) {
		URI location = bundle.getLocation();
		String scheme = location.getScheme();
		String host = location.getHost();
		String path = location.getPath();

		if (location.getScheme() == null) {
			URI baseLocation = bundle.getBaseLocation();
			if (baseLocation != null && baseLocation.getScheme() != null) {
				scheme = baseLocation.getScheme();
				host = baseLocation.getHost();
			}
		}

		String bundleLocation = null;
		try {
			URL bundleLocationURL = new URL(scheme, host, path);
			bundleLocation = bundleLocationURL.toExternalForm();

		} catch (MalformedURLException e1) {
			bundleLocation = location.toString();
		}

		if (useReference && bundleLocation.startsWith(FILE_PREFIX))
			bundleLocation = REFERENCE_PREFIX + bundleLocation;
		return bundleLocation;
	}
}
