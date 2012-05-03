/*******************************************************************************
 * Copyright (c) 2010 Hirisun Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Hirisun License v1.0
 * which accompanies this distribution, and is available at
 * http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.resources;

/**
 * Constants using in FSSvc component.
 * 
 * @author Leo Chang
 * @version 2010-10-21
 */
public interface IWebResConstants {
	/**
	 * the file upload parameter name: -- indicating the file type of uploading.
	 */
	public static final String FILE_PARA_TEYP = "filetype";
	/**
	 * the file upload parameter name: -- indicating the file content of
	 * uploading.
	 */
	public static final String FILE_PARA_FCONTEN = "fcontent";
	/**
	 * the file upload parameter name: -- indicating the receiever should
	 * concern the file structure.
	 */
	public static final String FILE_PARA_KEEP_FILE_STRUCTURE = "file.upload.key.file.structure";
	/**
	 * the file upload parameter name: -- indicating the file path of user's
	 * client file system.
	 */
	public static final String FILE_PARA_CLIENT_ABSOLUTE_PATH = "file.client.path";
	/**
	 * the file upload parameter name: -- indicating the target file path.
	 */
	public static final String FILE_PARA_TARGET_RELATIVE_PATH = "file.target.path";
}
