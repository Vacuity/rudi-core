package ai.vacuity.rudi.adaptors.hal.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
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
import org.slf4j.LoggerFactory;

import ai.vacuity.rudi.adaptors.bo.Config;
import ai.vacuity.rudi.adaptors.bo.IndexableInput;
import ai.vacuity.rudi.adaptors.bo.InputProtocol;
import ai.vacuity.rudi.adaptors.bo.Match;
import ai.vacuity.rudi.adaptors.hal.hao.AbstractHAO;
import ai.vacuity.rudi.adaptors.hal.hao.Constants;
import ai.vacuity.rudi.adaptors.hal.hao.GraphManager;
import ai.vacuity.rudi.adaptors.hal.hao.RestfulHAO;
import ai.vacuity.rudi.adaptors.hal.hao.SPARQLHao;
import ai.vacuity.rudi.adaptors.interfaces.IEvent;
import ai.vacuity.rudi.adaptors.interfaces.impl.AbstractTemplateModule;

public class DispatchService extends Thread {

	public DispatchService() {
		start(); // start dispatching events
	}

	/**
	 * Starts the dispatch service.
	 */
	private final static DispatchService dispatcher = new DispatchService(); // this field is required to start the dispatcher, even though it is, for now, never used
	public final static org.slf4j.Logger logger = LoggerFactory.getLogger(DispatchService.class);
	private static final Vector<AbstractHAO> queue = new Vector<AbstractHAO>();

	@Override
	public void run() {
		while (true) {
			// try {
			// Thread.sleep(5000);
			// }
			// catch (InterruptedException e) {
			// logger.error(e.getMessage(), e);
			// }
			while (getQueue().size() > 0) {
				getQueue().get(0).run();
				getQueue().remove(0);
			}
		}
	}

	public static List<String> dispatch(IEvent event) throws IOException, IllegalArgumentException {
		index(event);

		ArrayList<String> logs = new ArrayList<String>();
		InputProtocol[] protocols = GraphManager.getTypedPatterns();
		find_matches: for (int i = 0; i < protocols.length; i++) {
			InputProtocol ip = protocols[i];
			// try regex patterns only if no typed patterns matched
			if (ip == null) {
				if (protocols.equals(GraphManager.getTypedPatterns())) {
					protocols = GraphManager.getRegexPatterns();
					continue;
				}
				else break;
			}

			String procedure = ip.getEventHandler().getCall();

			String log = ip.getEventHandler().getLog();
			AbstractHAO hao = null;

			String[] processed = new String[] {};

			if (ip.getEventHandler().hasSparqlQuery()) {
				procedure = ip.getEventHandler().getSparql();
				processed = DispatchService.process(ip, event, procedure, log);
				hao = new SPARQLHao();
			}
			else {
				processed = DispatchService.process(ip, event, procedure, log);
				hao = new RestfulHAO();
			}
			if (processed.length == 0) continue find_matches;

			logs.add(processed[1]);
			logger.debug("[Rudi]: " + processed[1]);

			hao.setCall(processed[0]);
			hao.setInputProtocol(ip);
			hao.setEvent(event);
			DispatchService.add(hao);
		}
		return logs;
	}

	// TODO need to merge this with the other dispatch() method
	public static void dispatch(int id, Resource context) throws IOException, IllegalArgumentException {
		ArrayList<String> logs = new ArrayList<String>();
		find_matches: for (InputProtocol ip : GraphManager.getQueryPatterns()) {
			if (ip == null) break;
			if (ip.getEventHandler() != null || ip.hasSparqlQuery()) { // assume the event notifies an eventhandler
				String procedure = ip.getEventHandler().getCall(), log = ip.getEventHandler().getLog();
				String[] processed = new String[] {};
				AbstractHAO hao = null;

				if (ip.getEventHandler().hasSparqlQuery()) {
					procedure = ip.getEventHandler().getSparql();
					processed = DispatchService.process(ip, id, context, procedure, log);
					hao = new SPARQLHao();
				}
				else {
					processed = DispatchService.process(ip, id, context, procedure, log);
					hao = new RestfulHAO();
				}
				if (processed.length == 0) continue find_matches;

				logs.add(processed[1]);
				logger.debug("[Rudi]: " + processed[1]);

				hao.setCall(processed[0]);
				hao.setInputProtocol(ip);
				hao.setEvent(ip.getQuery()); // send a label form of the query to the response pipeline
				// hao.run();
				DispatchService.add(hao);
				GraphManager.removeFromInbox(context);
			}
			else {
				logger.debug("[Rudi]: Notifying the delegate '" + ip.getQuery().getOwnerIri() + "'");
			}
			index(ip.getQuery());
		}
	}

