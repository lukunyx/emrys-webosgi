package org.emrys.core.runtime.util;

import org.eclipse.core.runtime.Assert;

/**
 * {@link ThreadLocal} subclass that exposes a specified name
 * as {@link #toString()} result (allowing for introspection).
 * 
 * @author Leo Chang - Hirisun
 * @version 2011-6-2
 * @param <T>
 */
public class NamedThreadLocal<T> extends ThreadLocal<T> {
	private final String name;

	/**
	 * Create a new NamedThreadLocal with the given name.
	 * 
	 * @param name
	 *            a descriptive name for this ThreadLocal
	 */
	public NamedThreadLocal(String name) {
		Assert.isLegal(name != null && name.length() > 0,
				"The name of this thread local varaint cann't be empty.");
		this.name = name;
	}

	@Override
	public String toString() {
		return this.name;
	}
}
