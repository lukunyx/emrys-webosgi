package org.emrys.webosgi.core.resource.extension;

/**
 * 
 * @author Leo Chang
 * @version 2010-10-27
 */
public class FileRecieverException extends Exception {
	int errHttpCode = 700;

	public int getErrHttpCode() {
		return errHttpCode;
	}

	public void setErrHttpCode(int errHttpCode) {
		this.errHttpCode = errHttpCode;
	}
}
