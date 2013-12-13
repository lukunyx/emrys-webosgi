package org.emrys.webosgi.launcher.osgi;

import com.ibm.designer.runtime.domino.adapter.HttpService;
import com.ibm.designer.runtime.domino.adapter.IServiceFactory;
import com.ibm.designer.runtime.domino.adapter.LCDEnvironment;

/**
 * Define a empty Xpages Service Factory just to active this bundle for IBM
 * Domino server will load this factory when it start. Do it so, the WebOSGi web
 * container can be started automatically if need.
 * 
 * @author LeoChang
 * 
 */
public class XspSvcEmptyFactory implements IServiceFactory {
	public HttpService[] getServices(LCDEnvironment lcdEnv) {
		return new HttpService[0];
	}
}