/*******************************************************************************
 * Copyright (c) 2010 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.resources.extension;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletResponse;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.emrys.common.util.FileUtil;
import org.emrys.core.runtime.resources.ResroucesCom;


/**
 * 
 * @author Leo Chang - EMRYS
 * @version 2010-10-19
 */
public class DefaultUploadFileReciever implements IUploadFileReciever {
	public boolean interested(String fileType) {
		return true;
	}

	public boolean process(HttpServletResponse response, Map<File, Map<String, String>> fileParams) {
		Iterator<Entry<File, Map<String, String>>> it = fileParams.entrySet().iterator();
		while (it.hasNext()) {
			Entry entry = it.next();
			File file = (File) entry.getKey();
			// Map<String, String> params = (Map<String, String>) entry.getValue();
			try {
				URL url = Platform.getInstanceLocation().getURL();
				IPath uploadDir = new Path(url.getPath()).append(ResroucesCom.getInstance()
						.getBundleSymbleName()
						+ "/" + "unknow_uploads");
				File root = uploadDir.toFile();
				if (!root.exists())
					root.mkdirs();

				if (file.isDirectory())
					FileUtil.copyDirectiory(file.getAbsolutePath(), root.getAbsolutePath() + "/"
							+ file.getName());
				else {
					File newFile = uploadDir.append(file.getName()).toFile();
					FileUtil.copyFile(file, newFile);
				}
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return false;
	}

}
