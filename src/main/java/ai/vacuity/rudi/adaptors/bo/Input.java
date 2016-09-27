package ai.vacuity.rudi.adaptors.bo;

import java.util.HashMap;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;

import ai.vacuity.rudi.adaptors.data.QuadStore;

public class Input {

	public final static IRI PARSE_TYPE_REGEX = QuadStore.getValueFactory().createIRI("http://www.vacuity.ai/onto/via/Regex");

	Value trigger = null;
	IRI dataType = null;

	Literal label = null;
	HashMap<Integer, Literal> labelMap = new HashMap<Integer, Literal>();

	Output output = null;
	Object pattern = null;

	public Object getPattern() {
		return pattern;
	}

	public void setPattern(Object pattern) {
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

	public IRI getDataType() {
		return dataType;
	}

	public void setDataType(IRI dataType) {
		this.dataType = dataType;
	}
}
