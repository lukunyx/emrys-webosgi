/**
 * 
 */
package org.emrys.support.docpub;

import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.help.IToc;
import org.eclipse.help.ITopic;
import org.eclipse.help.IUAElement;
import org.eclipse.help.internal.toc.Toc;

/**
 * @author LeoChang
 * 
 */
public class TocWrapper extends Toc implements IToc {

	private final String pluginID;
	private final IToc toc;

	public TocWrapper(String pluginID, IToc toc) {
		super(toc);
		this.pluginID = pluginID;
		this.toc = toc;
	}

	@Override
	public IUAElement[] getChildren() {
		return toc.getChildren();
		/*IUAElement[] children = toc.getChildren();
		if (children == null || children.length == 0)
			return children;
		return wrapTopics(children);*/

	}

	@Override
	public String getHref() {
		return toc.getHref();
	}

	@Override
	public String getLabel() {
		return toc.getLabel();
	}

	@Override
	public ITopic getTopic(String href) {
		if (href != null && href.startsWith("/" + pluginID + "/"))
			href = href.replaceFirst("/" + pluginID, "");
		return wrapTopic(toc.getTopic(href));
	}

	private ITopic wrapTopic(ITopic topic) {
		if (topic == null || topic instanceof TopicWrapper)
			return topic;
		return new TopicWrapper(pluginID, topic);
	}

	@Override
	public ITopic[] getTopics() {
		ITopic[] topics = toc.getTopics();
		if (topics == null || topics.length == 0)
			return topics;
		return (ITopic[]) wrapTopics(toc.getTopics());
	}

	private IUAElement[] wrapTopics(IUAElement[] topics) {
		for (int i = 0; i < topics.length; i++)
			if (topics[i] instanceof ITopic)
				topics[i] = wrapTopic((ITopic) topics[i]);
		return topics;
	}

	@Override
	public boolean isEnabled(IEvaluationContext paramIEvaluationContext) {
		return toc.isEnabled(paramIEvaluationContext);
	}
}
