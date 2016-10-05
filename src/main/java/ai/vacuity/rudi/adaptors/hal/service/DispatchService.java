package ai.vacuity.rudi.adaptors.hal.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.slf4j.LoggerFactory;

import ai.vacuity.rudi.adaptors.bo.InputProtocol;
import ai.vacuity.rudi.adaptors.hal.hao.AbstractHAO;
import ai.vacuity.rudi.adaptors.hal.hao.Constants;
import ai.vacuity.rudi.adaptors.hal.hao.GraphMaster;
import ai.vacuity.rudi.adaptors.hal.hao.RestfulHAO;
import ai.vacuity.rudi.adaptors.interfaces.IEvent;
import ai.vacuity.rudi.adaptors.interfaces.impl.AbstractTemplateProcessor;

public class DispatchService {
	public final static org.slf4j.Logger logger = LoggerFactory.getLogger(DispatchService.class);

	public static List<String> dispatch(IEvent event) throws IOException, IllegalArgumentException {
		ArrayList<String> logs = new ArrayList<String>();
		find_matches: for (InputProtocol ip : GraphMaster.getInputs()) {
			if (ip == null) break;
			if (ip.hasSparqlQuery()) continue; // don't match alerts
			String procedure = ip.getEventHandler().getCall();
			String log = ip.getEventHandler().getLog();
			AbstractHAO hao = null;

			String[] processed = new String[] {};

			if (ip.getEventHandler().hasSparqlQuery()) {
				procedure = ip.getEventHandler().getSparql();
				processed = AbstractTemplateProcessor.process(ip, event, procedure, log);
				hao = new GraphMaster();
			}
			else {
				processed = AbstractTemplateProcessor.process(ip, event, procedure, log);
				hao = new RestfulHAO();
			}
			if (processed.length == 0) continue find_matches;

			logs.add(processed[1]);
			logger.debug("[Rudi]: " + processed[1]);

			DispatchService.index(event);

			hao.setCall(processed[0]);
			hao.setInputProtocol(ip);
			hao.setEvent(event);
			hao.run();
		}
		return logs;
	}

	// TODO need to merge this with the other dispatch() method
	public static void dispatch(int id, Resource context) throws IOException, IllegalArgumentException {
		ArrayList<String> logs = new ArrayList<String>();
		find_matches: for (InputProtocol ip : GraphMaster.getInputs()) {
			if (ip == null) break;
			if (!ip.hasSparqlQuery()) continue; // only match alerts

			if (ip.getQuery().getOwnerIri() == null) { // assume the event notifies an eventhandler
				String call = ip.getEventHandler().getCall();
				AbstractHAO hao = null;

				if (ip.getEventHandler().hasSparqlQuery()) {
					call = ip.getEventHandler().getSparql();
					call = AbstractTemplateProcessor.process(ip, id, call, context);
					hao = new GraphMaster();
				}
				else {
					call = AbstractTemplateProcessor.process(ip, id, call, context);
					hao = new RestfulHAO();
				}
				if (call == null) continue find_matches;

				String log = ip.getEventHandler().getLog();
				log = AbstractTemplateProcessor.process(ip, id, log, context);
				logs.add(log);
				logger.debug("[Rudi]: " + log);

				hao.setCall(call);
				hao.setInputProtocol(ip);
				hao.setEvent(ip.getQuery()); // send a label form of the query to the response pipeline
				hao.run();
			}
			else {
				logger.debug("[Rudi]: Notifying the owner '" + ip.getQuery().getOwnerIri() + "'");
			}

			DispatchService.index(ip.getQuery());

		}
	}

	public static void index(IEvent event) {
		Vector<Statement> tuples = new Vector<Statement>();
		IRI sioc_owner_of = GraphMaster.getValueFactory().createIRI(Constants.NS_SIOC, "owner_of");
		IRI rdf_type = GraphMaster.getValueFactory().createIRI(Constants.NS_RDF, "type");
		IRI via_channel = GraphMaster.getValueFactory().createIRI(Constants.NS_VIA, "Channel");
		IRI rdfs_label = GraphMaster.getValueFactory().createIRI(Constants.NS_RDFS, "label");
		IRI dc_date = GraphMaster.getValueFactory().createIRI(Constants.NS_DC, "date");
		IRI owner = event.getOwnerIri();
		if (owner == null) owner = GraphMaster.getValueFactory().createIRI(Constants.CONTEXT_DEMO);

		tuples.add(GraphMaster.getValueFactory().createStatement(owner, sioc_owner_of, event.getIri()));
		tuples.add(GraphMaster.getValueFactory().createStatement(event.getIri(), dc_date, GraphMaster.getValueFactory().createLiteral(new Date())));
		tuples.add(GraphMaster.getValueFactory().createStatement(event.getIri(), rdf_type, via_channel));
		tuples.add(GraphMaster.getValueFactory().createStatement(event.getIri(), rdfs_label, GraphMaster.getValueFactory().createLiteral(event.getLabel())));
		GraphMaster.addToRepository(tuples, GraphMaster.getValueFactory().createIRI(Constants.CONTEXT_DEMO));
	}

}
