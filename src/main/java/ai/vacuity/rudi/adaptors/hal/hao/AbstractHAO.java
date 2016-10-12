package ai.vacuity.rudi.adaptors.hal.hao;

import ai.vacuity.rudi.adaptors.bo.InputProtocol;
import ai.vacuity.rudi.adaptors.interfaces.IndexableEvent;

public class AbstractHAO {
	protected String call = null;
	protected InputProtocol inputProtocol = null;
	protected IndexableEvent event = null;

	public String getCall() {
		return call;
	}

	public void setCall(String call) {
		this.call = call;
	}

	public InputProtocol getInputProtocol() {
		return inputProtocol;
	}

	public void setInputProtocol(InputProtocol inputProtocol) {
		this.inputProtocol = inputProtocol;
	}

	public IndexableEvent getEvent() {
		return event;
	}

	public void setEvent(IndexableEvent input) {
		this.event = input;
	}

	public void run() {

	}

}
