package com.seomoz;

import ai.vacuity.rudi.adaptors.interfaces.IEvent;
import ai.vacuity.rudi.adaptors.interfaces.impl.AbstractResponseProcessor;

public class Context extends AbstractResponseProcessor {

	@Override
	public void process(String response, IEvent event) {
		super.process(response, event);
		this.response = this.response.replace("[{", "[{\"subject\":\"" + this.event.getLabel() + "\"},{");
	}

}
