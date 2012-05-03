/*******************************************************************************
 * Copyright (c) 2011 Hirisun Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Hirisun License v1.0
 * which accompanies this distribution, and is available at
 * http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.jeeres;

import org.emrys.core.runtime.jeecontainer.IBundledServletContext;

/**
 * The abstract parent Class for all servlet object type.
 * 
 * @author Leo Chang - Hirisun
 * @version 2011-3-23
 */
public abstract class AbstractBundledServletObject {
	protected IBundledServletContext ctx;
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

	public IBundledServletContext getBundleContext() {
		return ctx;
	}

	public void setBundleContext(IBundledServletContext ctx) {
		this.ctx = ctx;
	}
}
