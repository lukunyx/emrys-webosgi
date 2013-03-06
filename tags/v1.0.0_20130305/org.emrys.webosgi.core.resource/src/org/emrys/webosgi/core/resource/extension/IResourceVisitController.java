package org.emrys.webosgi.core.resource.extension;

import java.io.File;

import javax.servlet.http.HttpServletRequest;

/**
 * The resource visite authority controller.
 * 
 * @author Leo Chang
 * @version 2010-10-11
 */
public interface IResourceVisitController {
	/**
	 * whether the current client user has authority to read the resource.
	 * 
	 * @param requestUrl
	 * @return
	 */
	boolean canRead(HttpServletRequest requestUrl);

	/**
	 * whether the current client user has authority to modify the resource.
	 * 
	 * @param requestUrl
	 * @return
	 */
	boolean canModify(HttpServletRequest requestUrl);

	/**
	 * Whether the user has authority to browse the folder. If no, the response
	 * will return 404 http error code, instead of return the filder's
	 * information.
	 * 
	 * @param req
	 * @return
	 */
	boolean canBrowseFolder(HttpServletRequest req);

	/**
	 * Last-Modified-Time by which to judge wheter the sub resource should be
	 * not update to http client. If return zero, indicate the time framework
	 * started.
	 * 
	 * @param localFile
	 * @param req
	 * 
	 * @return the last modified time in millis.
	 */
	long getLastModifiedTimeMillis(HttpServletRequest req, File localFile);
}
