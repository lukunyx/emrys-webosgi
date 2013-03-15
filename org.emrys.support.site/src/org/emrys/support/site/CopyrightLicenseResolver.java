package org.emrys.support.site;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.emrys.webosgi.core.resource.extension.IPublishedFileResolver;

/**
 * 
 * @author Leo Chang
 * @version 2010-10-28
 */
public class CopyrightLicenseResolver implements IPublishedFileResolver {
	public File resolve(HttpServletRequest req, String path, String alias,
			String quickID) {
		try {
			String fileName = null;
			if ("/license.html".equals(quickID))
				fileName = "license.html";
			else if ("/copyright.html".equals(quickID))
				fileName = "epl-v10.html";

			if (fileName != null) {
				URL url = Activator.getInstance().getBundle()
						.getEntry(fileName);
				File file = new Path(FileLocator.toFileURL(url).getPath())
						.toFile();
				if (file != null && file.exists())
					return file;
			}
		} catch (IOException e) {
			// e.printStackTrace();
		}

		return null;
	}
}
