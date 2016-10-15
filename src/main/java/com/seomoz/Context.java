package com.seomoz;

import org.eclipse.rdf4j.model.IRI;

import ai.vacuity.rudi.adaptors.bo.InputProtocol;
import ai.vacuity.rudi.adaptors.interfaces.IndexableEvent;
import ai.vacuity.rudi.adaptors.interfaces.impl.AbstractResponseModule;

public class Context extends AbstractResponseModule {

	@Override
	public void run(IRI responseIRI, InputProtocol inputProtocol, IndexableEvent event) {
	}

	@Override
	public void process(String response, InputProtocol inputProtocol, IndexableEvent event) {
		super.process(response, inputProtocol, event);
		this.response = this.response.replace("[{", "[{\"subject\":\"" + this.event.getLabel() + "\"},{");
	}
}
