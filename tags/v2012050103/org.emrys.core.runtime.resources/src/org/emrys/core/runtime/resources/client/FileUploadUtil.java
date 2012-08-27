/*******************************************************************************
 * Copyright (c) 2010 EMRYS Corporation. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the EMRYS License v1.0 which accompanies this
 * distribution, and is available at http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.resources.client;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.httpclient.NameValuePair;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.emrys.common.util.FileUtil;
import org.emrys.core.runtime.resources.IWebResConstants;


/**
 * The file uplod Util methods.
 * 
 * @author Leo Chang - EMRYS
 * @version 2010-10-18
 */
public class FileUploadUtil {
	/**
	 * Upload files in context.
	 * 
	 * @param context
	 * @param monitor
	 * @throws Exception
	 */
	public static void uploadFile(IUploadTaskContext context, IProgressMonitor monitor)
			throws Exception {
		FileUploader uploader = new FileUploader(context);
		uploader.upload(monitor);
	}

	/**
	 * Upload given files.
	 * 
	 * @param files
	 * @param uploadType
	 * @param serverUrl
	 * @param keepFileStructure
	 * @param monitor
	 * @throws Exception
	 */
	public static void uploadFile(final List<File> files, String uploadType,
			final String serverUrl, boolean keepFileStructure, IProgressMonitor monitor)
			throws Exception {
		Map<String, String> globalAttrs = new HashMap<String, String>();
		globalAttrs.put(IWebResConstants.FILE_PARA_TEYP, uploadType);
		globalAttrs.put(IWebResConstants.FILE_PARA_KEEP_FILE_STRUCTURE, keepFileStructure ? "true"
				: "false");
		uploadFile(files, globalAttrs, serverUrl, monitor);
	}

	/**
	 * Upload files with global attributes.
	 * 
	 * @param files
	 * @param globalAttrs
	 * @param serverUrl
	 * @param monitor
	 * @throws Exception
	 */
	public static void uploadFile(final List<File> files, final Map<String, String> globalAttrs,
			final String serverUrl, IProgressMonitor monitor) throws Exception {
		if (files == null || files.size() == 0)
			return;

		IUploadTaskContext context = new IUploadTaskContext() {
			List<File> allFiles = new ArrayList<File>();

			public List<File> getUploadFiles() {
				if (allFiles.size() == 0) {
					for (File f : files) {
						if (f.isFile())
							allFiles.add(f);
						else
							collectSubFiles(f);
					}
				}
				return allFiles;
			}

			private void collectSubFiles(File f) {
				File[] fs = f.listFiles();
				for (File file : fs) {
					if (file.isFile())
						allFiles.add(file);
					else
						collectSubFiles(file);
				}
			}

			public String getTargetUrl() {
				return serverUrl;
			}

			public List<NameValuePair> getParameters4File(File file) {
				List<NameValuePair> result = new ArrayList<NameValuePair>();
				if (files.contains(file)) {
					result.add(new NameValuePair(IWebResConstants.FILE_PARA_CLIENT_ABSOLUTE_PATH,
							file.getAbsolutePath()));

				}
				result.add(new NameValuePair(IWebResConstants.FILE_PARA_TARGET_RELATIVE_PATH,
						getRelatedFilePath(file)));
				Iterator<Entry<String, String>> it = globalAttrs.entrySet().iterator();
				while (it.hasNext()) {
					Entry<String, String> entry = it.next();
					result.add(new NameValuePair(entry.getKey(), entry.getValue()));
				}
				return result;
			}

			private String getRelatedFilePath(File file) {
				if (files.contains(file)) {
					return file.getName();
				} else {
					File parentFile = file.getParentFile();
					while (!files.contains(parentFile)) {
						parentFile = parentFile.getParentFile();
					}

					if (parentFile != null) {
						IPath path = new Path(file.getAbsolutePath());
						IPath relPath = FileUtil.makeRelativeTo(path, new Path(parentFile
								.getAbsolutePath()));
						return new Path(getRelatedFilePath(parentFile)).append(relPath)
								.toPortableString();
					}
				}

				return serverUrl;
			}

			public int getConnTimeout() {
				return 5000;
			}

			public String getUploadResesType() {
				return globalAttrs.get(IWebResConstants.FILE_PARA_TEYP);
			}
		};

		uploadFile(context, monitor);
	}
}
