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
	 * 鏂规硶璇存槑锛�
	 * 
	 * @throws TagCoreException
	 */
	public void init() throws Exception {

	}

	/**
	 * 鏂规硶璇存槑锛�
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
	 * 鎭㈠娴呭喎鍐荤殑鏁版嵁
	 * 
	 * @param objectId
	 *            s
	 * @return map
	 */
	private Map<String, String> restoreShallowFrozenData(String objectId) {
		return new HashMap<String, String>();
	}

	/**
	 * 鎭㈠娣卞喎鍐荤殑鏁版嵁
	 * 
	 * @param objectId
	 *            s
	 * @return map
	 */
	private Map<String, String> restoreDeepFrozenData(String objectId) {
		return new HashMap<String, String>();
	}

	/**
	 * 鍐峰喕杩愯鏁版嵁锛屽湪鏈嶅姟鍣ㄥ叧闂椂鍥炰涪澶�
	 * 
	 * @param objectId
	 *            string
	 * @param freezableData
	 *            map
	 */
	private void shallowFreeze(String objectId, Map<String, String> freezableData) {

	}

	/**
	 * 鍐峰喕鐢ㄦ埛session鐘舵�佷俊鎭紝鏈嶅姟鍣ㄥ叧闂紝閲嶆柊鍚姩鍚庝細鑷姩鎭㈠銆�
	 * 
	 * @param objectId
	 *            string
	 * @param freezableData
	 *            map
	 */
	private void deepFreeze(String objectId, Map<String, String> freezableData) {

	}
}
