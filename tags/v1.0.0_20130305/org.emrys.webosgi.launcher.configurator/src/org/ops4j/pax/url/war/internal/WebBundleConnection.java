/*
 * Copyright 2008 Alin Dreghiciu, Achim Nierbeck.
 *
 * Licensed  under the  Apache License,  Version 2.0  (the "License");
 * you may not use  this file  except in  compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed  under the  License is distributed on an "AS IS" BASIS,
 * WITHOUT  WARRANTIES OR CONDITIONS  OF ANY KIND, either  express  or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.url.war.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.jar.Manifest;

import org.ops4j.pax.swissbox.bnd.OverwriteMode;
import org.osgi.framework.Constants;

import aQute.lib.osgi.Jar;
import aQute.libg.version.Version;

/**
 * Url connection for webbundle protocol handler.
 * 
 * @author Guillaume Nodet
 */
public class WebBundleConnection extends WarConnection {

	public WebBundleConnection(URL url, Configuration config)
			throws MalformedURLException {
		super(url, config);
	}

	@Override
	protected InputStream createBundle(InputStream inputStream,
			Properties instructions, String warUri) throws Exception {
		BufferedInputStream bis = new BufferedInputStream(inputStream,
				64 * 1024);
		bis.mark(64 * 1024);
		/*
		 * boolean isBundle = false; try { JarInputStream jis = new
		 * JarInputStream(bis); Manifest man = jis.getManifest(); if
		 * (man.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME) !=
		 * null) { isBundle = true; }
		 * 
		 * } catch (IOException e) { // Ignore e.printStackTrace(); } finally {
		 * if (bis.markSupported()) { try { bis.reset(); } catch (IOException
		 * ignore) { // Ignore since buffer is already resetted } } } if
		 * (isBundle) { final Properties originalInstructions = BndUtils
		 * .parseInstructions(getURL().getQuery()); if
		 * (originalInstructions.size() > 1 || originalInstructions.size() == 1
		 * && !originalInstructions.containsKey("Web-ContextPath")) { throw new
		 * MalformedURLException(
		 * "The webbundle URL handler can not be used with bundles"); } }
		 */

		// If the web bundle has its symbolic name and context path in
		// MANIFEST.MF file, use them at first.
		URL warURL = new URL(warUri);
		Jar jar = new Jar(warURL.getPath(), new File(warURL.getPath()));
		Manifest manifest = jar.getManifest();
		String symbolicName = manifest.getMainAttributes().getValue(
				Constants.BUNDLE_SYMBOLICNAME);
		String contextPath = manifest.getMainAttributes().getValue(
				"Web-ContextPath");
		if (symbolicName != null && symbolicName.trim().length() > 0)
			instructions.put(Constants.BUNDLE_SYMBOLICNAME, symbolicName);
		if (contextPath != null && contextPath.trim().length() > 0)
			instructions.put("Web-ContextPath", contextPath);

		// OSGi-Spec 128.3.1 WAB Definition
		// The Context Path must always begin with a forward slash ( ?/?).
		if (instructions.get("Web-ContextPath") != null) {
			String ctxtPath = (String) instructions.get("Web-ContextPath");
			if (!ctxtPath.startsWith("/")) {
				ctxtPath = "/" + ctxtPath;
				instructions.setProperty("Web-ContextPath", ctxtPath);
			}
		}
		// If not found bundle symblic name, use default.
		if (instructions.get(Constants.BUNDLE_SYMBOLICNAME) == null) {
			String defSymblicName = jar.getSource().getName();
			defSymblicName = defSymblicName.replaceAll("[^a-zA-Z_0-9.-]", "_")
					.replaceAll("(^|\\.)(\\d+)", "$1_$2");
			instructions.put(Constants.BUNDLE_SYMBOLICNAME, defSymblicName);
		}

		return super.createBundle(bis, instructions, warUri,
				OverwriteMode.MERGE);
	}

	@Override
	protected Properties getInstructions() throws MalformedURLException {
		Properties instructions = super.getInstructions();
		// Generate default Context Path of This wab if not found.
		if (!instructions.contains("Web-ContextPath"))
			instructions
					.setProperty("Web-ContextPath", getDefaultContextPath());

		// Add WebContainer framework bundle as required bundle
		if (!instructions.containsKey("Require-Bundle")) {
			instructions.setProperty("Require-Bundle",
					"org.emrys.webosgi.core.resource;bundle-version=\"1.0.0\"");
		}

		return instructions;
	}

	protected String getDefaultContextPath() {
		String path = this.getURL().getPath();
		int i = path.lastIndexOf("/");
		if (i == path.length() - 1) {
			path = path.substring(0, path.length() - 1);
			i = path.lastIndexOf("/");
		}
		if (i > -1) {
			String ctxPath = path.substring(i + 1);
			if (ctxPath.endsWith(".war"))
				ctxPath = ctxPath.substring(0, ctxPath.length() - 4);
			i = ctxPath.lastIndexOf('_');
			if (i > 0 && i != ctxPath.length() - 1) {
				// If the war name contains version string, remove it.
				if (Version.VERSION.matcher(ctxPath.substring(i + 1)).matches())
					ctxPath = ctxPath.substring(0, i);
			}
			return ctxPath;
		}
		return null;
	}
}
