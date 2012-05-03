/*******************************************************************************
 * Copyright (c) 2011 Hirisun Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Hirisun License v1.0
 * which accompanies this distribution, and is available at
 * http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.common.persistent;

/**
 * 
 * @author Leo Chang - Hirisun
 * @version 2011-3-17
 */
public interface IFreezableDataRepository {
	void freeze(IFreezableObject obj);

	void unFreeze(IFreezableObject obj);
}
