package com.openlink;

import ai.vacuity.rudi.adaptors.interfaces.impl.AbstractTemplateProcessor;

public class UriBurner extends AbstractTemplateProcessor {

	@Override
	public void process(String template, String target) {
		super.process(template, target);
		this.target = target.replace("://", "/");
		// this.target = URLEncoder.encode(this.target);
	}

	@Override
	public String getTemplate() {
		return this.template;
	}

	@Override
	public String getInput() {
		return this.target;
	}
}
