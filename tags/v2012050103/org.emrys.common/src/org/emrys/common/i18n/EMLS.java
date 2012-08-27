/*******************************************************************************
 * Copyright (c) 2010 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.common.i18n;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.RegistryFactory;
import org.emrys.common.CommonActivator;
import org.emrys.common.util.BundleServiceUtil;
import org.emrys.common.util.FileUtil;


/**
 * Manage external and multiple language string for plug-ins, and offer access interface for these
 * string with key specified and maybe any arguments to replace some symbol in the retrieved string
 * content. This class will automatically decide to load which messages.properties resource
 * according to the local language.
 * 
 * @author Leo Chang - EMRYS
 * @version 2010-9-6
 */
public class EMLS {
	private static final String MSG_FILE_NAME = "messages";
	private static final String MSG_FILE_EXT = "properties";
	private static final String DEFAULT_NL = "en_US";
	public static final String DEFAULT_SCOPE = "i18n_default_scope";

	private static Map<Long, BundledStringDomain> buffers = new HashMap<Long, BundledStringDomain>();
	private Map<String, String> msgMap;
	private static List<ExtLocaleDeciderInfo> deciderList = new ArrayList<ExtLocaleDeciderInfo>();
	private static boolean extDeciderInited;

	/**
	 * A bundle may have many messages file, this class manipulate those EMLS.
	 * 
	 * @author Leo Chang - EMRYS
	 * @version 2011-6-24
	 */
	private static class BundledStringDomain {
		Map<String, EMLS> map = new HashMap<String, EMLS>();
		private final long bundleID;

		public BundledStringDomain(long bundleID) {
			this.bundleID = bundleID;
		}

		void add(EMLS emls, String lang) {
			map.put(lang, emls);
		}

		void remove(String lang) {
			map.remove(lang);
		}

		EMLS getEMLS(String lang) {
			EMLS emls = map.get(lang);
			if (emls == null) {
				try {
					String msgFileName = getFileName(MSG_FILE_NAME, lang);
					URI messgesUri;

					URL entry = BundleServiceUtil.getBundle(bundleID).getEntry(msgFileName);
					// If the specified lang not found its messages.properties file.
					// try the Locale.defaultLocale() at first. And then the en_US at last.
					if (entry == null) {
						String sysLang = Locale.getDefault().getLanguage() + "_"
								+ Locale.getDefault().getCountry();
						msgFileName = getFileName(MSG_FILE_NAME, sysLang);
						entry = BundleServiceUtil.getBundle(bundleID).getEntry(msgFileName);
					}
					if (entry == null) {
						msgFileName = getFileName(MSG_FILE_NAME, DEFAULT_NL);
						entry = BundleServiceUtil.getBundle(bundleID).getEntry(msgFileName);
					}

					if (entry != null) {
						messgesUri = entry.toURI();
						emls = new EMLS(messgesUri);
					} else
						emls = new EMLS(null); // Create an empty EMLS to avoid the
					// NullPointerException.
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
			}
			if (emls != null)
				map.put(lang, emls);
			return emls;
		}
	}

