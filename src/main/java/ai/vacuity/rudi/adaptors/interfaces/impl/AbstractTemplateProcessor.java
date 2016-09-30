package ai.vacuity.rudi.adaptors.interfaces.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;

import ai.vacuity.rudi.adaptors.bo.Config;
import ai.vacuity.rudi.adaptors.bo.InputProtocol;
import ai.vacuity.rudi.adaptors.hal.hao.Constants;
import ai.vacuity.rudi.adaptors.hal.hao.SparqlHAO;
import ai.vacuity.rudi.adaptors.interfaces.ITemplateProcessor;

public abstract class AbstractTemplateProcessor implements ITemplateProcessor {

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
	public String getInput() {
		return this.target;
	}

	/**
	 * Matches the given input against the input protocol, then processes the associated response protocol templates
	 * 
	 * @param ip
	 *            the InputProtocol describing the match criteria
	 * @param input
	 *            the input to match
	 * @param template
	 *            the template associated with the response protocol
	 * @return
	 */
	public static String process(InputProtocol ip, String input, String template) {
		// 1. swap captured groups' placeholders first
		// FIRST MATCH GATE
		boolean found = false;
		if (ip.getDataType().equals(InputProtocol.PARSE_TYPE_REGEX) && ip.getPattern() instanceof Pattern) {
			Matcher matcher = ((Pattern) ip.getPattern()).matcher(input);
			while (matcher.find()) {
				if (!found) {
					SparqlHAO.logger.debug("Match: " + ip.getTrigger().stringValue());
					found = true;
				}
				int groups = matcher.groupCount();
				for (int gp = 1; gp <= groups; gp++) {
					SparqlHAO.logger.debug("group " + gp + ": " + matcher.group(gp));
					template = template.replace("${" + gp + "}", matcher.group(gp));
				}
				template = template.replace("${0}", matcher.group());
			}
		}

		// SECOND MATCH GATE
		if (ip.getDataType().equals(SparqlHAO.getValueFactory().createIRI("http://www.vacuity.ai/onto/via/1.0/URL"))) {
			try {
				new URL(input);
				found = true;
			}
			catch (MalformedURLException mfuex) {
				return null;
			}
		}
		if (ip.getDataType().equals(SparqlHAO.getValueFactory().createIRI("http://www.w3.org/2001/XMLSchema#anyURI"))) {
			try {
				SparqlHAO.getValueFactory().createIRI(input);
				found = true;
			}
			catch (IllegalArgumentException iaex) {
				return null;
			}
		}
		if (ip.getDataType().equals(SparqlHAO.getValueFactory().createIRI("http://www.w3.org/2001/XMLSchema#integer"))) {
			try {
				Integer.parseInt(input);
				found = true;
			}
			catch (NumberFormatException nfex) {
				return null;
			}
		}
		if (ip.getDataType().equals(SparqlHAO.getValueFactory().createIRI("http://www.w3.org/2001/XMLSchema#dateTime"))) {
			try {
				// for type dateTime, the value is the date format
				new SimpleDateFormat(((Value) ip.getPattern()).stringValue());
				found = true;
			}
			catch (NumberFormatException nfex) {
				return null;
			}
		}

		if (!found) return null;

		// 2. run the call processor (give customer processors priority over system processor)
		// TODO should the log include the unprocessed placeholder value?

		if (Config.getMap().get(ip.getEventHandler().getConfigLabel()).hasTemplateProcessor()) {
			Config.getMap().get(ip.getEventHandler().getConfigLabel()).getTemplateProcessor().process(template, input);
			template = Config.getMap().get(ip.getEventHandler().getConfigLabel()).getTemplateProcessor().getTemplate();

			// allow the template processor to transform user input prior to user input being filled in template
			input = Config.getMap().get(ip.getEventHandler().getConfigLabel()).getTemplateProcessor().getInput();
		}
		if (StringUtils.isNotBlank(template)) template = template.replace("${" + ip.getCaptureIndex() + "}", input);

		// 3. swap any remaining reserved placed holders
		if (Config.getMap().get(ip.getEventHandler().getConfigLabel()).hasKey()) template = template.replace("${key}", Config.getMap().get(ip.getEventHandler().getConfigLabel()).getKey());
		if (Config.getMap().get(ip.getEventHandler().getConfigLabel()).hasId()) template = template.replace("${id}", Config.getMap().get(ip.getEventHandler().getConfigLabel()).getId());
		if (Config.getMap().get(ip.getEventHandler().getConfigLabel()).hasToken()) template = template.replace("${token}", Config.getMap().get(ip.getEventHandler().getConfigLabel()).getToken());

		return template;
	}

