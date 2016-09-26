package ai.vacuity.rudi.adaptors.bo;

import java.util.HashMap;
import java.util.regex.Pattern;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;

public class Input {

	Value trigger = null;

	Literal label = null;
	HashMap<Integer, Literal> labelMap = new HashMap<Integer, Literal>();

	Output output = null;
	Pattern pattern = null;

	public Pattern getPattern() {
		return pattern;
	}

	public void setPattern(Pattern pattern) {
		this.pattern = pattern;
	}

	public Value getTrigger() {
		return trigger;
	}

	public void setTrigger(Value trigger) {
		this.trigger = trigger;
	}

	public Literal getLabel() {
		return label;
	}

	public void setLabel(Literal label) {
		this.label = label;
	}

	public HashMap<Integer, Literal> getLabelMap() {
		return labelMap;
	}

	public void setLabelMap(HashMap<Integer, Literal> labelMap) {
		this.labelMap = labelMap;
	}

	public Output getOutput() {
		return output;
	}

	public void setOutput(Output output) {
		this.output = output;
	}
}
