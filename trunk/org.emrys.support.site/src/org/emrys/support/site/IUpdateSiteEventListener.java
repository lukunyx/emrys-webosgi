package org.emrys.support.site;

/**
 * The listener to listen the Feature Update site event, such as update,
 * uploaded. etc.
 * 
 * @author Leo Chang
 * @version 2011-8-3
 */
public interface IUpdateSiteEventListener {
	/**
	 * A update site be updated
	 * 
	 * @param siteName
	 * @param version
	 */
	void siteUpdated(String siteName, String version);
}
