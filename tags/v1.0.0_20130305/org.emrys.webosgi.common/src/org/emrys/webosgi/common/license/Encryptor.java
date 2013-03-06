package org.emrys.webosgi.common.license;

/**
 * The Encryptor interface
 * 
 * @author Leo Chang
 * @version 2011-6-14
 */
public interface Encryptor {
	/**
	 * Set the License type this encryptor for.
	 * 
	 * @param licenseType
	 */
	void setLicenseType(LicenseType licenseType);

	/**
	 * encrypt the given content.
	 * 
	 * @param source
	 *            the source content to encrypt
	 * 
	 * @return the result.
	 */
	String encrypt(String source) throws Exception;

	/**
	 * decrypt the given source content and return.
	 * 
	 * @param source
	 *            given source content to decrypted
	 * @return decrypted result.
	 */
	String decrypt(String source) throws Exception;
}
