package ai.vacuity.rudi.adaptors.interfaces;

import ai.vacuity.rudi.adaptors.bo.p2p.Input;
import ai.vacuity.rudi.adaptors.bo.p2p.Topic;

public interface ITopicProvider {

	public Topic[] getTopics(Input input);

}