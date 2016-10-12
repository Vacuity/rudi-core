package com.openlink;

import ai.vacuity.rudi.adaptors.interfaces.IndexableEvent;
import ai.vacuity.rudi.adaptors.interfaces.impl.AbstractTemplateModule;

public class UriBurner extends AbstractTemplateModule {

	@Override
	public void process(String template, IndexableEvent target) {
		super.process(template, target);
		this.event.setLabel(target.getLabel().replace("://", "/"));
		// this.target = URLEncoder.encode(this.target);
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
