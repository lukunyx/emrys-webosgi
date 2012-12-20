/*******************************************************************************
 * Copyright (c) 2010 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.resources.client;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.emrys.common.util.FileUtil;
import org.emrys.core.runtime.resources.IWebResConstants;


/**
 * The file uploader class.
 * 
 * @author Leo Chang - EMRYS
 * @version 2010-10-18
 */
public class FileUploader {
	/**
	 * the upload task context.
	 */
	private final IUploadTaskContext context;

	public FileUploader(IUploadTaskContext context) {
		this.context = context;
	}

	/**
	 * Start upload to server.
	 * 
	 * @param monitor
	 * @return
	 * @throws Exception
	 */
	public boolean upload(IProgressMonitor monitor) throws Exception {
		boolean isLocalTarget = true;
		URL targetUrl = null;

		try {
			targetUrl = new URL(context.getTargetUrl());
			if (!targetUrl.getProtocol().equals("file"))
				isLocalTarget = false;
		} catch (Exception e1) {
			// e1.printStackTrace();
		}
		if (isLocalTarget) {
			IPath targetPath = new Path(context.getTargetUrl());
			monitor.beginTask("Move to local directory: " + targetPath.toPortableString(), 100);
			try {
				File target = targetPath.toFile();
				if (target != null) {
					if (!target.exists() || target.isFile()) {
						FileUtil.createFile(target, true);
					}

					copy2Local(target);
					return true;
				}
			} finally {
				monitor.done();
			}
			return false;
		}

		monitor.beginTask("Upload to web server: " + new URI(context.getTargetUrl()).toString(),
				100);
		PostMethod postMethod = new PostMethod(new URI(targetUrl.toString()).toString());
		try {
			List<File> files = context.getUploadFiles();
			List<Part> params = new ArrayList<Part>();
			for (File f : files) {
				// String Parameters first and then file part.
				List<NameValuePair> fParams = context.getParameters4File(f);
				if (fParams == null)
					continue;
				for (NameValuePair param : fParams) {
					StringPart stringPart = new StringPart(param.getName(), param.getValue());
					// support Chinese in name or value
					stringPart.setCharSet("GBK");
					params.add(stringPart);
				}

				FilePart filePart = new FilePart(f.getName(), f.getAbsolutePath(), f);
				// support Chinese in name
				filePart.setCharSet("GBK");
				params.add(filePart);
			}

			MultipartRequestEntity multReqEntity = new MultipartRequestEntity(params
					.toArray(new Part[params.size()]), postMethod.getParams());
			postMethod.setRequestEntity(multReqEntity);

			HttpClient client = new HttpClient();
			client.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
			int result = client.executeMethod(postMethod);
			if (result == HttpServletResponse.SC_OK) {
				return true;
			} else {
				String error = "File Upload Error HttpCode=" + result;
				throw new FileUploadException(error);
			}
		} catch (IOException e) {
			throw new FileUploadException("File Upload Error for:" + e.getMessage());
		} finally {
			postMethod.releaseConnection();
			monitor.done();
		}
	}

	/**
	 * If the target Server URL is just a file schema local url, just copy the uploding files to the
	 * target directory.
	 * 
	 * @throws Exception
	 */
	private void copy2Local(File targetRoot) throws Exception {
		List<File> files = context.getUploadFiles();
		for (File f : files) {
			if (!f.isFile())
				continue;
			String relativePath = null;
			List<NameValuePair> params = context.getParameters4File(f);
			for (NameValuePair pair : params) {
				if (pair.getName().equals(IWebResConstants.FILE_PARA_TARGET_RELATIVE_PATH))
					relativePath = pair.getValue();
			}

			IPath targetAbsPath = new Path(targetRoot.getAbsolutePath());
			if (relativePath != null)
				targetAbsPath = targetAbsPath.append(new Path(relativePath));
			else
				throw new Exception(
						"Resource \""
								+ f.getAbsolutePath()
								+ "\"'s relative path to target directory not found. This resource can not moved.");
			File targetFile = targetAbsPath.toFile();
			// FileUtil.createFile(targetFile, false);
			FileUtil.copyFile(f, targetFile);
		}
	}
}
