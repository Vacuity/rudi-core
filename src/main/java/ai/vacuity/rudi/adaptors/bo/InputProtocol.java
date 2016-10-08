package ai.vacuity.rudi.adaptors.bo;

import java.util.HashMap;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;

import ai.vacuity.rudi.adaptors.hal.hao.Constants;
import ai.vacuity.rudi.adaptors.hal.hao.GraphManager;

public class InputProtocol {

	public final static IRI PARSE_TYPE_REGEX = GraphManager.getValueFactory().createIRI(Constants.NS_VIA + "Regex");

	Value trigger = null;
	IRI dataType = null;

	String[] labels = null;
	HashMap<Integer, Literal> labelMap = new HashMap<Integer, Literal>();

	EventHandler eventHandler = null;
	Object pattern = null;

	IndexableQuery query = null;
	// boolean hasSparqlQuery = false;

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

	public String[] getLabels() {
		return labels;
	}

	public void setLabels(String[] label) {
		this.labels = label;
	}

	public HashMap<Integer, Literal> getLabelMap() {
		return labelMap;
	}

	public void setLabelMap(HashMap<Integer, Literal> labelMap) {
		this.labelMap = labelMap;
	}

	public EventHandler getEventHandler() {
		return eventHandler;
	}

	public void setEventHandler(EventHandler eventHandler) {
		this.eventHandler = eventHandler;
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
		return this.query != null;
	}

	// public void hasSparqlQuery(boolean hasSparqlQuery) {
	// this.hasSparqlQuery = hasSparqlQuery;
	// }
}
