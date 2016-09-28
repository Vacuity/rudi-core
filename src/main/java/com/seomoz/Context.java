package com.seomoz;

import ai.vacuity.rudi.adaptors.interfaces.impl.AbstractResponseProcessor;

public class Context extends AbstractResponseProcessor {

	@Override
	public void process(String response, String input) {
		super.process(response, input);
		this.response = this.response.replace("[{", "[{\"subject\":\"" + this.input + "\"},{");
	}

}
