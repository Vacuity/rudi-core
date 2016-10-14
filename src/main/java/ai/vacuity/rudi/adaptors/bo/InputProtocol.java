package ai.vacuity.rudi.adaptors.bo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;

import ai.vacuity.rudi.adaptors.hal.hao.Constants;
import ai.vacuity.rudi.adaptors.hal.hao.GraphManager;

public class InputProtocol {

	public final static IRI PARSE_TYPE_REGEX = GraphManager.getValueFactory().createIRI(Constants.NS_VIA + "Regex");
	public final static String OVERRIDE_PROMPT_POLICY = "prompt.policy";
	public final static String OVERRIDE_PROMPT_POLICY_RANDOM = "random";
	public final static String OVERRIDE_PROMPT_POLICY_ALL = "all";

	Value trigger = null;
	IRI dataType = null;

	List<Label> labels = new ArrayList<Label>();
	HashMap<Integer, Literal> labelMap = new HashMap<Integer, Literal>();

	EventHandler eventHandler = null;
	Object pattern = null;
	double patternScore = 0;

	Query query = null;
	List<Context> contexts = new ArrayList<Context>();
	// boolean hasSparqlQuery = false;

	Properties overrides = new Properties();

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

	public List<Label> getLabels() {
		return labels;
	}

	public void setLabels(List<Label> labels) {
		this.labels = labels;
	}

	public List<String> getLabelStrings() {
		List<String> labels = new ArrayList<String>();
		for (Label l : getLabels()) {
			labels.add(l.getLabel());
		}
		return labels;
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

	public Query getQuery() {
		return query;
	}

	public void setQuery(Query query) {
		this.query = query;
	}

	public boolean hasSparqlQuery() {
		return this.query != null;
	}

	public double getPatternScore() {
		return patternScore;
	}

	public void setPatternScore(double patternScore) {
		this.patternScore = patternScore;
	}

	public List<Context> getContexts() {
		return contexts;
	}

	public void setContexts(List<Context> contexts) {
		this.contexts = contexts;
	}

	public Properties getOverrides() {
		return overrides;
	}

	public void setOverrides(Properties overrides) {
		this.overrides = overrides;
	}

	// public void hasSparqlQuery(boolean hasSparqlQuery) {
	// this.hasSparqlQuery = hasSparqlQuery;
	// }
}