	/**
	 * Matches the given input against the input protocol, then processes the associated response protocol templates
	 * 
	 * @param ip
	 *            the InputProtocol describing the match criteria
	 * @param event
	 *            the input to match
	 * @param template
	 *            the template associated with the response protocol
	 * @return
	 */
	private static String[] process(InputProtocol ip, IEvent event, String... templates) {
		// 1. swap captured groups' placeholders first
		// FIRST MATCH GATE
		boolean found = false;
		if (ip.getDataType() != null && ip.getDataType().equals(InputProtocol.PARSE_TYPE_REGEX) && ip.getPattern() instanceof Pattern) {
			Matcher matcher = ((Pattern) ip.getPattern()).matcher(event.getLabel());
			while (matcher.find()) {
				if (!found) {
					// logger.debug("Match: " + ip.getTrigger().stringValue());
					if (event instanceof IndexableInput) {
						Match m = new Match();
						m.setPattern(ip.getPattern().toString());
						m.setScore(m.getPattern().length());
						((IndexableInput) event).addMatch(m);
					}
					found = true;
				}
				int groups = matcher.groupCount();
				for (int gp = 1; gp <= groups; gp++) {
					// logger.debug("group " + gp + ": " + matcher.group(gp));
					for (int i = 0; i < templates.length; i++)
						templates[i] = templates[i].replace("${" + gp + "}", matcher.group(gp));
				}

				// if ${0} remains, assume no pattern group matched it, so set it to the entire
				// input event
				for (int i = 0; i < templates.length; i++)
					templates[i] = templates[i].replace("${0}", matcher.group());
			}
		}

		// SECOND MATCH GATE
		if (ip.getPattern() instanceof List) {
			List<Value> l = (List<Value>) ip.getPattern();
			for (int j = 0; j < l.size(); j++) {
				Value value = l.get(j);
				if (value == null) continue;
				if (value instanceof Literal) {
					// if()
					Literal lit = (Literal) value;
					if (lit.getDatatype().equals(GraphManager.getValueFactory().createIRI(Constants.NS_VIA + "URL"))) {
						try {
							new URL(event.getLabel());
							found = true;
						}
						catch (MalformedURLException mfuex) {
							return new String[0];
						}
					}
					if (lit.getDatatype().equals(GraphManager.getValueFactory().createIRI(Constants.NS_XSD + "anyURI"))) {
						try {
							GraphManager.getValueFactory().createIRI(event.getLabel());
							found = true;
						}
						catch (IllegalArgumentException iaex) {
							return new String[0];
						}
					}
					if (lit.getDatatype().equals(GraphManager.getValueFactory().createIRI(Constants.NS_XSD + "integer"))) {
						try {
							Integer.parseInt(event.getLabel());
							found = true;
						}
						catch (NumberFormatException nfex) {
							return new String[0];
						}
					}
					if (lit.getDatatype().equals(GraphManager.getValueFactory().createIRI(Constants.NS_XSD + "dateTime"))) {
						try {
							// for type dateTime, the value specifies the date format
							new SimpleDateFormat(lit.stringValue()).parse(event.getLabel());
							found = true;
						}
						catch (ParseException e) {
							return new String[0];
						}
					}

					// if ${number} tags are remaining, assume the typed patterns matched and the regex patterns did not,
					// in this case, swap the capture group TODO corresponding to the current input region (for now, just
					// swap the first non-empty capture result
					for (int i = 0; i < templates.length; i++) {
						if (StringUtils.isNotBlank(templates[i])) {
							templates[i] = templates[i].replace("${" + j + "}", event.getLabel());
						}
					}
					break;
				}
			}
		}

		if (!found) return new String[0];

		// 2. run the call processor (give customer processors priority over system processor)
		// TODO should the log include the unprocessed placeholder value?

		for (int i = 0; i < templates.length; i++) {
			if (ip.getEventHandler().hasEndpointTemplateModule()) {

				ip.getEventHandler().getEndpointTemplateModule().process(templates[i], event);
				templates[i] = ip.getEventHandler().getEndpointTemplateModule().getTemplate();

				// allow the template processor to transform user input prior to user input being filled in template
				event = ip.getEventHandler().getEndpointTemplateModule().getEvent();
			}

			// 3. swap any remaining reserved placed holders
			if (ip.getEventHandler().hasEndpointKey()) templates[i] = templates[i].replace("${key}", ip.getEventHandler().getEndpointKey());
			if (ip.getEventHandler().hasEndpointId()) templates[i] = templates[i].replace("${id}", ip.getEventHandler().getEndpointId());
			if (ip.getEventHandler().hasEndpointToken()) templates[i] = templates[i].replace("${token}", ip.getEventHandler().getEndpointToken());
			templates[i] = templates[i].replace("http://rudi.endpoint.placeholders.vacuity.ai", Config.getRudiEndpoint());
			templates[i] = templates[i].replace("http://rudi.host.placeholders.vacuity.ai", (Boolean.parseBoolean(Config.RUDI_SECURE) ? "https://" : "http://") + Config.RUDI_HOST);
			templates[i] = templates[i].replace("${rudi.host}", Config.RUDI_HOST);
			templates[i] = templates[i].replace("${rudi.endpoint}", Config.getRudiEndpoint());
			templates[i] = templates[i].replace("${domain}", ip.getEventHandler().getEndpointDomain());
			templates[i] = templates[i].replace("${port}", ip.getEventHandler().getEndpointPort() + "");
		}

		return templates;
	}