	protected EMLS(URI messgesUri) {
		try {
			msgMap = new HashMap<String, String>();
			if (messgesUri != null) {
				// messgesUri = FileLocator.toFileURL(messgesUri.toURL()).toURI();
				StringBuffer content = FileUtil
						.getContent(messgesUri.toURL().openStream(), "UTF-8");
				String[] segements = content.toString().split(System.getProperty("line.separator"));
				for (int i = 0; i < segements.length; i++) {
					int index = segements[i].indexOf('=');
					// note: value not need to trim.
					if (index > 0 && index < segements[i].length() - 1) {
						msgMap.put(segements[i].substring(0, index).trim(), segements[i]
								.substring(index + 1));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			CommonActivator.getInstance().log(e);
		}
	}

	/**
	 * Get the system current preferred Locale and language setting. It's not just
	 * Locale.getDefault(); This use the default scope.
	 * 
	 * @return
	 */
	public static Locale getSysLocale() {
		return decideLocale(DEFAULT_SCOPE);
	}

	/**
	 * Retrieve EMLS for give plug-in and default scope.
	 * 
	 * @param bundleID
	 * @return
	 */
	public static EMLS getEMLS(long bundleID) {
		return getEMLS(bundleID, (String) null);
	}

	/**
	 * Retrieve EMLS for give plug-in.
	 * 
	 * @param bundleID
	 * @param scope
	 * @return
	 */
	public static EMLS getEMLS(long bundleID, String scope) {
		return getEMLS(bundleID, scope == null ? DEFAULT_SCOPE : scope, null);
	}

	/**
	 * Retrieve EMLS for give plug-in and Locale
	 * 
	 * @param bundleID
	 * @param Locale
	 *            locale
	 * @return
	 */
	public static EMLS getEMLS(long bundleID, Locale locale) {
		return getEMLS(bundleID, null, locale);
	}

	/**
	 * Get the EMLS instance for the bundle presenting by the argument bundleID, and the scope,
	 * specified Locale of messages.
	 * 
	 * @param bundleID
	 *            bundle's id that including the message files.
	 * @param scope
	 *            the scope of EMLS
	 * @param locale
	 *            specify the Locale of dediring EMLS instance
	 * @return
	 */
	private static EMLS getEMLS(long bundleID, String scope, Locale locale) {
		BundledStringDomain domain = buffers.get(bundleID);
		if (domain == null) {
			domain = new BundledStringDomain(bundleID);
			buffers.put(bundleID, domain);
		}

		if (locale == null)
			locale = decideLocale(scope);
		return domain.getEMLS(locale.getLanguage() + "_" + locale.getCountry());
	}

	private static String getFileName(String baseName, String lang) {
		String name = baseName;
		if (!lang.equalsIgnoreCase(DEFAULT_NL))
			name += "_" + lang;

		return name + "." + MSG_FILE_EXT;
	}

	/**
	 * Retrieve external multiple language string from messages.property file under plugin's root.
	 * And this method will replace arguments array to {0},{1},etc. in the retrieved string at last.
	 * 
	 * @param pluginID
	 *            Bundle.getBundleID()
	 * @param key
	 * @param arguments
	 * @return
	 */
	public static String MSG(long pluginID, String key, String... arguments) {
		return MSG(DEFAULT_SCOPE, pluginID, key, arguments);
	}

	/**
	 * Obtain the value of the external string specified by a key and scope.
	 * 
	 * @param scope
	 *            The effect scope of this external string's key.
	 * @param pluginID
	 * @param key
	 * @param arguments
	 * @return
	 */
	public static String MSG(String scope, long pluginID, String key, String... arguments) {
		EMLS eMLS = getEMLS(pluginID, scope);
		if (eMLS != null)
			return eMLS.msg(key, arguments);
		return key;
	}

	/**
	 * Get the locale of a EMLS instance.
	 * 
	 * @param emls
	 * @return
	 */
	public static Locale getLocaleOfEMLS(EMLS emls) {
		Iterator<BundledStringDomain> vit = buffers.values().iterator();
		while (vit.hasNext()) {
			BundledStringDomain domain = vit.next();
			Iterator<Entry<String, EMLS>> eit = domain.map.entrySet().iterator();
			while (eit.hasNext()) {
				Entry<String, EMLS> entry = eit.next();
				if (entry.getValue().equals(emls)) {
					String[] segs = entry.getKey().split("_");
					Locale[] locales = Locale.getAvailableLocales();
					for (Locale l : locales) {
						if (l.getLanguage().equals(segs[0]) && l.getCountry().equals(segs[1]))
							return l;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Retrieve external multiple language string from messages.property file under this plugin's
	 * root. And this method will replace arguments array to {0},{1},etc. in the retrieved string at
	 * last.
	 * 
	 * @param key
	 * @param arguments
	 * @return
	 */
	public String msg(String key, String... arguments) {
		Map<String, String> map = getMsgesMap();
		if (map != null && !map.isEmpty()) {
			String content = map.get(key);
			if (content != null) {
				content = unicodeToString(content);
				for (int i = 0; i < arguments.length; i++)
					content = content.replace("{" + i + "}", arguments[i]);

				// Replace all special black chars.
				// content.replace("\=", "=")
				return content;
			}
		}
		return key;
	}

	private String unicodeToString(String str) {
		Pattern pattern = Pattern.compile("(\\\\u(\\p{XDigit}{4}))");
		Matcher matcher = pattern.matcher(str);
		char ch;
		while (matcher.find()) {
			ch = (char) Integer.parseInt(matcher.group(2), 16);
			str = str.replace(matcher.group(1), ch + "");
		}
		return str;
	}

	private Map<String, String> getMsgesMap() {
		return msgMap;
	}

	private static class ExtLocaleDeciderInfo {
		ExternalStrLocaleDecider decider;
		boolean registered;
		int prority;
		String name;
		String id;
	}

	/**
	 * @param id
	 * @param name
	 * @param prority
	 * @param decider
	 */
	public static void registerLocalDecider(String id, String name, int prority,
			ExternalStrLocaleDecider decider) {
		if (id != null && decider != null) {
			ExtLocaleDeciderInfo info = new ExtLocaleDeciderInfo();
			info.id = id;
			info.name = name;
			info.prority = prority;
			info.decider = decider;
			info.registered = true;
			deciderList.add(info);
		}
	}

	public static void unregisterLocalDecider(String id) {
		for (Iterator<ExtLocaleDeciderInfo> it = deciderList.iterator(); it.hasNext();) {
			if (it.next().id.equals(id))
				it.remove();
		}
	}

	private static Locale decideLocale(String scope) {
		if (scope == null || scope.length() == 0)
			scope = DEFAULT_SCOPE;

		List<ExtLocaleDeciderInfo> list = getExtLocaleDeciders(false);
		for (Iterator<ExtLocaleDeciderInfo> it = list.iterator(); it.hasNext();) {
			ExtLocaleDeciderInfo info = it.next();
			ExternalStrLocaleDecider decider = info.decider;
			String[] supportedScopes = decider.getSupportScopes();
			if (supportedScopes == null)
				continue;
			for (int i = 0; i < supportedScopes.length; i++)
				if (scope.equals(supportedScopes[i])) {
					Locale locale = info.decider.getLocale(scope);
					if (locale != null)
						return locale;
				}
		}

		return Locale.getDefault();
	}

	private static List<ExtLocaleDeciderInfo> getExtLocaleDeciders(boolean update) {
		if (!extDeciderInited || update) {
			extDeciderInited = true;
			for (Iterator<ExtLocaleDeciderInfo> it = deciderList.iterator(); it.hasNext();) {
				if (!it.next().registered)
					it.remove();
			}
			IExtensionPoint extPoint = RegistryFactory.getRegistry().getExtensionPoint(
					CommonActivator.getInstance().getBundleSymbleName() + ".externalStringLocale");
			IConfigurationElement[] ces = extPoint.getConfigurationElements();
			for (IConfigurationElement ce : ces) {
				if (!ce.getName().equals("decider"))
					continue;

				ExtLocaleDeciderInfo info = new ExtLocaleDeciderInfo();
				int priority = 0;
				try {
					priority = Integer.parseInt(ce.getAttribute("priority"));
				} catch (Exception e) {
				}

				try {
					String id = ce.getAttribute("id");
					String name = ce.getAttribute("name");
					ExternalStrLocaleDecider decider;
					decider = (ExternalStrLocaleDecider) ce.createExecutableExtension("class");
					if (id != null) {
						info.id = id;
						info.name = name;
						info.prority = priority;
						info.decider = decider;
						deciderList.add(info);
					}
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}

			Collections.sort(deciderList, new Comparator<ExtLocaleDeciderInfo>() {
				public int compare(ExtLocaleDeciderInfo o1, ExtLocaleDeciderInfo o2) {
					return o1.prority > o2.prority ? -1 : 1;
				}
			});
		}
		return deciderList;
	}
}
