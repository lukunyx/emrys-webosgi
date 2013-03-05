package org.emrys.webosgi.core.resource.util;

import java.io.File;

import org.eclipse.core.runtime.Path;
import org.emrys.webosgi.common.util.FileUtil;
import org.emrys.webosgi.core.resource.ResroucesCom;

/**
 * 
 * @author Leo Chang
 * @version 2010-10-25
 */
public class FildRecieveUtil {
	public static File getTmpUploadedFileDir(String subPath, boolean forceCreate) {
		File root = getTmpUploadedFileRoot();
		File file = new Path(root.getAbsolutePath()).append(new Path(subPath))
				.toFile();

		if (!file.exists() && forceCreate) {
			file.mkdirs();
		}

		return file;
	}

	public static void clearTmpUploadedRoot() {
		File root = getTmpUploadedFileRoot();
		// Clear temporary files.
		// root.deleteOnExit();
		FileUtil.deleteAllFile(root, null);
	}

	public static File getTmpUploadedFileRoot() {
		File file = ResroucesCom.getInstance().getStateLocation().append(
				"upload").toFile();
		if (!file.exists()) {
			file.mkdirs();
		}
		return file;
	}
}
