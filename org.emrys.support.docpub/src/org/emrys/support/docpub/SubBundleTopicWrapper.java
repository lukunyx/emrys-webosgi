package org.emrys.support.docpub;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.help.ITocContribution;
import org.eclipse.help.ITopic;
import org.eclipse.help.IUAElement;

/**
 * 
 * @author Leo Chang
 * @version 2011-7-15
 */
public class SubBundleTopicWrapper implements ITopic {
	public static ITopic[] wrapper(ITopic[] topics,
			ITocContribution bundleTocCon) {
		List<ITopic> result = new ArrayList<ITopic>();
		if (topics != null)
			for (ITopic t : topics) {
				result.add(new SubBundleTopicWrapper(t, bundleTocCon));
			}
		return result.toArray(new ITopic[result.size()]);
	}

	private final ITopic topic;
	private final ITocContribution bundleTocCon;

	SubBundleTopicWrapper(ITopic topic, ITocContribution bundleTocCon) {
		this.topic = topic;
		this.bundleTocCon = bundleTocCon;
	}

	public IUAElement[] getChildren() {
		return topic.getChildren();
	}

	public String getHref() {
		String href = topic.getHref();
		if (href != null
				&& href.startsWith("/"
						+ AutoFindTocProvider.FEATURE_SITE_TOC_CON_PREFIX)) {
			int index = href.indexOf('/');
			index = index + href.substring(index + 1).indexOf('/') + 1;
			return "/" + bundleTocCon.getContributorId()
					+ href.substring(index);
		}

		return href;
	}

	public String getLabel() {
		return topic.getLabel();
	}

	public ITopic[] getSubtopics() {
		return wrapper(topic.getSubtopics(), bundleTocCon);
	}

	public boolean isEnabled(IEvaluationContext context) {
		return topic.isEnabled(context);
	}
}