	private static String[] process(InputProtocol ip, int id, Resource context, String... templates) {
		if (ip.getQuery().getId() != id) return null;
		String query = ip.getQuery().getDelegate().toString().replace("\n", "").replace("\t", "");

		logger.debug("Context: " + context);

		TupleQuery alertQuery = GraphManager.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query);
		alertQuery.setBinding("context", context);
		try (TupleQueryResult alerts = alertQuery.evaluate()) {
			if (!alerts.hasNext()) return new String[0];
			UUID u = UUID.randomUUID();
			IRI alertIRI = GraphManager.getValueFactory().createIRI(Constants.NS_VI, "alrt-" + u);
			Vector<Statement> tuples = new Vector<Statement>();
			if (alerts.hasNext()) {
				Resource owner = ip.getQuery().getOwnerIri();
				if (owner == null) owner = ip.getEventHandler().getIri();
				if (owner == null) owner = GraphManager.getRepository().getValueFactory().createIRI(Constants.CONTEXT_DEMO);
				tuples.add(GraphManager.getValueFactory().createStatement(owner, GraphManager.getValueFactory().createIRI(Constants.NS_SIOC + "owner_of"), alertIRI));
				tuples.add(GraphManager.getValueFactory().createStatement(alertIRI, GraphManager.getValueFactory().createIRI(Constants.NS_VIA + "context"), context));
			}
			for (int j = 0; alerts.hasNext(); j++) {
				try {
					UUID uuid = UUID.randomUUID();
					IRI hit = GraphManager.getValueFactory().createIRI(Constants.NS_VI, "hit-" + uuid);
					BindingSet captures = alerts.next();
					Iterator<String> capture_names = captures.getBindingNames().iterator();
					tuples.add(GraphManager.getValueFactory().createStatement(hit, GraphManager.rdf_type, GraphManager.via_Hit));
					tuples.add(GraphManager.getValueFactory().createStatement(hit, GraphManager.getValueFactory().createIRI(Constants.NS_SIOC + "has_container"), alertIRI));
					tuples.add(GraphManager.getValueFactory().createStatement(hit, GraphManager.via_timestamp, GraphManager.getValueFactory().createLiteral(new Date())));
					tuples.add(GraphManager.getValueFactory().createStatement(hit, GraphManager.via_query, ip.getQuery().getIri()));
					while (capture_names.hasNext()) {
						String name = capture_names.next();
						if (captures.getValue(name) == null) {
							AbstractTemplateModule.logger.error("Null value for capture: " + name + "\nQuery:\n" + query);
						}
						String value = captures.getValue(name).stringValue();
						if (value == null) value = "-- null value --";
						for (int i = 0; i < templates.length; i++) {
							if (StringUtils.isNotBlank(templates[i])) {
								templates[i] = templates[i].replace("${" + name + "}", value);
							}
						}

						AbstractTemplateModule.logger.debug("SPARQL Capture Value (" + name + "." + j + "): " + value);

						Literal valueLit = GraphManager.getValueFactory().createLiteral(name);

						try {
							valueLit = GraphManager.getValueFactory().createLiteral(Integer.parseInt(name)); // try to make the capture's datatype granular
						}
						catch (NumberFormatException nfex) {
						}
						UUID puuid = UUID.randomUUID();
						IRI p = GraphManager.getValueFactory().createIRI(Constants.NS_VI, "p-" + puuid);
						tuples.add(GraphManager.getValueFactory().createStatement(p, GraphManager.rdf_type, GraphManager.via_Projection));
						tuples.add(GraphManager.getValueFactory().createStatement(p, GraphManager.via_bindName, valueLit));
						tuples.add(GraphManager.getValueFactory().createStatement(p, GraphManager.via_value, captures.getValue(name)));
						tuples.add(GraphManager.getValueFactory().createStatement(p, GraphManager.getValueFactory().createIRI(Constants.NS_SIOC + "has_container"), hit));
					}
				}
				catch (Exception e) {
					logger.error(e.getMessage(), e);
					continue;
				}
				GraphManager.addToRepository(tuples, alertIRI);
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
			return new String[0];
		}
		finally {
			GraphManager.getConnection().close();
		}
		for (int i = 0; i < templates.length; i++) {
			if (ip.getEventHandler().hasEndpointTemplateModule()) {
				ip.getEventHandler().getEndpointTemplateModule().process(templates[i], ip.getQuery());
				templates[i] = ip.getEventHandler().getEndpointTemplateModule().getTemplate();

				// allow the template processor to transform user input prior to user input being filled in template
				ip.getQuery().setLabel(Config.get(ip.getEventHandler().getConfigLabel()).getTemplateModule().getEvent().getLabel());
			}
			// if ${number} tags are remaining, assume the typed patterns matched and the regex patterns did not,
			// in this case, swap the capture group TODO corresponding to the current input region (for now, just
			// swap the first non-empty capture result
			// if (StringUtils.isNotBlank(templates[i])) {
			// if (ip.getPattern() instanceof Model) {
			// Iterator<Statement> m = ((Model) ip.getPattern()).iterator();
			// for (int j = 0; m.hasNext(); j++) {
			// Statement st = m.next();
			// if (st == null) continue;
			// templates[i] = templates[i].replace("${" + j + "}", ip.getQuery().getLabel());
			// break;
			// }
			// }
			// }

			// if ${0} remains, assume no projection items replaced it, so set 0 = the entire query event
			if (StringUtils.isNotBlank(templates[i])) templates[i] = templates[i].replace("${0}", ip.getQuery().getDelegate().toString());

			// 3. swap any remaining reserved placed holders
			if (ip.getEventHandler().hasEndpointKey()) templates[i] = templates[i].replace("${key}", ip.getEventHandler().getEndpointKey());
			if (ip.getEventHandler().hasEndpointId()) templates[i] = templates[i].replace("${id}", ip.getEventHandler().getEndpointId());
			if (ip.getEventHandler().hasEndpointToken()) templates[i] = templates[i].replace("${token}", ip.getEventHandler().getEndpointToken());
			templates[i] = templates[i].replace("http://rudi.endpoint.placeholders.vacuity.ai", Config.getRudiEndpoint());
			templates[i] = templates[i].replace("http://rudi.host.placeholders.vacuity.ai", (Boolean.parseBoolean(Config.RUDI_SECURE) ? "https://" : "http://") + Config.RUDI_HOST);
			templates[i] = templates[i].replace("${rudi.host}", Config.RUDI_HOST);
			templates[i] = templates[i].replace("${rudi.endpoint}", Config.getRudiEndpoint());
			templates[i] = templates[i].replace("${domain}", ip.getEventHandler().getEndpointDomain());
			templates[i] = templates[i].replace("${port}", ip.getEventHandler().getEndpointPort() + "");
		}

		return templates;
	}

