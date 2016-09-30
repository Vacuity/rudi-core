package ai.vacuity.rudi.adaptors.bo;

import java.util.HashMap;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;

import ai.vacuity.rudi.adaptors.hal.hao.IndexableQuery;
import ai.vacuity.rudi.adaptors.hal.hao.SparqlHAO;

public class InputProtocol {

	public final static IRI PARSE_TYPE_REGEX = SparqlHAO.getValueFactory().createIRI("http://www.vacuity.ai/onto/via/1.0/Regex");

	Value trigger = null;
	IRI dataType = null;

	Literal label = null;
	HashMap<Integer, Literal> labelMap = new HashMap<Integer, Literal>();

	ResponseProtocol responseProtocol = null;
	Object pattern = null;

	IndexableQuery query = null;
	boolean hasSparqlQuery = false;

	// by setting the default value to 0, all datatype patterns will default to zero.
	// see QuadStore.processTemplate()
	int captureIndex = 0;

	public int getCaptureIndex() {
		return captureIndex;
	}

	public void setCaptureIndex(int captureIndex) {
		this.captureIndex = captureIndex;
	}

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

	public ResponseProtocol getResponseProtocol() {
		return responseProtocol;
	}

	public void setResponseProtocol(ResponseProtocol responseProtocol) {
		this.responseProtocol = responseProtocol;
	}

	public IRI getDataType() {
		return dataType;
	}

	public void setDataType(IRI dataType) {
		this.dataType = dataType;
	}

	public IndexableQuery getQuery() {
		return query;
	}

	public void setQuery(IndexableQuery query) {
		this.query = query;
	}

	public boolean hasSparqlQuery() {
		return hasSparqlQuery;
	}

	public void hasSparqlQuery(boolean hasSparqlQuery) {
		this.hasSparqlQuery = hasSparqlQuery;
	}
}
