package org.emrys.webosgi.core.resource.extension;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.RegistryFactory;
import org.emrys.webosgi.core.resource.ResroucesCom;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

/**
 * The file upload service extension point register.
 * 
 * @author Leo Chang
 * @version 2010-10-19
 */
public class ResUploadSVCRegister implements BundleListener {
	/**
	 * The Upload Receiver's wrapper to contain the extension data.
	 * 
	 * @author Leo Chang
	 * @version 2011-8-1
	 */
	public static class ExtUploadReceiever {
		String id;
		String name;
		IUploadFileReciever reciever;
		int priority;

		public ExtUploadReceiever(String id, String name,
				IUploadFileReciever reciever, int priority) {
			this.id = id;
			this.name = name;
			this.reciever = reciever;
			this.priority = priority;
		}

		public String getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public IUploadFileReciever getReciever() {
			return reciever;
		}

		public int getPriority() {
			return priority;
		}
	}

	public static final String RES_EXT_POINT_ID = ResroucesCom.getInstance()
			.getBundleSymbleName()
			+ ".resUploadService";
	private static ResUploadSVCRegister instance;
	private List<ExtUploadReceiever> recievers;
	private boolean internalUpdateMark = false;

	public static ResUploadSVCRegister getInstance() {
		if (instance == null)
			instance = new ResUploadSVCRegister();
		return instance;
	}

	protected ResUploadSVCRegister() {
		ResroucesCom.getInstance().getBundle().getBundleContext()
				.addBundleListener(this);
	}

	public List<ExtUploadReceiever> getSortedRegisteredRecievers(
			boolean forceUpdate) {
		if (internalUpdateMark || forceUpdate || recievers == null) {
			int maxPriority = -1;
			internalUpdateMark = false;
			if (recievers == null)
				recievers = new ArrayList<ExtUploadReceiever>();
			else
				recievers.clear();

			ArrayList<ExtUploadReceiever> tmpList = new ArrayList<ExtUploadReceiever>();
			IExtensionPoint extPoint = RegistryFactory.getRegistry()
					.getExtensionPoint(RES_EXT_POINT_ID);
			IConfigurationElement[] cfEles = extPoint
					.getConfigurationElements();
			for (IConfigurationElement ele : cfEles) {
				if (!ele.getName().equals("reciever"))
					continue;
				try {
					String id = ele.getAttribute("id");
					String name = ele.getAttribute("name");
					int priority = 0;
					try {
						priority = Integer.parseInt(ele
								.getAttribute("priority"));
					} catch (NumberFormatException e) {
					}
					IUploadFileReciever reciever = (IUploadFileReciever) ele
							.createExecutableExtension("class");
					ExtUploadReceiever repo = new ExtUploadReceiever(id, name,
							reciever, priority);
					maxPriority = maxPriority < priority ? priority
							: maxPriority;
					tmpList.add(repo);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			for (int i = maxPriority; i >= 0; i--) {
				for (ExtUploadReceiever r : tmpList) {
					if (i == r.priority) {
						recievers.add(r);
					}
				}
			}
		}

		return recievers;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seeorg.osgi.framework.BundleListener#bundleChanged(org.osgi.framework.
	 * BundleEvent)
	 */
	public void bundleChanged(BundleEvent event) {
		// If any bunlde stop, start event, force update internal buffer.
		internalUpdateMark = true;
	}
}
