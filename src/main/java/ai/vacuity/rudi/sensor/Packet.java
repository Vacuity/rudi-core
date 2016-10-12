package ai.vacuity.rudi.sensor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.vacuity.rudi.adaptors.bo.p2p.Input;
import ai.vacuity.rudi.adaptors.bo.p2p.Response;
import ai.vacuity.rudi.adaptors.types.Transaction;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;

/**
 * An example message.
 * 
 * @author Jeff Hoye
 * @author In Lak'ech.
 */
public class Packet implements Message {
	private static final long serialVersionUID = 265675863194010712L;
	private static final double VERSION_1_0 = 1.0;

	private Id from;
	private Serializable to;

	private Input event;
	private Response response;
	private Integer priority = Message.LOW_PRIORITY;

	private double version = Packet.VERSION_1_0;
	private Serializable more;

	public Packet() {

	}

	public Packet(Id from, Serializable to) {
		this.from = from;
		this.to = to;
	}

	/**
	 * Use low priority to prevent interference with overlay maintenance traffic.
	 */
	public int getPriority() {
		return priority;
	}

	public Input getEvent() {
		return event;
	}

	public void setEvent(Input event) {
		this.event = event;
	}

	private Integer ttl;
	private List<Transaction> transactions = new ArrayList<Transaction>();
	private Map<String, Serializable> additionalProperties = new HashMap<String, Serializable>();

	public Integer getTtl() {
		return ttl;
	}

	public void setTtl(Integer ttl) {
		this.ttl = ttl;
	}

	public List<Transaction> getTransactions() {
		return transactions;
	}

	public void setTransactions(List<Transaction> transactions) {
		this.transactions = transactions;
	}

	public String toString() {
		// return ToStringBuilder.reflectionToString(this);
		return "From: " + this.getFrom() + "; To: " + this.getTo() + "; Message: " + this.getEvent().getLabel();
	}

	public Map<String, Serializable> getAdditionalProperties() {
		return this.additionalProperties;
	}

	public void setAdditionalProperty(String name, Serializable value) {
		this.additionalProperties.put(name, value);
	}

	public Id getFrom() {
		return from;
	}

	public void setFrom(Id from) {
		this.from = from;
	}

	public Serializable getTo() {
		return to;
	}

	public void setTo(Serializable to) {
		this.to = to;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	public Object getResponse() {
		return response;
	}

	public void setResponse(Response response) {
		this.response = response;
	}

	public double getVersion() {
		return this.version;
	}

	public void setVersion(double version) {
		this.version = version;
	}

	public Object getMore() {
		return more;
	}

	public void setMore(Serializable more) {
		this.more = more;
	}

}
