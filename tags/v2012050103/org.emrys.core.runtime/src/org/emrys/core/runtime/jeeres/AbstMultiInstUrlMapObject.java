/*******************************************************************************
 * Copyright (c) 2011 EMRYS Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the EMRYS License v1.0
 * which accompanies this distribution, and is available at
 * http://www.EMRYS.com/legal/epl-v10.html
 *******************************************************************************/
package org.emrys.core.runtime.jeeres;

import java.util.ArrayList;
import java.util.List;

/**
 * The serlvet and filter's common interface which can be obtaining url pattern
 * to sort with or other process.
 * 
 * @author Leo Chang - EMRYS
 * @version 2011-7-2
 */
public class AbstMultiInstUrlMapObject extends AbstractBundledServletObject {
	public static String MULTI_MAP_SEG_SEPERATOR = ";";
	private String rawURLPatterns;
	private String[] patterns;

	public void setRawURLPatterns(String rawURLPatterns) {
		if (rawURLPatterns != null) {
			this.rawURLPatterns = rawURLPatterns;
			// According to the behavior of tomcat, the url perttern like /abc/* will match the path
			// like /abc?v=a. here add a extra url parttern to patterns array if like /xxx/* as
			// /xxx.
			String[] patterns = rawURLPatterns.split(MULTI_MAP_SEG_SEPERATOR);
			List<String> patternList = new ArrayList<String>();
			for (int i = 0; i < patterns.length; i++) {
				String pattern = patterns[i];
				patternList.add(pattern);
				if (pattern.endsWith("/*") && pattern.length() > 2) {
					patternList.add(pattern.substring(0, pattern.length() - 2));
				}
			}
			this.patterns = patternList.toArray(new String[patternList.size()]);
		}
	}

	public String getRawURLPatterns() {
		return rawURLPatterns;
	}

	public ClonedExecutableServletObject<AbstMultiInstUrlMapObject>[] cloneInstances() {
		String[] urlPatterns = getURLPatterns();
		if (urlPatterns != null && urlPatterns.length > 0) {
			List<ClonedExecutableServletObject<AbstMultiInstUrlMapObject>> result = new ArrayList<ClonedExecutableServletObject<AbstMultiInstUrlMapObject>>();
			for (int i = 0; i < urlPatterns.length; i++) {
				ClonedExecutableServletObject<AbstMultiInstUrlMapObject> instance = new ClonedExecutableServletObject<AbstMultiInstUrlMapObject>(
						i, this);
				result.add(instance);
			}

			return result.toArray(new ClonedExecutableServletObject[result.size()]);
		}

		return new ClonedExecutableServletObject[0];
	}

	public String[] getURLPatterns() {
		return patterns;
	}
}
