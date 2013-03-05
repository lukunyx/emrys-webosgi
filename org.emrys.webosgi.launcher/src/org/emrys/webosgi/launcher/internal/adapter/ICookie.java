package org.emrys.webosgi.launcher.internal.adapter;

/**
 * This interface exists form framework to adapt some Cookie from Framework.
 * 
 * @author Leo Chang
 * @version 2011-6-14
 */
public interface ICookie {
	String getComment();

	String getDomain();

	int getMaxAge();

	String getName();

	String getPath();

	boolean getSecure();

	String getValue();

	int getVersion();

	void setComment(String purpose);

	void setDomain(String pattern);

	void setMaxAge(int expiry);

	void setPath(String uri);

	void setSecure(boolean flag);

	void setValue(String newValue);

	void setVersion(int v);

}
