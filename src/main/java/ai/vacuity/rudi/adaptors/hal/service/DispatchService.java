package ai.vacuity.rudi.adaptors.hal.service;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.slf4j.LoggerFactory;

import ai.vacuity.rudi.adaptors.bo.Config;
import ai.vacuity.rudi.adaptors.bo.IndexableInput;
import ai.vacuity.rudi.adaptors.bo.InputProtocol;
import ai.vacuity.rudi.adaptors.hal.hao.AbstractHAO;
import ai.vacuity.rudi.adaptors.hal.hao.Constants;
import ai.vacuity.rudi.adaptors.hal.hao.GraphManager;
import ai.vacuity.rudi.adaptors.hal.hao.RestfulHAO;
import ai.vacuity.rudi.adaptors.hal.hao.SPARQLHao;
import ai.vacuity.rudi.adaptors.interfaces.IEvent;
import ai.vacuity.rudi.adaptors.interfaces.impl.AbstractTemplateModule;
import ai.vacuity.rudi.adaptors.types.Match;
import ai.vacuity.rudi.adaptors.types.Report;

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

			synchronized (getQueue()) {
				while (getQueue().size() > 0) {
					getQueue().remove(0).run();
				}
			}
		}
	}

	public static ai.vacuity.rudi.adaptors.types.Report process(IEvent event) throws IOException, IllegalArgumentException {
		return process(event, true);
	}

	public static Report process(IEvent event, boolean dispatch) throws IOException, IllegalArgumentException {
		index(event);

		Report report = new Report();
		List<AbstractHAO> haos = new ArrayList<AbstractHAO>();

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
				processed = DispatchService.begin(ip, event, report, procedure, log);
				hao = new SPARQLHao();
			}
			else {
				processed = DispatchService.begin(ip, event, report, procedure, log);
				hao = new RestfulHAO();
			}
			if (processed.length == 0) continue find_matches;

			report.getLogs().add(processed[1]);

			if (dispatch) {
				hao.setCall(processed[0]);
				hao.setInputProtocol(ip);
				hao.setEvent(event);
				haos.add(hao);
			}
		}
		double highest = 0;
		for (Match m : report.getMatches()) {
			if (m.getScore() > highest) highest = m.getScore();
		}
		for (int i = 0; i < report.getMatches().size(); i++) {
			Match m = report.getMatches().get(i);
			if (m.getScore() < highest) {
				report.getMatches().remove(i);
				report.getLogs().remove(i);
				if (dispatch) haos.remove(i);
				i--;
			}
		}
		for (AbstractHAO hao : haos) {
			DispatchService.add(hao);
		}
		for (String log : report.getLogs()) {
			logger.debug("[Rudi]: " + log);
		}
		return report;
	}

	// TODO need to merge this with the other dispatch() method
	public static void process(int id, Resource context) throws IOException, IllegalArgumentException {
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
	private static String[] process(InputProtocol ip, IEvent event, Report report, String... templates) {
		// 1. swap captured groups' placeholders first
		// FIRST MATCH GATE
		boolean found = false;
		Match m = new Match();
		m.setLabels(ip.getLabels());
		m.setScore(ip.getPatternScore());
		if (ip.getDataType() != null && ip.getDataType().equals(InputProtocol.PARSE_TYPE_REGEX) && ip.getPattern() instanceof Pattern) {
			Matcher matcher = ((Pattern) ip.getPattern()).matcher(event.getLabel());
			// TODO for now, only allow one run of the matcher per input, to allow a full run, the added matches will need to correspond to the added HAOs in the process() caller
			if (matcher.find()) {
				if (!found) {
					if (event instanceof IndexableInput) {
						report.getMatches().add(m);
					}
					// logger.debug("Match: " + ip.getTrigger().stringValue());
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
							report.getMatches().add(m);
							found = true;
						}
						catch (MalformedURLException mfuex) {
							return new String[0];
						}
					}
					if (lit.getDatatype().equals(GraphManager.getValueFactory().createIRI(Constants.NS_XSD + "anyURI"))) {
						try {
							GraphManager.getValueFactory().createIRI(event.getLabel());
							report.getMatches().add(m);
							found = true;
						}
						catch (IllegalArgumentException iaex) {
							return new String[0];
						}
					}
					if (lit.getDatatype().equals(GraphManager.getValueFactory().createIRI(Constants.NS_XSD + "integer"))) {
						try {
							Integer.parseInt(event.getLabel());
							report.getMatches().add(m);
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
							report.getMatches().add(m);
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

	private static String[] begin(InputProtocol ip, IEvent event, Report report, String... templates) {
		List<String> imports = ip.getEventHandler().getImports();
		if (ip.getEventHandler().getImports().size() > 0) {
			try {
				Method m = DispatchService.class.getDeclaredMethod("process", new Class[] { InputProtocol.class, IEvent.class, Report.class, String[].class });
				m.setAccessible(true);
				Object[] args = new Object[4];
				args[0] = ip;
				args[1] = event;
				args[2] = report;
				int len = templates.length;
				String[] sa = new String[len + imports.size()];
				for (int i = 0; i < len; i++) {
					sa[i] = templates[i];
				}
				for (int i = len; i < len + imports.size(); i++) {
					sa[i] = imports.get(i - len);
				}
				args[3] = sa;
				try {
					templates = (String[]) m.invoke(new DispatchService(), args);
					templates = process(report, len, sa);
				}
				catch (IllegalAccessException e) {
					logger.error(e.getMessage(), e);
				}
				catch (IllegalArgumentException e) {
					logger.error(e.getMessage(), e);
				}
				catch (InvocationTargetException e) {
					logger.error(e.getMessage(), e);
				}
				return templates;
			}
			catch (NoSuchMethodException e) {
				logger.error(e.getMessage(), e);
			}
			catch (SecurityException e) {
				logger.error(e.getMessage(), e);
			}
			return templates;
		}
		else {
			return process(ip, event, report, templates);
		}
	}

	private static String[] process(InputProtocol ip, int id, Resource context, String... templates) {
		if (ip.getQuery().getId() != id) return null;
		String query = ip.getQuery().getDelegate().toString().replace("\n", "").replace("\t", "");

		// logger.debug("Context: " + context);

		try (RepositoryConnection con = GraphManager.getConnection()) {
			TupleQuery alertQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, query);
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
									String captureIndex = name;
									if (captureIndex.indexOf("_") > 0) {
										captureIndex = captureIndex.substring(0, captureIndex.indexOf("_"));
									}
									templates[i] = templates[i].replace("${" + captureIndex + "}", value);
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
		}
		catch (RepositoryException rex) {
			logger.error(rex.getMessage(), rex);
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

	public static String[] process(Report report, int onset, String... templates) {
		for (int i = onset; i < templates.length; i++) {
			String query = templates[i];
			try (RepositoryConnection con = GraphManager.getConnection()) {
				TupleQuery ipQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, query);
				try (TupleQueryResult r3 = ipQuery.evaluate()) {
					if (r3.hasNext()) { // expect a single result
						BindingSet bindings = r3.next();
						Iterator<String> binding_names = bindings.getBindingNames().iterator();
						while (binding_names.hasNext()) {
							String name = binding_names.next();
							String value = bindings.getValue(name).stringValue();
							if (value == null) value = "-- null value --";
							for (int j = 0; j < templates.length; j++) {
								if (StringUtils.isNotBlank(templates[j])) {
									templates[j] = templates[j].replace("${" + name + "}", value);
								}
							}
						}
					}
				}
				catch (QueryEvaluationException qex) {
					logger.error(qex.getMessage(), qex);
				}
			}
			catch (RepositoryException rex) {
				logger.error(rex.getMessage(), rex);
			}
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
