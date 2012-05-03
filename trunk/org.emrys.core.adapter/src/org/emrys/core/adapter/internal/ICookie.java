/*******************************************************************************
 * Copyright (c) 2011 Hirisun Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Hirisun License v1.0
 * which accompanies this distribution, and is available at
 * http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.adapter.internal;

/**
 * This interface exists form framework to adapt some Cookie from Framework.
 * 
 * @author Leo Chang - Hirisun
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
