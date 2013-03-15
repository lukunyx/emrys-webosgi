package org.emrys.support.docpub;

import java.io.IOException;
import java.io.InputStream;

/*
 * A TocFile represents an XML toc fileURL contributed via the toc extension point.
 */
public class TocFile {

	private final String id;
	private final InputStream fileURL;
	private final boolean isPrimary;
	private final String locale;
	private final String extraDir;
	private final String category;

	public TocFile(String id, InputStream fileUrl, boolean isPrimary,
			String locale, String extradir, String category) {
		this.id = id;
		this.fileURL = fileUrl;
		this.isPrimary = isPrimary;
		this.locale = locale;
		this.extraDir = extradir;
		this.category = category;
	}

	public String getCategory() {
		return category;
	}

	public String getExtraDir() {
		return extraDir;
	}

	public InputStream getInputStream() throws IOException {
		return fileURL;
	}

	public String getLocale() {
		return locale;
	}

	public String getID() {
		return id;
	}

	public boolean isPrimary() {
		return isPrimary;
	}
}
