/**
 * 
 */
package org.emrys.support.docpub;

import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.help.ITopic;
import org.eclipse.help.IUAElement;

/**
 * @author LeoChang
 * 
 */
public class TopicWrapper implements ITopic {

	private final ITopic topic;
	private final String pluginID;

	public TopicWrapper(String pluginID, ITopic topic) {
		this.pluginID = pluginID;
		this.topic = topic;
	}

	public IUAElement[] getChildren() {
		IUAElement[] children = topic.getChildren();
		if (children == null || children.length == 0)
			return children;
		return wrapTopics(children);
	}

	public String getHref() {
		// The topic href will be "/" + contributor's id + the real path.
		// So we let the contributor's id be resource index str.
		// the plugind id be the real plugin id of this bundle.
		String href = topic.getHref();
		if (href == null)
			return null;

		String thisPluginIDPrefix = "/"
				+ Activator.getInstance().getBundleSymbleName() + "/";
		if (href.startsWith(thisPluginIDPrefix))
			href = href.replace(thisPluginIDPrefix, thisPluginIDPrefix
					+ pluginID + "/");
		return href;
	}

	public String getLabel() {
		return topic.getLabel();
	}

	public ITopic[] getSubtopics() {
		ITopic[] topics = topic.getSubtopics();
		if (topics == null || topics.length == 0)
			return topics;
		return (ITopic[]) wrapTopics(topics);
	}

	private ITopic wrapTopic(ITopic topic) {
		if (topic == null || topic instanceof TopicWrapper)
			return topic;
		return new TopicWrapper(pluginID, topic);
	}

	private IUAElement[] wrapTopics(IUAElement[] topics) {
		for (int i = 0; i < topics.length; i++)
			if (topics[i] instanceof ITopic)
				topics[i] = wrapTopic((ITopic) topics[i]);
		return topics;
	}

	public boolean isEnabled(IEvaluationContext paramIEvaluationContext) {
		return topic.isEnabled(paramIEvaluationContext);
	}
}
