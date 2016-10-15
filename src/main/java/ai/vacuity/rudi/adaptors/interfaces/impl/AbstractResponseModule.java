package ai.vacuity.rudi.adaptors.interfaces.impl;

import org.eclipse.rdf4j.model.IRI;

import ai.vacuity.rudi.adaptors.bo.InputProtocol;
import ai.vacuity.rudi.adaptors.interfaces.IResponseModule;
import ai.vacuity.rudi.adaptors.interfaces.IndexableEvent;

/**
 * Performs transformations on the response and/or input value.
 * 
 * @author In Lak'ech.
 *
 */
public abstract class AbstractResponseModule implements IResponseModule {

	protected String response;
	protected IRI responseIRI;
	protected IndexableEvent event;
	protected InputProtocol inputProtocol;

	@Override
	public void process(String response, InputProtocol inputProtocol, IndexableEvent event) {
		this.response = response;
		this.event = event;
		this.inputProtocol = inputProtocol;
	}

	@Override
	public void run(IRI responseIRI, InputProtocol inputProtocol, IndexableEvent event) {
		this.responseIRI = responseIRI;
		this.event = event;
		this.inputProtocol = inputProtocol;
	}

	@Override
	public String getResponse() {
		return this.response;
	}

	@Override
	public IndexableEvent getEvent() {
		return this.event;
	}

}