	public static String process(InputProtocol ip, int id, String template) {
		if (ip.getQuery().getId() != id) return null;
		// 1. swap captured groups' placeholders first
		// FIRST MATCH GATE
		// boolean found = false;
		// if (ip.getDataType().equals(InputProtocol.PARSE_TYPE_REGEX) && ip.getPattern() instanceof Pattern) {
		// Matcher matcher = ((Pattern) ip.getPattern()).matcher(input);
		// while (matcher.find()) {
		// if (!found) {
		// SparqlHAO.logger.debug("Match: " + ip.getTrigger().stringValue());
		// found = true;
		// }
		// int groups = matcher.groupCount();
		// for (int gp = 1; gp <= groups; gp++) {
		// SparqlHAO.logger.debug("group " + gp + ": " + matcher.group(gp));
		// template = template.replace("${" + gp + "}", matcher.group(gp));
		// }
		// template = template.replace("${0}", matcher.group());
		// }
		// }
		//
		// // SECOND MATCH GATE
		// if (ip.getDataType().equals(SparqlHAO.getValueFactory().createIRI("http://www.vacuity.ai/onto/via/1.0/URL"))) {
		// try {
		// new URL(input);
		// found = true;
		// }
		// catch (MalformedURLException mfuex) {
		// return null;
		// }
		// }
		// if (ip.getDataType().equals(SparqlHAO.getValueFactory().createIRI("http://www.w3.org/2001/XMLSchema#anyURI"))) {
		// try {
		// SparqlHAO.getValueFactory().createIRI(input);
		// found = true;
		// }
		// catch (IllegalArgumentException iaex) {
		// return null;
		// }
		// }
		// if (ip.getDataType().equals(SparqlHAO.getValueFactory().createIRI("http://www.w3.org/2001/XMLSchema#integer"))) {
		// try {
		// Integer.parseInt(input);
		// found = true;
		// }
		// catch (NumberFormatException nfex) {
		// return null;
		// }
		// }
		// if (ip.getDataType().equals(SparqlHAO.getValueFactory().createIRI("http://www.w3.org/2001/XMLSchema#dateTime"))) {
		// try {
		// // for type dateTime, the value is the date format
		// new SimpleDateFormat(((Value) ip.getPattern()).stringValue());
		// found = true;
		// }
		// catch (NumberFormatException nfex) {
		// return null;
		// }
		// }
		//
		// if (!found) return null;

		// 2. run the call processor (give customer processors priority over system processor)
		// TODO should the log include the unprocessed placeholder value?
		String query = ip.getQuery().getDelegate().toString().replace("\n", "").replace("\t", "");
		TupleQuery alertQuery = SparqlHAO.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query);
		// data.setBinding("patternPropertyType", getValueFactory().createIRI("http://www.vacuity.ai/onto/via/pattern"));
		try (TupleQueryResult alerts = alertQuery.evaluate()) {
			if (!alerts.hasNext()) return null;
			Vector<Statement> tuples = new Vector<Statement>();
			while (alerts.hasNext()) {
				BindingSet captures = alerts.next();
				Iterator<String> capture_names = captures.getBindingNames().iterator();
				while (capture_names.hasNext()) {
					String name = capture_names.next();
					String value = captures.getValue(name).stringValue();
					if (value == null) value = "-- null value --";
					if (StringUtils.isNotBlank(template)) template = template.replace("${" + name + "}", value);

					UUID uuid = UUID.randomUUID();

					// log the hit
					IRI hit = SparqlHAO.getValueFactory().createIRI(Constants.NS_VI, "hit-" + uuid);
					IRI rdf_type = SparqlHAO.getValueFactory().createIRI(Constants.NS_RDF, "type");
					IRI via_Hit = SparqlHAO.getValueFactory().createIRI(Constants.NS_RDF, "type");
					IRI via_index = SparqlHAO.getValueFactory().createIRI(Constants.NS_RDF, "index");
					IRI via_value = SparqlHAO.getValueFactory().createIRI(Constants.NS_RDF, "value");
					IRI via_query = SparqlHAO.getValueFactory().createIRI(Constants.NS_RDF, "query");
					Literal valueLit = SparqlHAO.getValueFactory().createLiteral(Integer.parseInt(name));

					tuples.add(SparqlHAO.getValueFactory().createStatement(hit, rdf_type, via_Hit));
					tuples.add(SparqlHAO.getValueFactory().createStatement(hit, via_query, ip.getQuery().getIri()));
					tuples.add(SparqlHAO.getValueFactory().createStatement(hit, via_index, valueLit));
					tuples.add(SparqlHAO.getValueFactory().createStatement(hit, via_value, captures.getValue(name)));
				}
			}
			Resource context = SparqlHAO.getRepository().getValueFactory().createIRI("http://tryrudi.io/rdf/demo/");
			SparqlHAO.getRepository().getConnection().begin();
			SparqlHAO.getRepository().getConnection().add(tuples, context);
			SparqlHAO.getRepository().getConnection().commit();
		}
		catch (NumberFormatException nfex) {
			return null;
		}

		if (Config.getMap().get(ip.getEventHandler().getConfigLabel()).hasTemplateProcessor()) {
			Config.getMap().get(ip.getEventHandler().getConfigLabel()).getTemplateProcessor().process(template, ip.getQuery().getLabel());
			template = Config.getMap().get(ip.getEventHandler().getConfigLabel()).getTemplateProcessor().getTemplate();

			// allow the template processor to transform user input prior to user input being filled in template
			ip.getQuery().setLabel(Config.getMap().get(ip.getEventHandler().getConfigLabel()).getTemplateProcessor().getInput());
		}
		if (StringUtils.isNotBlank(template)) template = template.replace("${" + ip.getCaptureIndex() + "}", ip.getQuery().getLabel());

		// 3. swap any remaining reserved placed holders
		if (Config.getMap().get(ip.getEventHandler().getConfigLabel()).hasKey()) template = template.replace("${key}", Config.getMap().get(ip.getEventHandler().getConfigLabel()).getKey());
		if (Config.getMap().get(ip.getEventHandler().getConfigLabel()).hasId()) template = template.replace("${id}", Config.getMap().get(ip.getEventHandler().getConfigLabel()).getId());
		if (Config.getMap().get(ip.getEventHandler().getConfigLabel()).hasToken()) template = template.replace("${token}", Config.getMap().get(ip.getEventHandler().getConfigLabel()).getToken());

		return template;
	}
}
