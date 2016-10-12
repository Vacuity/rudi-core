package com.seomoz;

import ai.vacuity.rudi.adaptors.interfaces.IndexableEvent;
import ai.vacuity.rudi.adaptors.interfaces.impl.AbstractResponseModule;

public class Context extends AbstractResponseModule {

	@Override
	public void process(String response, IndexableEvent event) {
		super.process(response, event);
		this.response = this.response.replace("[{", "[{\"subject\":\"" + this.event.getLabel() + "\"},{");
	}

}
