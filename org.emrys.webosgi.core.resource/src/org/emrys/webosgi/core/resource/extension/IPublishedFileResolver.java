package org.emrys.webosgi.core.resource.extension;

import java.io.File;

import javax.servlet.http.HttpServletRequest;

/**
 * The published resource file resolver.
 * 
 * @author Leo Chang
 * @version 2010-10-25
 */
public interface IPublishedFileResolver {
	/**
	 * Resolve and return the resource file.
	 * 
	 * @param req
	 *            the current servlet request.
	 * @param path
	 *            the path of resource to find.
	 * @param alias
	 *            the alias of resource.
	 * @param quick_ID
	 *            the quick visit id of this resource.
	 * @return
	 */
	File resolve(HttpServletRequest req, String path, String alias,
			String quick_ID);
}
