/*******************************************************************************
 * Copyright (c) 2010 Hirisun Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Hirisun License v1.0
 * which accompanies this distribution, and is available at
 * http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.resources.client;

import java.io.File;
import java.util.List;

import org.apache.commons.httpclient.NameValuePair;

/**
 * The Upload Task Context to manipulate context data during the file uploading.
 * 
 * @author Leo Chang - Hirisun
 * @version 2010-10-18
 */
public interface IUploadTaskContext {
	/**
	 * @return the resource type to upload.
	 */
	String getUploadResesType();

	/**
	 * @return the target server's url.
	 */
	String getTargetUrl();

	/**
	 * @return the connection timeout in million second.
	 */
	int getConnTimeout();

	/**
	 * @return get uploading files.
	 */
	List<File> getUploadFiles();

	/**
	 * Get the parameters for a file to upload.
	 * 
	 * @param file
	 * @return
	 */
	List<NameValuePair> getParameters4File(File file);
}
