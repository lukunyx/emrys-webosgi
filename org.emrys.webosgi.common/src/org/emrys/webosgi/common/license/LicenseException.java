package org.emrys.webosgi.common.license;

/**
 * License Exception wrappring the Common Exception
 * 
 * @author Leo Chang
 * @version 2011-6-8
 */
public class LicenseException extends Exception {

	/**
	 * Constructor
	 */
	public LicenseException() {
		super();
	}

	/**
	 * @param message
	 * @param cause
	 */
	public LicenseException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 */
	public LicenseException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public LicenseException(Throwable cause) {
		super(cause);
	}
}
