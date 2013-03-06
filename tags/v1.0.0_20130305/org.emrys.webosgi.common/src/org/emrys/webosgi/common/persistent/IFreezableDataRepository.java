package org.emrys.webosgi.common.persistent;

/**
 * 
 * @author Leo Chang
 * @version 2011-3-17
 */
public interface IFreezableDataRepository {
	void freeze(IFreezableObject obj);

	void unFreeze(IFreezableObject obj);
}
