package ai.vacuity.rudi.adaptors.interfaces.impl;

import org.slf4j.LoggerFactory;

import ai.vacuity.rudi.adaptors.interfaces.IndexableEvent;
import ai.vacuity.rudi.adaptors.interfaces.ITemplateModule;

public abstract class AbstractTemplateModule implements ITemplateModule {
	public final static org.slf4j.Logger logger = LoggerFactory.getLogger(AbstractTemplateModule.class);

	protected String template;
	protected IndexableEvent event;

	@Override
	public void process(String template, IndexableEvent event) {
		this.template = template;
		this.event = event;
	}

	@Override
	public String getTemplate() {
		return this.template;
	}

	@Override
	public IndexableEvent getEvent() {
		return this.event;
	}
}
