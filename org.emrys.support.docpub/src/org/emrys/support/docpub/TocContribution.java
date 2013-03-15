/**
 * 
 */
package org.emrys.support.docpub;

import org.eclipse.help.IToc;
import org.eclipse.help.ITocContribution;

/**
 * @author LeoChang
 * 
 */
public class TocContribution implements ITocContribution {

	private boolean primary;
	private IToc toc;
	private String locale;
	private String linkTo;
	private String id;
	private String[] extraDocuments;
	private String contributorId;
	private String categoryId;

	public String getCategoryId() {
		return categoryId;
	}

	public String getContributorId() {
		return contributorId;
	}

	public String[] getExtraDocuments() {
		return extraDocuments;
	}

	public String getId() {
		return id;
	}

	public String getLinkTo() {
		return linkTo;
	}

	public String getLocale() {
		return locale;
	}

	public IToc getToc() {
		return toc;
	}

	public boolean isPrimary() {
		return primary;
	}

	public void setPrimary(boolean primary) {
		this.primary = primary;
	}

	public void setToc(IToc toc) {
		this.toc = toc;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public void setLinkTo(String linkTo) {
		this.linkTo = linkTo;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setExtraDocuments(String[] extraDocuments) {
		this.extraDocuments = extraDocuments;
	}

	public void setContributorId(String contributorId) {
		this.contributorId = contributorId;
	}

	public void setCategoryId(String categoryId) {
		this.categoryId = categoryId;
	}

}
