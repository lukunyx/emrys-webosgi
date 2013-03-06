package org.emrys.webosgi.core.resource;

import javax.servlet.http.HttpServletRequest;

import org.emrys.webosgi.core.resource.extension.DefaultResourceVisitController;
import org.emrys.webosgi.core.resource.extension.DefinesRoot;
import org.emrys.webosgi.core.resource.extension.IPublishedFileResolver;
import org.emrys.webosgi.core.resource.extension.ResFolder;
import org.emrys.webosgi.core.resource.extension.ResPublishSVCRegister;
import org.osgi.framework.Bundle;

/**
 * The File Service Core of Framework to provide the entrance method for File
 * Service.
 * 
 * @author Leo Chang
 * @version 2011-3-24
 */
public final class WebResCore {
	/**
	 * the singleton instance.
	 */
	private static WebResCore instance;
	/**
	 * the extension point register.
	 */
	private final ResPublishSVCRegister register;

	/**
	 * @return the singleton instance.
	 */
	public static WebResCore getInstance() {
		if (instance == null)
			instance = new WebResCore();
		return instance;
	}

	/**
	 * Default hidden constructor.
	 */
	protected WebResCore() {
		// initialize extension point register.
		register = ResPublishSVCRegister.getInstance();
		register.getVirtualRepositories(true);
	}

	/**
	 * register Web Content of a web bundle to publish.
	 * 
	 * @param bundle
	 * @param wabCtxPath
	 * @param rootPath
	 * @return
	 */
	public DefinesRoot registerWebContextRoot(Bundle bundle, String wabCtxPath,
			String rootPath) {
		DefinesRoot root = new DefinesRoot();
		ResFolder webContentFoler = new ResFolder();
		webContentFoler.setPath(wabCtxPath);
		webContentFoler.setAlias(wabCtxPath);
		IPublishedFileResolver resolver = new BundleWebContentFileResolver(
				bundle, rootPath);
		webContentFoler.setResolverID(resolver.getClass().getName());
		webContentFoler.setResolver(resolver);
		root.getResResolvers().put(resolver.getClass().getName(), resolver);
		root.getResources().add(webContentFoler);
		root.setSourceBundle(bundle);
		root.setName(wabCtxPath);
		root.setId(bundle.getSymbolicName());
		root.setVisitControler(new DefaultResourceVisitController() {
			@Override
			public boolean canRead(HttpServletRequest requestUrl) {
				return true;
			}

			@Override
			public boolean canModify(HttpServletRequest requestUrl) {
				return false;
			}

			@Override
			public boolean canBrowseFolder(HttpServletRequest req) {
				return false;
			}
		});
		register.getExtraRepositories().add(root);
		refreshRepository();
		return root;
	}

	/**
	 * Unregister the web content of the web bundle.
	 * 
	 * @param root
	 */
	public void unregisterWebContextRoot(DefinesRoot root) {
		register.getExtraRepositories().remove(root);
		refreshRepository();
	}

	/**
	 * Refresh extension point register of Published Resource.
	 */
	public void refreshRepository() {
		register.getVirtualRepositories(true);
	}
}