	public static void index(IEvent event) {
		Vector<Statement> tuples = new Vector<Statement>();
		IRI sioc_owner_of = GraphManager.getValueFactory().createIRI(Constants.NS_SIOC, "owner_of");
		IRI rdf_type = GraphManager.getValueFactory().createIRI(Constants.NS_RDF, "type");
		IRI via_channel = GraphManager.getValueFactory().createIRI(Constants.NS_VIA, "Channel");
		IRI rdfs_label = GraphManager.getValueFactory().createIRI(Constants.NS_RDFS, "label");
		IRI owner = event.getOwnerIri();
		if (owner == null) owner = GraphManager.getValueFactory().createIRI(Constants.CONTEXT_DEMO);

		tuples.add(GraphManager.getValueFactory().createStatement(owner, sioc_owner_of, event.getIri()));
		tuples.add(GraphManager.getValueFactory().createStatement(event.getIri(), GraphManager.via_timestamp, GraphManager.getValueFactory().createLiteral(new Date())));
		tuples.add(GraphManager.getValueFactory().createStatement(event.getIri(), rdf_type, via_channel));
		tuples.add(GraphManager.getValueFactory().createStatement(event.getIri(), rdfs_label, GraphManager.getValueFactory().createLiteral(event.getLabel())));
		GraphManager.addToRepository(tuples, GraphManager.getValueFactory().createIRI(Constants.CONTEXT_DEMO));
	}

	private static Vector<AbstractHAO> getQueue() {
		return DispatchService.queue;
	}

	private static void add(AbstractHAO hao) {
		getQueue().add(hao);
	}

}
