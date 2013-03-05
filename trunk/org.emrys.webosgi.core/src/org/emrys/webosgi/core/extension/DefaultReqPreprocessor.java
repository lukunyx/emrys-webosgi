package org.emrys.webosgi.core.extension;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * DefaultReqPreprocessor
 * 
 * @author Leo Chang
 * @version 2010-11-2
 */
public class DefaultReqPreprocessor implements IReqProcessor {
	public int process(ServletRequest request, ServletResponse response) {
		return RESULT_CONTINUE;
	}
}
