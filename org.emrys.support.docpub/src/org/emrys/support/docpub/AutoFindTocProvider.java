package org.emrys.support.docpub;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.help.AbstractTocProvider;
import org.eclipse.help.IToc;
import org.eclipse.help.ITocContribution;
import org.eclipse.help.ITopic;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.base.IHelpBaseConstants;
import org.eclipse.help.internal.base.remote.RemoteContentLocator;
import org.eclipse.help.internal.dynamic.DocumentReader;
import org.eclipse.help.internal.toc.Toc;
import org.emrys.webosgi.common.util.FileUtil;
import org.emrys.webosgi.common.util.FileUtil.IFileVisitor;
import org.emrys.webosgi.core.internal.FwkRuntime;
import org.osgi.service.prefs.Preferences;

/**
 * Search all .doc bundle from features repository and create ITocContribution.
 * 
 * @author Leo Chang
 * @version 2011-7-6
 */
public class AutoFindTocProvider extends AbstractTocProvider {
	public static final String FEATURE_SITE_TOC_CON_PREFIX = "FS$";
	public static Map<IPath, File> docJarPaths = new HashMap<IPath, File>();
	private static Map<String, Map<String, List<ITocContribution>>> siteVersionTocConMap = new HashMap<String, Map<String, List<ITocContribution>>>();

	static {
		IPreferencesService service = Platform.getPreferencesService();
		Preferences root = service.getRootNode();
		Preferences myInstanceNode = root.node(InstanceScope.SCOPE).node(
				HelpBasePlugin.PLUGIN_ID);
		if (myInstanceNode != null)
			myInstanceNode.putBoolean(IHelpBaseConstants.P_KEY_REMOTE_HELP_ON,
					true);
		/*Platform.getPreferencesService().getBoolean(HelpBasePlugin.PLUGIN_ID,
				IHelpBaseConstants.P_KEY_REMOTE_HELP_ON, false, null);*/
	}

	private String baseUrl;

