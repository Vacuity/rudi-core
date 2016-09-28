package ai.vacuity.rudi.adaptors.interfaces.impl;

import ai.vacuity.rudi.adaptors.interfaces.ResponseProcessor;

public class AbstractResponseProcessor implements ResponseProcessor {

	protected String response;
	protected String input;

	@Override
	public void process(String response, String input) {
		this.response = response;
		this.input = input;
	}

	@Override
	public String getResponse() {
		return this.response;
	}

	@Override
	public String getInput() {
		return this.input;
	}

}
