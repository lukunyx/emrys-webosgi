package org.part3rd.dojo;

import org.emrys.core.runtime.resources.WebResComActivator;

public class Activator extends WebResComActivator {

	public static Activator Instance;

	public Activator() {
		Instance = this;
	}

	@Override
	public String getServiceNSPrefix() {
		return "dojolib";
	}
}
