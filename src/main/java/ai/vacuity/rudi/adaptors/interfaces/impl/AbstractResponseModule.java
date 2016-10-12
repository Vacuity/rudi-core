package ai.vacuity.rudi.adaptors.interfaces.impl;

import ai.vacuity.rudi.adaptors.interfaces.IndexableEvent;
import ai.vacuity.rudi.adaptors.interfaces.IResponseModule;

/**
 * Performs transformations on the response and/or input value.
 * 
 * @author In Lak'ech.
 *
 */
public abstract class AbstractResponseModule implements IResponseModule {

	protected String response;
	protected IndexableEvent event;

	@Override
	public void process(String response, IndexableEvent event) {
		this.response = response;
		this.event = event;
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
