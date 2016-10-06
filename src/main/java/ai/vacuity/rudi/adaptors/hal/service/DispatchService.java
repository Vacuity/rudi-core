package ai.vacuity.rudi.adaptors.hal.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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
import ai.vacuity.rudi.adaptors.bo.InputProtocol;
import ai.vacuity.rudi.adaptors.hal.hao.AbstractHAO;
import ai.vacuity.rudi.adaptors.hal.hao.Constants;
import ai.vacuity.rudi.adaptors.hal.hao.GraphMaster;
import ai.vacuity.rudi.adaptors.hal.hao.RestfulHAO;
import ai.vacuity.rudi.adaptors.hal.hao.SPARQLHao;
import ai.vacuity.rudi.adaptors.interfaces.IEvent;
import ai.vacuity.rudi.adaptors.interfaces.impl.AbstractTemplateModule;

public class DispatchService extends Thread {

	public DispatchService() {
		start(); // start dispatching events
	}

	private final static DispatchService dispatcher = new DispatchService();
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
			if (getQueue().size() <= 0) continue;
			getQueue().get(0).run();
			getQueue().remove(0);
		}
	}

	// TODO need to merge this with the other dispatch() method
	public static void dispatch(int id, Resource context) throws IOException, IllegalArgumentException {
		ArrayList<String> logs = new ArrayList<String>();
		find_matches: for (InputProtocol ip : GraphMaster.getQueryPatterns()) {
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
				GraphMaster.removeFromInbox(context);
			}
			else {
				logger.debug("[Rudi]: Notifying the delegate '" + ip.getQuery().getOwnerIri() + "'");
			}
			index(ip.getQuery());
		}
	}

	public static List<String> dispatch(IEvent event) throws IOException, IllegalArgumentException {
		index(event);

		ArrayList<String> logs = new ArrayList<String>();
		InputProtocol[] protocols = GraphMaster.getTypedPatterns();
		find_matches: for (int i = 0; i < protocols.length; i++) {
			InputProtocol ip = protocols[i];
			// try regex patterns only if no typed patterns matched
			if (ip == null) {
				if (protocols.equals(GraphMaster.getTypedPatterns())) {
					protocols = GraphMaster.getRegexPatterns();
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
		if (ip.getDataType().equals(InputProtocol.PARSE_TYPE_REGEX) && ip.getPattern() instanceof Pattern) {
			Matcher matcher = ((Pattern) ip.getPattern()).matcher(event.getLabel());
			while (matcher.find()) {
				if (!found) {
					// logger.debug("Match: " + ip.getTrigger().stringValue());
					found = true;
				}
				int groups = matcher.groupCount();
				for (int gp = 1; gp <= groups; gp++) {
					// logger.debug("group " + gp + ": " + matcher.group(gp));
					for (int i = 0; i < templates.length; i++)
						templates[i] = templates[i].replace("${" + gp + "}", matcher.group(gp));
				}
				for (int i = 0; i < templates.length; i++)
					templates[i] = templates[i].replace("${0}", matcher.group());
			}
		}

		// SECOND MATCH GATE
		if (ip.getDataType().equals(GraphMaster.getValueFactory().createIRI("http://www.vacuity.ai/onto/via/1.0/URL"))) {
			try {
				new URL(event.getLabel());
				found = true;
			}
			catch (MalformedURLException mfuex) {
				return new String[0];
			}
		}
		if (ip.getDataType().equals(GraphMaster.getValueFactory().createIRI("http://www.w3.org/2001/XMLSchema#anyURI"))) {
			try {
				GraphMaster.getValueFactory().createIRI(event.getLabel());
				found = true;
			}
			catch (IllegalArgumentException iaex) {
				return new String[0];
			}
		}
		if (ip.getDataType().equals(GraphMaster.getValueFactory().createIRI("http://www.w3.org/2001/XMLSchema#integer"))) {
			try {
				Integer.parseInt(event.getLabel());
				found = true;
			}
			catch (NumberFormatException nfex) {
				return new String[0];
			}
		}
		if (ip.getDataType().equals(GraphMaster.getValueFactory().createIRI("http://www.w3.org/2001/XMLSchema#dateTime"))) {
			try {
				// for type dateTime, the value is the date format
				new SimpleDateFormat(((Value) ip.getPattern()).stringValue());
				found = true;
			}
			catch (NumberFormatException nfex) {
				return new String[0];
			}
		}

		if (!found) return new String[0];

		// 2. run the call processor (give customer processors priority over system processor)
		// TODO should the log include the unprocessed placeholder value?

		for (int i = 0; i < templates.length; i++) {
			if (Config.getMap().get(ip.getEventHandler().getConfigLabel()).hasTemplateProcessor()) {

				Config.getMap().get(ip.getEventHandler().getConfigLabel()).getTemplateProcessor().process(templates[i], event);
				templates[i] = Config.getMap().get(ip.getEventHandler().getConfigLabel()).getTemplateProcessor().getTemplate();

				// allow the template processor to transform user input prior to user input being filled in template
				event = Config.getMap().get(ip.getEventHandler().getConfigLabel()).getTemplateProcessor().getEvent();
			}
			if (StringUtils.isNotBlank(templates[i])) templates[i] = templates[i].replace("${" + ip.getCaptureIndex() + "}", event.getLabel());

			// 3. swap any remaining reserved placed holders
			if (Config.getMap().get(ip.getEventHandler().getConfigLabel()).hasKey()) templates[i] = templates[i].replace("${key}", Config.getMap().get(ip.getEventHandler().getConfigLabel()).getKey());
			if (Config.getMap().get(ip.getEventHandler().getConfigLabel()).hasId()) templates[i] = templates[i].replace("${id}", Config.getMap().get(ip.getEventHandler().getConfigLabel()).getId());
			if (Config.getMap().get(ip.getEventHandler().getConfigLabel()).hasToken()) templates[i] = templates[i].replace("${token}", Config.getMap().get(ip.getEventHandler().getConfigLabel()).getToken());
		}

		return templates;
	}

	private static String[] process(InputProtocol ip, int id, Resource context, String... templates) {
		if (ip.getQuery().getId() != id) return null;
		String query = ip.getQuery().getDelegate().toString().replace("\n", "").replace("\t", "");

		logger.debug("Context: " + context);

		TupleQuery alertQuery = GraphMaster.getConnection().prepareTupleQuery(QueryLanguage.SPARQL, query);
		alertQuery.setBinding("context", context);
		try (TupleQueryResult alerts = alertQuery.evaluate()) {
			if (!alerts.hasNext()) return new String[0];
			UUID u = UUID.randomUUID();
			IRI alertIRI = GraphMaster.getValueFactory().createIRI(Constants.NS_VI, "alrt-" + u);
			Vector<Statement> tuples = new Vector<Statement>();
			if (alerts.hasNext()) {
				Resource owner = ip.getQuery().getOwnerIri();
				if (owner == null) owner = ip.getEventHandler().getIri();
				if (owner == null) owner = GraphMaster.getRepository().getValueFactory().createIRI(Constants.CONTEXT_DEMO);
				tuples.add(GraphMaster.getValueFactory().createStatement(alertIRI, GraphMaster.getValueFactory().createIRI(Constants.NS_SIOC + "has_owner"), owner));
				tuples.add(GraphMaster.getValueFactory().createStatement(alertIRI, GraphMaster.getValueFactory().createIRI(Constants.NS_VIA + "context"), context));
			}
			for (int j = 0; alerts.hasNext(); j++) {
				try {
					UUID uuid = UUID.randomUUID();
					IRI hit = GraphMaster.getValueFactory().createIRI(Constants.NS_VI, "hit-" + uuid);
					BindingSet captures = alerts.next();
					Iterator<String> capture_names = captures.getBindingNames().iterator();
					tuples.add(GraphMaster.getValueFactory().createStatement(hit, GraphMaster.rdf_type, GraphMaster.via_Hit));
					tuples.add(GraphMaster.getValueFactory().createStatement(hit, GraphMaster.getValueFactory().createIRI(Constants.NS_SIOC + "has_container"), alertIRI));
					tuples.add(GraphMaster.getValueFactory().createStatement(hit, GraphMaster.via_timestamp, GraphMaster.getValueFactory().createLiteral(new Date())));
					tuples.add(GraphMaster.getValueFactory().createStatement(hit, GraphMaster.via_query, ip.getQuery().getIri()));
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

						Literal valueLit = GraphMaster.getValueFactory().createLiteral(name);

						try {
							valueLit = GraphMaster.getValueFactory().createLiteral(Integer.parseInt(name)); // try to make the capture's datatype granular
						}
						catch (NumberFormatException nfex) {
						}
						UUID puuid = UUID.randomUUID();
						IRI p = GraphMaster.getValueFactory().createIRI(Constants.NS_VI, "p-" + puuid);
						tuples.add(GraphMaster.getValueFactory().createStatement(p, GraphMaster.rdf_type, GraphMaster.via_Projection));
						tuples.add(GraphMaster.getValueFactory().createStatement(p, GraphMaster.via_bindName, valueLit));
						tuples.add(GraphMaster.getValueFactory().createStatement(p, GraphMaster.via_value, captures.getValue(name)));
						tuples.add(GraphMaster.getValueFactory().createStatement(p, GraphMaster.getValueFactory().createIRI(Constants.NS_SIOC + "has_container"), hit));
					}
				}
				catch (Exception e) {
					logger.error(e.getMessage(), e);
					continue;
				}
				GraphMaster.addToRepository(tuples, alertIRI);
			}
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
			return new String[0];
		}
		for (int i = 0; i < templates.length; i++) {
			if (Config.getMap().get(ip.getEventHandler().getConfigLabel()).hasTemplateProcessor()) {
				Config.getMap().get(ip.getEventHandler().getConfigLabel()).getTemplateProcessor().process(templates[i], ip.getQuery());
				templates[i] = Config.getMap().get(ip.getEventHandler().getConfigLabel()).getTemplateProcessor().getTemplate();

				// allow the template processor to transform user input prior to user input being filled in template
				ip.getQuery().setLabel(Config.getMap().get(ip.getEventHandler().getConfigLabel()).getTemplateProcessor().getEvent().getLabel());
			}
			if (StringUtils.isNotBlank(templates[i])) templates[i] = templates[i].replace("${" + ip.getCaptureIndex() + "}", ip.getQuery().getLabel());

			// 3. swap any remaining reserved placed holders
			if (Config.getMap().get(ip.getEventHandler().getConfigLabel()).hasKey()) templates[i] = templates[i].replace("${key}", Config.getMap().get(ip.getEventHandler().getConfigLabel()).getKey());
			if (Config.getMap().get(ip.getEventHandler().getConfigLabel()).hasId()) templates[i] = templates[i].replace("${id}", Config.getMap().get(ip.getEventHandler().getConfigLabel()).getId());
			if (Config.getMap().get(ip.getEventHandler().getConfigLabel()).hasToken()) templates[i] = templates[i].replace("${token}", Config.getMap().get(ip.getEventHandler().getConfigLabel()).getToken());
		}

		return templates;
	}

	public static void index(IEvent event) {
		Vector<Statement> tuples = new Vector<Statement>();
		IRI sioc_owner_of = GraphMaster.getValueFactory().createIRI(Constants.NS_SIOC, "owner_of");
		IRI rdf_type = GraphMaster.getValueFactory().createIRI(Constants.NS_RDF, "type");
		IRI via_channel = GraphMaster.getValueFactory().createIRI(Constants.NS_VIA, "Channel");
		IRI rdfs_label = GraphMaster.getValueFactory().createIRI(Constants.NS_RDFS, "label");
		IRI owner = event.getOwnerIri();
		if (owner == null) owner = GraphMaster.getValueFactory().createIRI(Constants.CONTEXT_DEMO);

		tuples.add(GraphMaster.getValueFactory().createStatement(owner, sioc_owner_of, event.getIri()));
		tuples.add(GraphMaster.getValueFactory().createStatement(event.getIri(), GraphMaster.via_timestamp, GraphMaster.getValueFactory().createLiteral(new Date())));
		tuples.add(GraphMaster.getValueFactory().createStatement(event.getIri(), rdf_type, via_channel));
		tuples.add(GraphMaster.getValueFactory().createStatement(event.getIri(), rdfs_label, GraphMaster.getValueFactory().createLiteral(event.getLabel())));
		GraphMaster.addToRepository(tuples, GraphMaster.getValueFactory().createIRI(Constants.CONTEXT_DEMO));
	}

	private static Vector<AbstractHAO> getQueue() {
		return DispatchService.queue;
	}

	private static void add(AbstractHAO hao) {
		getQueue().add(hao);
	}

}
