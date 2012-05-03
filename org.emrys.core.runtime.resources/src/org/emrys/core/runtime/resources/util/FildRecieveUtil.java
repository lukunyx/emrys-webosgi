/*******************************************************************************
 * Copyright (c) 2010 Hirisun Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Hirisun License v1.0
 * which accompanies this distribution, and is available at
 * http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.resources.util;

import java.io.File;

import org.eclipse.core.runtime.Path;
import org.emrys.common.util.FileUtil;
import org.emrys.core.runtime.resources.ResroucesCom;


/**
 * 
 * @author Leo Chang - Hirisun
 * @version 2010-10-25
 */
public class FildRecieveUtil {
	public static File getTmpUploadedFileDir(String subPath, boolean forceCreate) {
		File root = getTmpUploadedFileRoot();
		File file = new Path(root.getAbsolutePath()).append(new Path(subPath)).toFile();

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
		File file = ResroucesCom.getInstance().getStateLocation().append("upload").toFile();
		if (!file.exists()) {
			file.mkdirs();
		}
		return file;
	}
}
