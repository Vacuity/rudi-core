package ai.vacuity.rudi.adaptors.interfaces.impl;

import ai.vacuity.rudi.adaptors.interfaces.TemplateProcessor;

public class AbstractTemplateProcessor implements TemplateProcessor {

	protected String template;
	protected String target;

	@Override
	public void process(String template, String target) {
		this.template = template;
		this.target = target;
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
