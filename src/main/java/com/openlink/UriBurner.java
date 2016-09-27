package com.openlink;

import ai.vacuity.rudi.adaptors.interfaces.TemplateProcessor;

public class UriBurner implements TemplateProcessor {
	private String template = null;
	private String target = null;

	@Override
	public void process(String template, String target) {
		this.template = template;
		this.target = target.replace("://", "/");
	}

	@Override
	public String getTemplate() {
		return this.template;
	}

	@Override
	public String getTarget() {
		return this.target;
	}
}
