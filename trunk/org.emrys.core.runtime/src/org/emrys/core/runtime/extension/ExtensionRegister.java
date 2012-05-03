/*******************************************************************************
 * Copyright (c) 2010 Hirisun Corporation. All rights reserved. This program and the accompanying
 * materials are made available under the terms of the Hirisun License v1.0 which accompanies this
 * distribution, and is available at http://www.hirisun.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.extension;

import java.util.Vector;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.RegistryFactory;
import org.emrys.core.runtime.FwkActivator;


/**
 * The manager of Framework Extension point regitstraion.
 * 
 * @author Leo Chang - Hirisun
 * @version 2010-11-2
 */
public class ExtensionRegister {
	public static class ExtReqProcessor {
		String id;
		String name;

		/**
		 * @param id
		 * @param name
		 * @param preprocessor
		 * @param priority
		 */
		public ExtReqProcessor(String id, String name, IReqProcessor preprocessor, int priority) {
			super();
			this.id = id;
			this.name = name;
			this.preprocessor = preprocessor;
			this.priority = priority;
		}

		IReqProcessor preprocessor;
		int priority;

		public String getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public IReqProcessor getPreprocessor() {
			return preprocessor;
		}

		public int getPriority() {
			return priority;
		}
	}

	private Vector<ExtReqProcessor> reqPreprocessors;
	private static ExtensionRegister instance;

	protected ExtensionRegister() {

	}

	public Vector<ExtReqProcessor> getReqPreprocessors(boolean forceUpdate) {
		if (forceUpdate || reqPreprocessors == null) {
			reqPreprocessors = new Vector<ExtReqProcessor>();
			parseReqPreprocessors();
		}

		return reqPreprocessors;
	}

	private void parseReqPreprocessors() {
		IExtensionPoint extPoint = RegistryFactory.getRegistry().getExtensionPoint(
				FwkActivator.getInstance().getBundleSymbleName() + ".reqExtProcessor");
		IConfigurationElement[] ces = extPoint.getConfigurationElements();
		for (IConfigurationElement ce : ces) {
			if (!ce.getName().equals("processor"))
				continue;

			int priority = 0;
			try {
				priority = Integer.parseInt(ce.getAttribute("priority"));
			} catch (Exception e) {
			}

			try {
				String id = ce.getAttribute("id");
				String name = ce.getAttribute("name");
				IReqProcessor preprocessor;
				preprocessor = (IReqProcessor) ce.createExecutableExtension("class");
				ExtReqProcessor ext = new ExtReqProcessor(id, name, preprocessor, priority);

				// sort by priority.
				int i = 0;
				for (i = 0; i < reqPreprocessors.size(); i++) {
					ExtReqProcessor p = reqPreprocessors.get(i);
					if (priority <= p.getPriority())
						break;
				}
				reqPreprocessors.insertElementAt(ext, i);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}

	public static ExtensionRegister getInstance() {
		return instance;
	}
}
