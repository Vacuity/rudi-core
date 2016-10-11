package ai.vacuity.rudi.sensor;

import java.util.Collection;

import org.slf4j.LoggerFactory;

import rice.p2p.commonapi.Application;
import rice.p2p.commonapi.Endpoint;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.commonapi.Node;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.commonapi.RouteMessage;
import rice.p2p.scribe.Scribe;
import rice.p2p.scribe.ScribeContent;
import rice.p2p.scribe.ScribeImpl;
import rice.p2p.scribe.ScribeMultiClient;
import rice.p2p.scribe.Topic;
import rice.pastry.commonapi.PastryIdFactory;

/**
 * Listens to packets from the network using the Socket Protocol.
 * 
 * @author Jeff Hoye
 * @author In Lak'ech.
 */
public class OverlaySensor implements ScribeMultiClient, Application {
	public final static org.slf4j.Logger logger = LoggerFactory.getLogger(OverlaySensor.class);

	public static final String INSTANCE_ID_OVERLAY = "DEFAULT-INSTANCE-OVERLAY";
	public static final String INSTANCE_ID_SCRIBE = "DEFAULT-INSTANCE-SCRIBE";
	public static final String TOPIC_EVENTS = "P2P-TOPIC-EVENTS";

	/**
	 * The Endpoint represents the underlying node. By making calls on the Endpoint, it assures that the message will be delivered to a MyApp on whichever node the message is intended for.
	 */
	protected Endpoint endpoint;
	protected Scribe scribe;
	protected Topic topic;

	public OverlaySensor(Node node) {
		// We are only going to use one instance of this application on each PastryNode
		this.endpoint = node.buildEndpoint(this, OverlaySensor.INSTANCE_ID_OVERLAY);

		// construct Scribe
		scribe = new ScribeImpl(node, OverlaySensor.INSTANCE_ID_SCRIBE);

		// construct the topic
		topic = new Topic(new PastryIdFactory(node.getEnvironment()), OverlaySensor.TOPIC_EVENTS);
		logger.debug("Topic = " + topic);

		// now we can receive messages
		this.endpoint.register();
	}

	public void subscribe() {
		getScribe().subscribe(getTopic(), this, new Packet(), null);
	}

	public void broadcast(Packet pkt) {
		getScribe().publish(getTopic(), pkt);
	}

	public void send(Packet pkt) {
		pkt.setFrom(this.endpoint.getId());
		if (pkt.getTo() instanceof Id) {
			logger.debug(this + " sending to " + pkt.getTo());
			endpoint.route((Id) pkt.getTo(), pkt, null);
		}
		if (pkt.getTo() instanceof NodeHandle) {
			logger.debug(this + " sending directly to " + pkt.getTo());
			endpoint.route(((NodeHandle) pkt.getTo()).getId(), pkt, null);
		}
	}

	/**
	 * Called when we receive a message.
	 */
	public void deliver(Id id, Message message) {
		if (message instanceof Packet) {
			Packet pkt = (Packet) message;
			logger.debug(this + " packet received " + pkt.getEvent());
		}
	}

	public void update(NodeHandle handle, boolean joined) {
	}

	public boolean forward(RouteMessage message) {
		return true;
	}

	public String toString() {
		return this.getClass().getName() + ": " + endpoint.getId();
	}

	public Scribe getScribe() {
		return scribe;
	}

	public void setScribe(Scribe scribe) {
		this.scribe = scribe;
	}

	public Topic getTopic() {
		return topic;
	}

	public void setTopic(Topic topic) {
		this.topic = topic;
	}

	@Override
	public boolean anycast(Topic arg0, ScribeContent arg1) {
		return false;
	}

	@Override
	public void childAdded(Topic arg0, NodeHandle arg1) {

	}

	@Override
	public void childRemoved(Topic arg0, NodeHandle arg1) {

	}

	@Override
	public void deliver(Topic arg0, ScribeContent arg1) {

	}

	@Override
	public void subscribeFailed(Topic arg0) {

	}

	@Override
	public void subscribeFailed(Collection<Topic> arg0) {

	}

	@Override
	public void subscribeSuccess(Collection<Topic> arg0) {

	}

}
