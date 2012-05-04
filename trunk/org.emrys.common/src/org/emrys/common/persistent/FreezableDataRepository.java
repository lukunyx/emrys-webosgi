/*******************************************************************************
 * Copyright (c) 2011 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.common.persistent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author Leo Chang - EMRYS
 * @version 2011-3-17
 */
public class FreezableDataRepository implements IFreezableDataRepository {
	/**
	 * 方法说明＄1�7
	 * 
	 * @throws TagCoreException
	 */
	public void init() throws Exception {

	}

	/**
	 * 方法说明＄1�7
	 * 
	 * @param obj
	 *            obj
	 */
	public void freeze(IFreezableObject obj) {
		deepFreeze(obj.getSessionId(), obj.getFreezableDataStore(true));
		shallowFreeze(obj.getSessionId(), obj.getFreezableDataStore(false));

		if (obj instanceof ICompositeFreezableObject) {
			List<IFreezableObject> subObjs = ((ICompositeFreezableObject) obj)
					.getSubFreezableObjects();
			if (subObjs != null) {
				for (IFreezableObject o : subObjs) {
					freeze(o);
				}
			}
		}
	}

	/**
	 * @param obj
	 *            IFreezableObject
	 */
	public void unFreeze(IFreezableObject obj) {
		obj.setUnfrozenData(restoreShallowFrozenData(obj.getSessionId()), false);
		obj.setUnfrozenData(restoreDeepFrozenData(obj.getSessionId()), true);

		if (obj instanceof ICompositeFreezableObject) {
			List<IFreezableObject> subObjs = ((ICompositeFreezableObject) obj)
					.getSubFreezableObjects();
			if (subObjs != null) {
				for (IFreezableObject o : subObjs) {
					unFreeze(o);
				}
			}
		}
	}

	/**
	 * 恢复浅冷冻的数据
	 * 
	 * @param objectId
	 *            s
	 * @return map
	 */
	private Map<String, String> restoreShallowFrozenData(String objectId) {
		return new HashMap<String, String>();
	}

	/**
	 * 恢复深冷冻的数据
	 * 
	 * @param objectId
	 *            s
	 * @return map
	 */
	private Map<String, String> restoreDeepFrozenData(String objectId) {
		return new HashMap<String, String>();
	}

	/**
	 * 冷冻运行数据，在服务器关闭时回丢处1�7
	 * 
	 * @param objectId
	 *            string
	 * @param freezableData
	 *            map
	 */
	private void shallowFreeze(String objectId, Map<String, String> freezableData) {

	}

	/**
	 * 冷冻用户session状�1�7�信息，服务器关闭，重新启动后会自动恢复〄1�7
	 * 
	 * @param objectId
	 *            string
	 * @param freezableData
	 *            map
	 */
	private void deepFreeze(String objectId, Map<String, String> freezableData) {

	}
}
