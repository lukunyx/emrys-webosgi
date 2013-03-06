package org.emrys.webosgi.common.i18n;

import java.util.Locale;

/**
 * The decider for the Locale to resolve the external String from String Define
 * files of multiple languages. Use international external String scope, we can
 * define several scope the different Locales can be use, like the back
 * administrat page or front ui page of a web application.
 * 
 * @author Leo Chang
 * @version 2011-6-24
 */
public interface ExternalStrLocaleDecider {
	/**
	 * Provide the propriety Locale. If not interest with the this, return null
	 * let following decider to process. If not any Local decided at last, the
	 * system locale will be applied.
	 * 
	 * @param scope
	 *            this external string locale decider's effect scope. If this
	 *            decider not support, return null.
	 * 
	 * @return If this decider not support the given scope, return null.
	 */
	Locale getLocale(String scope);

	/**
	 * Defines the supported scopes. A scope is a normal String representing a
	 * scope which this decider's effect.
	 * 
	 * @return
	 */
	String[] getSupportScopes();
}
