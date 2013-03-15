package org.emrys.support.docpub;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.help.IToc;
import org.eclipse.help.ITocContribution;
import org.eclipse.help.ITopic;
import org.eclipse.help.IUAElement;
import org.eclipse.help.internal.Topic;
import org.eclipse.help.internal.toc.Toc;

/**
 * 
 * @author Leo Chang
 * @version 2011-7-14
 */
public class VersionTopic extends Topic implements ITopic {
	private final Collection<ITocContribution> docBundleContributes;
	private final Collection<TocWrapperTopic> tocTopics = new ArrayList<TocWrapperTopic>();
	private final String versionName;

	public VersionTopic(Toc parentToc, String versionName,
			Collection<ITocContribution> docBundleContributes) {
		super();
		this.versionName = versionName;
		this.docBundleContributes = docBundleContributes;

		// create sub toc's topic.
		for (Iterator<ITocContribution> it = docBundleContributes.iterator(); it
				.hasNext();) {
			ITocContribution con = it.next();
			IToc toc = con.getToc();
			if (toc instanceof Toc) {
				TocWrapperTopic topic = new TocWrapperTopic((Toc) toc);
				tocTopics.add(topic);
			}

		}
	}

	@Override
	public IUAElement[] getChildren() {
		return tocTopics.toArray(new IUAElement[tocTopics.size()]);
	}

	@Override
	public String getHref() {
		return null;
	}

	@Override
	public String getLabel() {
		return versionName;
	}

	@Override
	public ITopic[] getSubtopics() {
		return tocTopics.toArray(new ITopic[tocTopics.size()]);
	}

	@Override
	public boolean isEnabled(IEvaluationContext context) {
		return true;
	}

	private class TocWrapperTopic extends Topic implements ITopic {
		private final Toc toc;

		public TocWrapperTopic(Toc toc) {
			super(toc.getElement());
			this.toc = toc;
		}

		@Override
		public ITopic[] getSubtopics() {
			return SubBundleTopicWrapper.wrapper(toc.getTopics(), toc
					.getTocContribution());
		}

		@Override
		public IUAElement[] getChildren() {
			return toc.getChildren();
		}

		@Override
		public boolean isEnabled(IEvaluationContext context) {
			return true;
		}

		@Override
		public String getHref() {
			String href = toc.getHref();
			if (href != null
					&& href.startsWith("/"
							+ AutoFindTocProvider.FEATURE_SITE_TOC_CON_PREFIX)) {
				int index = href.indexOf('/');
				index = index + href.substring(index + 1).indexOf('/') + 1;
				return "/" + toc.getTocContribution().getContributorId()
						+ href.substring(index);
			}
			return href;
		}

		@Override
		public String getLabel() {
			return toc.getLabel();
		}

	}
}
