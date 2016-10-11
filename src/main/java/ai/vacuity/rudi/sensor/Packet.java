package ai.vacuity.rudi.sensor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import ai.vacuity.rudi.adaptors.bo.IndexableInput;
import ai.vacuity.rudi.adaptors.types.Transaction;
import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;
import rice.p2p.scribe.ScribeContent;

/**
 * An example message.
 * 
 * @author Jeff Hoye
 * @author In Lak'ech.
 */
public class Packet implements Message, ScribeContent {
	private static final long serialVersionUID = 265675863194010712L;

	Id from;
	Object to;

	IndexableInput event;
	Object response;
	Integer priority = Message.LOW_PRIORITY;

	public Packet() {

	}

	public Packet(Id from, Object to) {
		this.from = from;
		this.to = to;
	}

	/**
	 * Use low priority to prevent interference with overlay maintenance traffic.
	 */
	public int getPriority() {
		return priority;
	}

	public IndexableInput getEvent() {
		return event;
	}

	public void setEvent(IndexableInput event) {
		this.event = event;
	}

	private Integer ttl;
	private List<Transaction> transactions = new ArrayList<Transaction>();
	private Map<String, Object> additionalProperties = new HashMap<String, Object>();

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
		return ToStringBuilder.reflectionToString(this);
	}

	public Map<String, Object> getAdditionalProperties() {
		return this.additionalProperties;
	}

	public void setAdditionalProperty(String name, Object value) {
		this.additionalProperties.put(name, value);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(to).append(from).append(priority).append(ttl).append(transactions).append(additionalProperties).toHashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) { return true; }
		if ((other instanceof Packet) == false) { return false; }
		Packet rhs = ((Packet) other);
		return new EqualsBuilder().append(to, rhs.to).append(from, rhs.from).append(priority, rhs.priority).append(ttl, rhs.ttl).append(transactions, rhs.transactions).append(additionalProperties, rhs.additionalProperties).isEquals();
	}

	public Id getFrom() {
		return from;
	}

	public void setFrom(Id from) {
		this.from = from;
	}

	public Object getTo() {
		return to;
	}

	public void setTo(Object to) {
		this.to = to;
	}

	public void setPriority(Integer priority) {
		this.priority = priority;
	}

	public Object getResponse() {
		return response;
	}

	public void setResponse(Object response) {
		this.response = response;
	}

}