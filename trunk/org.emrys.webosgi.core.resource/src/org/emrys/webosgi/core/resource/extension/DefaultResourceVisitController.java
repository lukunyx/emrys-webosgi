package org.emrys.webosgi.core.resource.extension;

import java.io.File;

import javax.servlet.http.HttpServletRequest;

/**
 * The default Resource Visit controller.
 * 
 * @author Leo Chang
 * @version 2010-10-11
 */
public class DefaultResourceVisitController implements IResourceVisitController {
	public boolean canBrowseFolder(HttpServletRequest request) {
		return true;
	}

	public boolean canModify(HttpServletRequest request) {
		return true;
	}

	public boolean canRead(HttpServletRequest request) {
		return true;
	}

	public long getLastModifiedTimeMillis(HttpServletRequest req, File localFile) {
		// return 0L;
		return localFile.lastModified();
	}
}
