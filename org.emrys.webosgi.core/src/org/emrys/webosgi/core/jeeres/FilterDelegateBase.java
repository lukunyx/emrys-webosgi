package org.emrys.webosgi.core.jeeres;

import java.util.Hashtable;

import javax.servlet.Filter;

/**
 * 
 * @author Leo Chang
 * @version 2011-3-22
 */
public class FilterDelegateBase {

	public Filter filter;
	public Hashtable<String, String> parameters;
	public String name;
	public String clazzName;

}
