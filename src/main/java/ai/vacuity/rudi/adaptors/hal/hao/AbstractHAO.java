package ai.vacuity.rudi.adaptors.hal.hao;

import ai.vacuity.rudi.adaptors.bo.InputProtocol;
import ai.vacuity.rudi.adaptors.interfaces.IEvent;

public class AbstractHAO {
	static String call = null;
	static InputProtocol inputProtocol = null;
	static IEvent event = null;

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

	public IEvent getEvent() {
		return event;
	}

	public void setEvent(IEvent input) {
		this.event = input;
	}

	public void run() {

	}

}
