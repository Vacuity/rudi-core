package ai.vacuity.rudi.adaptors.interfaces.impl;

import ai.vacuity.rudi.adaptors.bo.p2p.Input;
import ai.vacuity.rudi.adaptors.bo.p2p.Topic;
import ai.vacuity.rudi.adaptors.interfaces.ITopicProvider;

public class DefaultTopicProvider implements ITopicProvider {

	/*
	 * (non-Javadoc)
	 * 
	 * @see ai.vacuity.rudi.adaptors.interfaces.impl.TopicProvider#getTopics(ai.vacuity.rudi.adaptors.bo.p2p.Input)
	 */
	@Override
	public Topic[] getTopics(Input input) {
		return new Topic[0];
	}
}
