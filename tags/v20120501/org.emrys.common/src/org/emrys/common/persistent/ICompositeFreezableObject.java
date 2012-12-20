/*******************************************************************************
 * Copyright (c) 2011 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.common.persistent;

import java.util.List;

/**
 * 
 * @author Leo Chang - EMRYS
 * @version 2011-3-17
 */
public interface ICompositeFreezableObject extends IFreezableObject {
	List<IFreezableObject> getSubFreezableObjects();
}
