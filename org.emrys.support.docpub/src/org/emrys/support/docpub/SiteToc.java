package org.emrys.support.docpub;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.help.IToc;
import org.eclipse.help.ITocContribution;
import org.eclipse.help.ITopic;
import org.eclipse.help.IUAElement;
import org.eclipse.help.internal.UAElement;
import org.eclipse.help.internal.toc.Toc;

/**
 * 
 * @author Leo Chang
 * @version 2011-7-13
 */
public class SiteToc extends Toc implements IToc {
	private final Toc toc;
	private final Map<String, List<ITocContribution>> versionContributeMap;
	private List<ITopic> verisonTopics;

	public SiteToc(Toc toc,
			Map<String, List<ITocContribution>> versionContributeMap) {
		super(toc);
		this.versionContributeMap = versionContributeMap;
		this.toc = toc;
		getVersionTopics();
	}

	@Override
	public IUAElement[] getChildren() {
		List<IUAElement> r = new ArrayList<IUAElement>();
		if (super.getChildren() != null) {
			r.addAll(Arrays.asList(super.getChildren()));
			for (Iterator<IUAElement> it = r.iterator(); it.hasNext();) {
				IUAElement e = it.next();
				if (e instanceof ITopic
						&& "version".equals(((ITopic) e).getLabel())) {
					it.remove();
					break;
				}
			}
		}

		return r.toArray(new IUAElement[r.size()]);
	}

	@Override
	public String getHref() {
		return this.getTocContribution().getId();
	}

	@Override
	public String getLabel() {
		return toc.getLabel();
	}

	@Override
	public ITopic getTopic(String href) {
		return super.getTopic(href);
	}

	@Override
	public ITopic[] getTopics() {
		return super.getTopics();
		/*getVersionTopics();
		return verisonTopics.toArray(new ITopic[verisonTopics.size()]);*/
	}

	@Override
	public boolean isEnabled(IEvaluationContext context) {
		return true;
	}

	private List<ITopic> getVersionTopics() {
		if (verisonTopics == null) {
			verisonTopics = new ArrayList<ITopic>();
			Iterator<Entry<String, List<ITocContribution>>> eit = versionContributeMap
					.entrySet().iterator();
			while (eit.hasNext()) {
				Entry<String, List<ITocContribution>> entry = eit.next();
				this.getChildren();
				VersionTopic topic = new VersionTopic(this, entry.getKey(),
						entry.getValue());
				this.appendChild(topic);
				verisonTopics.add(topic);
			}
		}
		return verisonTopics;
	}

	@Override
	public void removeChild(UAElement elementToRemove) {
		// super.removeChild(elementToRemove);
	}
}
