package org.emrys.support.docpub;

import java.io.InputStream;
import java.util.Locale;

import org.eclipse.help.IHelpContentProducer;

/**
 * 
 */

/**
 * @author LeoChang
 * 
 */
public class DynaHelpContentProducer implements IHelpContentProducer {

	public InputStream getInputStream(String pluginID, String resPath,
			Locale locale) {
		// It seems that if TocProvider provided from ext point, this content
		// producer not works.
		// System.out.println("DynamicHelpRes:" + pluginID + " : " + resPath);
		return null;
	}
}
