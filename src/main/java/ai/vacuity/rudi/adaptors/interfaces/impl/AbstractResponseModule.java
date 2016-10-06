package ai.vacuity.rudi.adaptors.interfaces.impl;

import ai.vacuity.rudi.adaptors.interfaces.IEvent;
import ai.vacuity.rudi.adaptors.interfaces.IResponseModule;

/**
 * Performs transformations on the response and/or input value.
 * 
 * @author In Lak'ech.
 *
 */
public abstract class AbstractResponseModule implements IResponseModule {

	protected String response;
	protected IEvent event;

	@Override
	public void process(String response, IEvent event) {
		this.response = response;
		this.event = event;
	}

	@Override
	public String getResponse() {
		return this.response;
	}

	@Override
	public IEvent getEvent() {
		return this.event;
	}

}
