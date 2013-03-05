package org.emrys.webosgi.core.jeeres;

import org.emrys.webosgi.core.service.IWABServletContext;

/**
 * The abstract parent Class for all servlet object type.
 * 
 * @author Leo Chang
 * @version 2011-3-23
 */
public abstract class AbstractBundledServletObject {
	protected IWABServletContext ctx;
	protected boolean initialized = false;

	/**
	 * A convenient method to judge whether this delegate and a give object is
	 * bundled the same bundle context.
	 * 
	 * @param obj
	 * @return
	 */
	public boolean isSameBundled(AbstractBundledServletObject obj) {
		return this.getBundleContext().equals(obj.getBundleContext());
	}

	public boolean isInitialized() {
		return initialized;
	}

	protected void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}

	public IWABServletContext getBundleContext() {
		return ctx;
	}

	public void setBundleContext(IWABServletContext ctx) {
		this.ctx = ctx;
	}
}
