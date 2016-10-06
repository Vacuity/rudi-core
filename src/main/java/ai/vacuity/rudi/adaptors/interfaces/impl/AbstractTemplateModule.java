package ai.vacuity.rudi.adaptors.interfaces.impl;

import org.slf4j.LoggerFactory;

import ai.vacuity.rudi.adaptors.interfaces.IEvent;
import ai.vacuity.rudi.adaptors.interfaces.ITemplateModule;

public abstract class AbstractTemplateModule implements ITemplateModule {
	public final static org.slf4j.Logger logger = LoggerFactory.getLogger(AbstractTemplateModule.class);

	protected String template;
	protected IEvent event;

	@Override
	public void process(String template, IEvent event) {
		this.template = template;
		this.event = event;
	}

	@Override
	public String getTemplate() {
		return this.template;
	}

	@Override
	public IEvent getEvent() {
		return this.event;
	}
}
