/*******************************************************************************
 * Copyright (c) 2010 Hirisun Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Hirisun License v1.0
 * which accompanies this distribution, and is available at
 * http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.resources.servlet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.emrys.core.runtime.resources.IWebResConstants;
import org.emrys.core.runtime.resources.ResroucesCom;
import org.emrys.core.runtime.resources.extension.FileRecieverException;
import org.emrys.core.runtime.resources.extension.IUploadFileReciever;
import org.emrys.core.runtime.resources.extension.ResUploadSVCRegister;
import org.emrys.core.runtime.resources.extension.ResUploadSVCRegister.ExtUploadReceiever;
import org.emrys.core.runtime.resources.util.FildRecieveUtil;

import com.oreilly.servlet.multipart.FilePart;
import com.oreilly.servlet.multipart.MultipartParser;
import com.oreilly.servlet.multipart.ParamPart;
import com.oreilly.servlet.multipart.Part;

/**
 * The Servlet providing the file or folder uploading service.
 * 
 * @author Leo Chang - Hirisun
 * @version 2010-6-1
 */
public class ResUploadSvcServlet extends HttpServlet implements IWebResConstants {
	private static final long serialVersionUID = 1L;

	@Override
	public void init() throws ServletException {
		FildRecieveUtil.clearTmpUploadedRoot();
		super.init();
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
			IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// Clear buffer files in temporary folder.
		FildRecieveUtil.clearTmpUploadedRoot();
		// processByApacheCommons(request, response);
		try {
			processByOreilly(request, response);
			return;
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.doPost(request, response);
	}

	/**
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	private void processByOreilly(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		ResroucesCom.getInstance().log("File upload request arrived:" + request, 0, true);
		// Create tmp folder named with timestamp.
		String timestamp = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
		File tmpDir = FildRecieveUtil.getTmpUploadedFileDir(timestamp, true);

		Map<File, Map<String, String>> fileParams = new HashMap<File, Map<String, String>>();
		Map<String, String> params = new HashMap<String, String>();

		// Add request parameters in params for each file.
		copyReqParams(request.getParameterMap(), params);

		// case user not use httpclient MultPart Method upload file, but use url and query
		// parameters( /ul?fcontent=xxx)
		try {
			// Max file size = 1GB
			MultipartParser mp = new MultipartParser(request, 1024 * 1024 * 1024, false, false,
					"gbk");
			System.out.println("The file is uploaded by httpclient multiple part method.");
			Part part;
			while ((part = mp.readNextPart()) != null) {
				String name = part.getName();
				if (part.isParam()) {
					ParamPart paramPart = (ParamPart) part;
					String value = paramPart.getStringValue();
					// System.out.println("param: name=" + name + "; value=" + value);
					params.put(name, value);
				} else if (part.isFile()) {
					// it's a file part
					FilePart filePart = (FilePart) part;
					String fileName = filePart.getFileName();
					String filePath = filePart.getFilePath();
					if (fileName != null) {
						boolean keepFileStru = "true".equals(params
								.get(IWebResConstants.FILE_PARA_KEEP_FILE_STRUCTURE));

						String relativePath = params
								.get(IWebResConstants.FILE_PARA_TARGET_RELATIVE_PATH);
						IPath newFilePath = null;
						if (keepFileStru && relativePath != null && relativePath.length() > 0) {
							newFilePath = new Path(tmpDir.getAbsolutePath()).append(relativePath);
						} else
							newFilePath = new Path(tmpDir.getAbsolutePath()).append(fileName);

						File newFile = newFilePath.toFile();
						createFile(newFile, false);
						long size = filePart.writeTo(new FileOutputStream(newFile));
						/*
						 * System.out.println("file: name=" + name + "; fileName=" + fileName
						 * + ", filePath=" + filePart.getFilePath() + ", targetPath= "
						 * + newFilePath.toPortableString() + "contentType="
						 * + filePart.getContentType() + ", size=" + size);
						 */

						if (newFile != null && newFile.exists()) {
							if (keepFileStru && relativePath != null && relativePath.length() > 0) {
								File rootDirOfRelPath = new Path(tmpDir.getAbsolutePath()).append(
										new Path(relativePath).segment(0)).toFile();
								recordUniqueFile(fileParams, rootDirOfRelPath, params);
							} else {
								recordUniqueFile(fileParams, newFile, params);
							}
						}

						params = new HashMap<String, String>();
						// Add request parameters in params for each file.
						copyReqParams(request.getParameterMap(), params);
					}
					System.out.flush();
				}
			}
		} catch (Exception e) {
			// e.printStackTrace();
			ResroucesCom.getInstance().log(e);
		}

		// Check if fcontent parameter exists, convert it to a temporary file.
		if (fileParams.size() == 0) {
			String fcontent = params.get(FILE_PARA_FCONTEN);
			if (fcontent != null) {
				params.remove(FILE_PARA_FCONTEN);
				File file = File.createTempFile("Tmp_Upload_Req_File", "tmp");
				fileParams.put(file, params);
			}
			System.out.println("The file is uploaded in Request parameter");
		}

		List<ExtUploadReceiever> recievers = ResUploadSVCRegister.getInstance()
				.getSortedRegisteredRecievers(true);
		boolean isRecived = false;
		try {
			for (ExtUploadReceiever er : recievers) {
				IUploadFileReciever reciever = er.getReciever();
				// Not use IUploadFileReceiver.interested() method to judge whether the reveiver
				// support the uploaded file.
				// In process method, if the receiver not support ,return false directly.
				if (/* reciever.interested(fileType)&& */reciever.process(response, fileParams)) {
					isRecived = true;
					ResroucesCom.getInstance().log("Recieved file:" + fileParams, 0, false);
					break;
				}
			}
		} catch (FileRecieverException e) {
			e.printStackTrace();
			response.sendError(e.getErrHttpCode(), "Upload File Error: " + e.getMessage()); //$NON-NLS-1$
		}

		if (isRecived) {
			response.setStatus(HttpServletResponse.SC_OK);
		} else {
			response
					.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED,
							"No reciever for the uploaded file, and the file was saved to temporary directory."); //$NON-NLS-1$
		}
	}

	/**
	 * @param parameterMap
	 * @param params
	 */
	private void copyReqParams(Map reqParameterMap, Map<String, String> params) {
		Iterator<Map.Entry<String, ?>> it = reqParameterMap.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, ?> entry = it.next();
			String str = "";
			Object value = entry.getValue();
			if (value.getClass().isArray()) {
				String[] objs = (String[]) (value);
				if (objs.length > 0) {

					for (String obj : objs)
						str = str + "," + obj;
					str = str.substring(1);
				}

			} else
				str = value.toString();

			params.put(entry.getKey(), str);
		}

	}

	/**
	 * @param fileParams
	 * @param rootDirOfRelPath
	 * @param params
	 */
	private void recordUniqueFile(Map<File, Map<String, String>> fileParams, File file,
			Map<String, String> params) {
		Iterator<File> it = fileParams.keySet().iterator();
		while (it.hasNext()) {
			File f = it.next();
			if (f.getAbsolutePath().equals(file.getAbsolutePath()))
				return;
		}
		fileParams.put(file, params);
	}

	private void createFile(File target, boolean isDirectory) throws IOException {
		if (!target.exists() || (isDirectory && target.isFile())
				|| (!isDirectory && target.isDirectory())) {
			if (isDirectory)
				target.mkdirs();
			else {
				createFile(target.getParentFile(), true);
				target.createNewFile();
			}
		}
	}

	/**
	 * @param request
	 * @param response
	 * @throws Exception
	 */
	private void processByApacheCommons(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		String timestamp = new SimpleDateFormat("yyyyymmdd").format(new Date());
		File tmpDir = FildRecieveUtil.getTmpUploadedFileDir(timestamp, true);
		Map<File, Map<String, String>> fileParams = new HashMap<File, Map<String, String>>();

		request.setCharacterEncoding("gbk");
		RequestContext requestContext = new ServletRequestContext(request);

		if (FileUpload.isMultipartContent(requestContext)) {
			DiskFileItemFactory factory = new DiskFileItemFactory();
			factory.setRepository(tmpDir);
			ServletFileUpload upload = new ServletFileUpload(factory);
			// upload.setHeaderEncoding("gbk");
			upload.setSizeMax(2000000);
			List<FileItem> items = new ArrayList<FileItem>();
			try {
				items = upload.parseRequest(request);
			} catch (FileUploadException e) {
				e.printStackTrace();
			}
			Map<String, String> params = new HashMap<String, String>();
			Iterator<FileItem> it = items.iterator();
			while (it.hasNext()) {
				FileItem fileItem = it.next();
				// If is parameter
				if (fileItem.isFormField()) {
					System.out.println(fileItem.getFieldName() + "    " + fileItem.getName()
							+ "    "
							+ new String(fileItem.getString().getBytes("iso8859-1"), "gbk"));
					params.put(fileItem.getFieldName() + "." + fileItem.getName(), new String(
							fileItem.getString().getBytes("iso8859-1"), "gbk"));
				} else {
					// Else file
					if (fileItem.getName() != null && fileItem.getSize() != 0) {
						String fileName = URLEncoder.encode(fileItem.getFieldName(), "gbk");
						File newFile = new File(tmpDir, fileName);
						fileItem.write(newFile);
						recordUniqueFile(fileParams, newFile, params);
						params = new HashMap<String, String>();
					} else {
						// System.out.println("No file uploaded.");
					}
				}
			}

			List<ExtUploadReceiever> recievers = ResUploadSVCRegister.getInstance()
					.getSortedRegisteredRecievers(true);
			for (ExtUploadReceiever er : recievers) {
				IUploadFileReciever reciever = er.getReciever();
				if (reciever.process(response, fileParams))
					break;
			}
		}
	}
}
