/**
 * 
 */
package org.emrys.webosgi.core.extension;

import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.emrys.webosgi.common.i18n.EMLS;
import org.emrys.webosgi.common.i18n.ExternalStrLocaleDecider;
import org.emrys.webosgi.core.FwkActivator;
import org.emrys.webosgi.core.runtime.OSGiWebContainer;


/**
 * The default web external string locale decider by judge the current http
 * request head.
 * 
 * @author LeoChang
 * 
 */
public class DefaultStrLocaleDecider implements ExternalStrLocaleDecider {

	public Locale getLocale(String scope) {
		OSGiWebContainer jeeContainer = FwkActivator.getInstance()
				.getJeeContainer();

		HttpServletRequest req = (HttpServletRequest) jeeContainer
				.getReqThreadVariants().get(OSGiWebContainer.THREAD_V_REQUEST);
		if (req != null) {
			String acceptLang = req.getHeader("Accept-Language");
			if (acceptLang != null && acceptLang.contains("zh"))
				return Locale.CHINA;
		}
		return Locale.US;
	}

	public String[] getSupportScopes() {
		return new String[] { EMLS.DEFAULT_SCOPE };
	}
}