	@Override
	public ITocContribution[] getTocContributions(final String locale) {
		docJarPaths.clear();
		siteVersionTocConMap.clear();
		final File tmpUnzippedDocRoot = new File(Activator.getInstance()
				.getComponentWorkspaceRoot(), "tmpUnzippedDoc");
		if (tmpUnzippedDocRoot.exists())
			FileUtil.deleteAllFile(tmpUnzippedDocRoot, null);

		String hostName = (String) ((List) FwkRuntime.getInstance()
				.getFrameworkAttribute(FwkRuntime.ATTR_WEB_APP_HOSTS)).get(0);
		Integer hostPort = (Integer) FwkRuntime.getInstance()
				.getFrameworkAttribute(FwkRuntime.ATTR_WEB_APP_PORT);
		String servletContextPath = Activator.getInstance()
				.getBundleServletContext().getContextPath();

		baseUrl = "http://"
				+ hostName
				+ ((hostPort != null || hostPort != 80) ? (":" + hostPort) : "")
				+ servletContextPath;

		final Set<String> siteDisplayNameSet = new HashSet<String>();

		final List<ITocContribution> result = new ArrayList<ITocContribution>();
		final List<IToc> tocs = new ArrayList<IToc>();
		File featuresRootFile = org.emrys.support.site.Activator
				.getInstance().getFeaUpdateSitesRoot().toFile();
		IFileVisitor visitor = new IFileVisitor() {
			public boolean visit(File f) {
				IPath fPath = new Path(f.getAbsolutePath());
				if (fPath.segmentCount() < 4)
					return f.isDirectory();

				String siteDisplayName = null;

				File displayNameFile = fPath.append("displayname.txt").toFile();
				if (displayNameFile.exists()) {
					try {
						siteDisplayName = FileUtil.getContent(displayNameFile,
								"UTF-8").toString().trim();
						if (siteDisplayName != null
								&& siteDisplayName.length() > 0) {
							if (!siteDisplayNameSet.contains(siteDisplayName)) {
								siteDisplayNameSet.add(siteDisplayName);
								return true;
							} else if (!fPath.append("site.xml").toFile()
									.exists())
								// If not a Updatesite version folder there is a
								// displayname.txt file.
								return false;
						}
					} catch (IOException e) {
						// e.printStackTrace();
					}
				}

				String parentFileName = fPath.segment(fPath.segmentCount() - 2);
				if (!parentFileName.equals("plugins"))
					return f.isDirectory();

				// FIXME: only care the toc.xml in a bunlde's root and the
				// bundle's name
				// contains "doc" now. But it's enough at present.
				if (f.getName().contains(".doc")
						&& !f.getName().contains(".doc.source")) {
					String versionName = fPath
							.segment(fPath.segmentCount() - 3);
					String siteName = fPath.segment(fPath.segmentCount() - 4);

					displayNameFile = fPath.removeLastSegments(3).append(
							"displayname.txt").toFile();
					if (displayNameFile.exists()) {
						try {
							siteDisplayName = FileUtil.getContent(
									displayNameFile, "UTF-8").toString().trim();
						} catch (Throwable t) {
							// e.printStackTrace();
						}
					}

					if (siteDisplayName == null
							|| siteDisplayName.length() == 0)
						siteDisplayName = siteName;

					if (f.isFile() && f.getName().endsWith(".jar")) {
						JarFile jar = null;
						try {
							String thisPluginId = Activator.getInstance()
									.getBundleSymbleName();
							String pluginName = f.getName().substring(0,
									f.getName().indexOf('_'));
							String fullPluginName = FEATURE_SITE_TOC_CON_PREFIX
									+ siteName + "$" + versionName + "$"
									+ pluginName;
							jar = new JarFile(f);
							// FIXME: Here not hadle toc link cross multiple
							// toc.
							// Search for the toc*.xml file.
							boolean hasTocContribute = false;
							Enumeration<JarEntry> entries = jar.entries();
							while (entries.hasMoreElements()) {
								ZipEntry entry = entries.nextElement();
								if (entry.isDirectory())
									continue;

								Pattern p = Pattern.compile(".*toc.*\\.xml");
								Matcher m = p.matcher(entry.getName());
								if (!m.matches())
									continue;
								/*String url = "jar:file:/";
								url = url + new Path(f.getCanonicalPath()).toPortableString();
								url = url + "!/" + entry.getName();*/

								// jar:file:/c:/my%20sources/src.zip!/mypackage/toc.xml
								// jar entry url will lock the file after the
								// inputstream closed.
								// Here use JarFile to open the jar url
								// inputstream and close the
								// JarFile at last.
								TocContribution contribute = parse(new TocFile(
										fullPluginName, jar
												.getInputStream(entry), false,
										locale, null, siteName + "/"
												+ versionName + "/"
												+ pluginName));
								if (contribute != null) {
									Map<String, List<ITocContribution>> versionMap = siteVersionTocConMap
											.get(siteName + "&"
													+ siteDisplayName);
									if (versionMap == null) {
										versionMap = new HashMap<String, List<ITocContribution>>();

										siteVersionTocConMap.put(siteName + "&"
												+ siteDisplayName, versionMap);
									}
									List<ITocContribution> contributeListOfVersion = versionMap
											.get(versionName);
									if (contributeListOfVersion == null) {
										contributeListOfVersion = new ArrayList<ITocContribution>();
										versionMap.put(versionName,
												contributeListOfVersion);
									}

									RemoteContentLocator.addContentPage(
											fullPluginName, baseUrl);
									contribute.setId(fullPluginName);
									contribute.setContributorId(thisPluginId);
									contributeListOfVersion.add(contribute);

									result.add(contribute);
									hasTocContribute = true;
								}
							}

							if (hasTocContribute) {
								File unzippedDoc = new File(tmpUnzippedDocRoot,
										fullPluginName);
								unzippedDoc.mkdirs();
								FileUtil.unZipFile(f, null, unzippedDoc);
								docJarPaths.put(new Path(f.getAbsolutePath()),
										unzippedDoc);
							}
						} catch (IOException e) {
							// e.printStackTrace();
							Activator.getInstance().log(e);
						} finally {
							if (jar != null) {
								try {
									jar.close();
								} catch (IOException e) {
									Activator.getInstance().log(e);
								}
							}
						}
					}
				}
				return false;
			}
		};

		FileUtil.visitFiles(featuresRootFile, visitor);
		result.addAll(createVirtualPrimaryToc(locale));

		return result.toArray(new ITocContribution[result.size()]);
	}

