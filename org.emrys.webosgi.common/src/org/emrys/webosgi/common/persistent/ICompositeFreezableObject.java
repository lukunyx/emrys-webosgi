package org.emrys.webosgi.common.persistent;

import java.util.List;

/**
 * 
 * @author Leo Chang
 * @version 2011-3-17
 */
public interface ICompositeFreezableObject extends IFreezableObject {
	List<IFreezableObject> getSubFreezableObjects();
}
