package ai.vacuity.rudi.adaptors.hal.hao;

import ai.vacuity.rudi.adaptors.bo.InputProtocol;

public class AbstractHAO {
	static String call = null;
	static InputProtocol inputProtocol = null;
	static String input = null;

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

	public String getInput() {
		return input;
	}

	public void setInput(String input) {
		this.input = input;
	}

	public void run() {

	}

}
