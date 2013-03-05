package org.emrys.webosgi.core.extension;

/**
 * 
 * @author Leo Chang
 * @version 2010-11-2
 */
public interface IEarlyStarter {
	public static final int WHEN_AFTER_FWK_STARTED = 0;
	public static final int WHEN_BEFORE_FWK_START = 1;
	public static final int WHEN_BEFORE_JEE_START = 2;
	public static final int WHEN_AFTER_JEE_STARTED = 3;

	/**
	 * Start Extra process after the whole OSGi initialized, started and
	 * workspace opened.
	 */
	void start();
}
