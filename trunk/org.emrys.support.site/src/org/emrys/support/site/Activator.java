package org.emrys.support.site;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.emrys.webosgi.core.ServiceInitException;
import org.emrys.webosgi.core.WebComActivator;
import org.emrys.webosgi.core.resource.extension.DefinesRoot;
import org.emrys.webosgi.core.resource.extension.ResPublishSVCRegister;
import org.osgi.framework.BundleContext;

public class Activator extends WebComActivator {

	private static Activator instance;

	public static Activator getInstance() {
		return instance;
	}

	@Override
	public String getServiceNSPrefix() {
		return "features";
	}

	@Override
	protected void startComponent(BundleContext context) {
		instance = this;
		super.startComponent(context);
	}

	@Override
	public void startWebService(BundleContext context)
			throws ServiceInitException {
		instance = this;
		super.startWebService(context);
		ResPublishSVCRegister register = ResPublishSVCRegister.getInstance();
		List<DefinesRoot> repos = register.getVirtualRepositories(true);
	}

	public IPath getFeaUpdateSitesRoot() {
		URL url = Platform.getInstanceLocation().getURL();
		IPath updateSitesRoot = new Path(url.getPath()).append(Activator
				.getInstance().getBundleSymbleName()
				+ "/" + "update_sites");
		File root = updateSitesRoot.toFile();
		if (!root.exists())
			root.mkdirs();
		return updateSitesRoot;
	}
}
