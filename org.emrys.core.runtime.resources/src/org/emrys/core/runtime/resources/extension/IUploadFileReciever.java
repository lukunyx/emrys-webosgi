/*******************************************************************************
 * Copyright (c) 2010 Hirisun Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Hirisun License v1.0
 * which accompanies this distribution, and is available at
 * http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.resources.extension;

import java.io.File;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

/**
 * The Reciever for uploaded files by File servcie component.
 * 
 * @author Leo Chang - Hirisun
 * @version 2010-10-19
 */
public interface IUploadFileReciever {
	/**
	 * Each Feature Update Site including features and bundles will be received and restored into a
	 * folder with name like ${site-name}/${version-obtained-from version.text file under site
	 * folder}/. Ant he existing version folder will be replaced fully. If not version.txt file
	 * found, the contents of received site folder will be stored in a folder named "current" and
	 * this will always replace the existing folder.Not use IUploadFileReceiver.interested() method
	 * to judge whether the receiver support the uploaded file. In process method, if the receiver
	 * not support ,return false directly.
	 * 
	 * @param response
	 * 
	 * @param params
	 * @param files
	 * @return processed, other receiver may need to process also.
	 */
	boolean process(HttpServletResponse response, Map<File, Map<String, String>> fileParams)
			throws FileRecieverException;

	/**
	 * whether this reciever is interesting with the upload file's type. If true, the File Service
	 * Component will invoke the process method.
	 * Not use IUploadFileReceiver.interested() method to judge whether the reveiver support the
	 * uploaded file. In process method, if the receiver not support ,return false directly.
	 * 
	 * @deprecated
	 * @param fileType
	 * @return
	 */
	@Deprecated
	boolean interested(String fileType);
}
