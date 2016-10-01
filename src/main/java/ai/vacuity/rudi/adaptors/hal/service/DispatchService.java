package ai.vacuity.rudi.adaptors.hal.service;

import java.io.IOException;
import java.util.Vector;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.slf4j.LoggerFactory;

import ai.vacuity.rudi.adaptors.bo.InputProtocol;
import ai.vacuity.rudi.adaptors.hal.hao.AbstractHAO;
import ai.vacuity.rudi.adaptors.hal.hao.Constants;
import ai.vacuity.rudi.adaptors.hal.hao.RestfulHAO;
import ai.vacuity.rudi.adaptors.hal.hao.SparqlHAO;
import ai.vacuity.rudi.adaptors.interfaces.IEvent;
import ai.vacuity.rudi.adaptors.interfaces.impl.AbstractTemplateProcessor;

public class DispatchService {
	public final static org.slf4j.Logger logger = LoggerFactory.getLogger(DispatchService.class);

	public static void dispatch(IEvent event) throws IOException, IllegalArgumentException {
		find_matches: for (InputProtocol ip : SparqlHAO.getInputs()) {
			if (ip == null) break;
			if (ip.hasSparqlQuery()) continue; // don't match alerts
			String call = ip.getEventHandler().getCall();
			String log = ip.getEventHandler().getLog();
			AbstractHAO hao = null;

			if (ip.getEventHandler().hasSparqlQuery()) {
				call = ip.getEventHandler().getSparql();
				call = AbstractTemplateProcessor.process(ip, event, call);
				hao = new SparqlHAO();
			}
			else {
				call = AbstractTemplateProcessor.process(ip, event, call);
				hao = new RestfulHAO();
			}
			if (call == null) continue find_matches;
			log = AbstractTemplateProcessor.process(ip, event, log);

			logger.debug("[Rudi]: " + log);

			DispatchService.index(event);

			hao.setCall(call);
			hao.setInputProtocol(ip);
			hao.setEvent(event);
			hao.run();
		}
	}

	// TODO need to merge this with the other dispatch() method
	public static void dispatch(int id) throws IOException, IllegalArgumentException {
		find_matches: for (InputProtocol ip : SparqlHAO.getInputs()) {
			if (ip == null) break;
			if (!ip.hasSparqlQuery()) continue; // only match alerts

			if (ip.getQuery().getOwnerIri() == null) { // assume the event notifies an eventhandler
				String call = ip.getEventHandler().getCall();
				AbstractHAO hao = null;

				if (ip.getEventHandler().hasSparqlQuery()) {
					call = ip.getEventHandler().getSparql();
					call = AbstractTemplateProcessor.process(ip, id, call);
					hao = new SparqlHAO();
				}
				else {
					call = AbstractTemplateProcessor.process(ip, id, call);
					hao = new RestfulHAO();
				}
				if (call == null) continue find_matches;

				String log = ip.getEventHandler().getLog();
				log = AbstractTemplateProcessor.process(ip, id, log);
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
		Vector<Statement> st = new Vector<Statement>();
		IRI foaf_made = SparqlHAO.getValueFactory().createIRI(Constants.NS_FOAF, "made");
		IRI rdf_type = SparqlHAO.getValueFactory().createIRI(Constants.NS_RDF, "type");
		IRI via_channel = SparqlHAO.getValueFactory().createIRI(Constants.NS_VIA, "Channel");
		IRI rdfs_label = SparqlHAO.getValueFactory().createIRI(Constants.NS_RDFS, "label");
		IRI owner = event.getOwnerIri();
		if (owner == null) owner = SparqlHAO.getValueFactory().createIRI(Constants.CONTEXT_DEMO);

		st.add(SparqlHAO.getValueFactory().createStatement(owner, foaf_made, event.getIri()));
		st.add(SparqlHAO.getValueFactory().createStatement(event.getIri(), rdf_type, via_channel));
		st.add(SparqlHAO.getValueFactory().createStatement(event.getIri(), rdfs_label, SparqlHAO.getValueFactory().createLiteral(event.getLabel())));
		SparqlHAO.addToRepository(st, SparqlHAO.getValueFactory().createIRI(Constants.CONTEXT_DEMO));
	}

}