	/**
	 * @return
	 */
	private Collection<ITocContribution> createVirtualPrimaryToc(String locale) {
		Set<ITocContribution> result = new HashSet<ITocContribution>();

		Iterator<Entry<String, Map<String, List<ITocContribution>>>> eit = siteVersionTocConMap
				.entrySet().iterator();
		while (eit.hasNext()) {
			Entry<String, Map<String, List<ITocContribution>>> siteConEntry = eit
					.next();
			String siteId = siteConEntry.getKey();
			String siteName = siteId.substring(0, siteId.indexOf('&'));
			String siteDiplayName = siteId.substring(siteId.indexOf('&') + 1);
			Map<String, List<ITocContribution>> versionMap = siteConEntry
					.getValue();

			// create a toc for site
			URL fileUrl = Activator.getInstance().getBundle().getEntry(
					"toc_template.xml");
			DocumentReader reader = new DocumentReader();
			try {
				InputStream in = fileUrl.openStream();
				if (in != null) {
					Toc toc = (Toc) reader.read(in);
					toc.setLabel(siteDiplayName);
					SiteToc siteToc = new SiteToc(toc, versionMap);
					TocContribution contribution = new TocContribution();
					contribution.setCategoryId(siteName);
					contribution.setContributorId(Activator.getInstance()
							.getBundleSymbleName());
					contribution.setId(FEATURE_SITE_TOC_CON_PREFIX + siteName);
					contribution.setLocale(locale);
					contribution.setToc(siteToc);
					contribution.setPrimary(true);

					RemoteContentLocator.addContentPage(
							FEATURE_SITE_TOC_CON_PREFIX + siteName, baseUrl);

					result.add(contribution);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return result;
	}

	@SuppressWarnings("restriction")
	private TocContribution parse(TocFile tocFile) {
		try {
			DocumentReader reader = new DocumentReader();
			InputStream in = tocFile.getInputStream();
			if (in != null) {
				Toc toc = (Toc) reader.read(in);
				in.close();
				TocContribution contribution = new TocContribution();

				// Wrap the parsed toc to modify the href path with some prefix.
				contribution.setToc(new TocWrapper(tocFile.getID(), toc));
				contribution.setCategoryId(tocFile.getCategory());
				// contribution.setContributorId(tocFile.getPluginId());
				contribution.setId(tocFile.getID());
				contribution.setLocale(tocFile.getLocale());
				contribution.setPrimary(tocFile.isPrimary());

				String[] resPaths = collectExtraDocuments(contribution, tocFile);
				contribution.setExtraDocuments(resPaths);
				return contribution;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param contribution
	 * @return
	 */
	private String[] collectExtraDocuments(TocContribution contribution,
			TocFile tocFile) {
		Set<String> result = new HashSet<String>();
		ITopic[] topics = contribution.getToc().getTopics();
		for (ITopic t : topics) {
			result.add(/*tocFile.getFile() + "/" + */t.getHref());
			findSubTopics(tocFile, t, result);
		}
		return result.toArray(new String[result.size()]);
	}

	/**
	 * @param tocFile
	 * @param t
	 * @param result
	 */
	private void findSubTopics(TocFile tocFile, ITopic parentTopic,
			Set<String> result) {
		ITopic[] topics = parentTopic.getSubtopics();
		if (topics != null) {
			for (ITopic t : topics) {
				result.add(/*tocFile.getFile() + "/" + */t.getHref());
				findSubTopics(tocFile, t, result);
			}
		}
	}
}
