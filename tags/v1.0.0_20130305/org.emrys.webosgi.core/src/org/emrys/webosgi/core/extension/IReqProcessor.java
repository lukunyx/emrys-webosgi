package org.emrys.webosgi.core.extension;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * 
 * @author Leo Chang
 * @version 2010-11-2
 */
public interface IReqProcessor {
	public static final int RESULT_CONTINUE = 0;
	public static final int RESULT_OK = 1;
	public static final int RESULT_BREAK_OTHERS = 2;

	/**
	 * Process request or response of it before servlet or after. Here, user can
	 * redirect the url, modify http head, cookie,etc. before servlet. As well,
	 * process the response content is also possible.
	 * 
	 * @param request
	 * @param response
	 * @return
	 */
	int process(ServletRequest request, ServletResponse response);
}
