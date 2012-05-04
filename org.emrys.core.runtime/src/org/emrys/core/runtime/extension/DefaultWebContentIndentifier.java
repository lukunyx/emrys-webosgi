package org.emrys.core.runtime.extension;

import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.osgi.framework.Bundle;

/*******************************************************************************
 * Copyright (c) 2010 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/

/**
 * 
 * @author Leo Chang - EMRYS
 * @version 2010-10-13
 */
public class DefaultWebContentIndentifier {
	/**
	 * recgnize web content path.
	 * 
	 * @param bundle
	 * @param webContentBundlePath
	 * @return
	 */
	public boolean recgnize(Bundle bundle, String webContentBundlePath) {
		IPath path = new Path(webContentBundlePath).append(new Path("WEB-INF/web.xml"));
		URL[] es = FileLocator.findEntries(bundle, path);
		if (es.length == 0)
			return false;
		try {
			FileLocator.toFileURL(es[0]).openStream();
		} catch (IOException e) {
			// e.printStackTrace();
			return false;
		}

		return true;
	}
}
