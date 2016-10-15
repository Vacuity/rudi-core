package ai.vacuity.rudi.adaptors.hal.hao;

import java.util.Date;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.LoggerFactory;

public class SPARQLHao extends AbstractHAO {
	public final static org.slf4j.Logger logger = LoggerFactory.getLogger(AbstractHAO.class);

	@Override
	public void run() {
		UUID uuid = UUID.randomUUID();
		IRI reply = GraphManager.getValueFactory().createIRI(Constants.NS_VI, "rst-" + uuid);
		try (RepositoryConnection con = getInputProtocol().getEventHandler().getRepository().getConnection()) {
			Query q = con.prepareQuery(QueryLanguage.SPARQL, getCall());
			if (q instanceof TupleQuery) {
				try (TupleQueryResult r = ((TupleQuery) q).evaluate()) {
					Vector<Statement> results = new Vector<Statement>();
					// UUID ruuid = UUID.randomUUID();
					// IRI reply = GraphManager.getValueFactory().createIRI(Constants.NS_VI, "r-" + ruuid);
					if (r.hasNext()) {
						results.add(GraphManager.getValueFactory().createStatement(reply, GraphManager.rdf_type, GraphManager.via_Results));
					}
					while (r.hasNext()) { // iterate over the result
						BindingSet bindingSet = r.next();
						Iterator<String> capture_names = bindingSet.getBindingNames().iterator();
						UUID uuid2 = UUID.randomUUID();
						IRI result = GraphManager.getValueFactory().createIRI(Constants.NS_VI, "rst-" + uuid2);
						results.add(GraphManager.getValueFactory().createStatement(result, GraphManager.rdf_type, GraphManager.via_QueryResult));
						results.add(GraphManager.getValueFactory().createStatement(result, GraphManager.via_timestamp, GraphManager.getValueFactory().createLiteral(new Date())));
						results.add(GraphManager.getValueFactory().createStatement(result, GraphManager.via_query, inputProtocol.getEventHandler().getIri()));
						results.add(GraphManager.getValueFactory().createStatement(inputProtocol.getEventHandler().getIri(), GraphManager.getValueFactory().createIRI(Constants.NS_SIOC + "owner_of"), result));
						results.add(GraphManager.getValueFactory().createStatement(result, GraphManager.getValueFactory().createIRI(Constants.NS_SIOC + "has_container"), reply));
						while (capture_names.hasNext()) {
							String name = capture_names.next();
							if (bindingSet.getValue(name) == null) {
								logger.error("Null value for capture: " + name + "\nQuery:\n" + getCall());
							}

							// log the hit
							// UUID uuid = UUID.randomUUID();
							Literal valueLit = GraphManager.getValueFactory().createLiteral(name);

							try {
								valueLit = GraphManager.getValueFactory().createLiteral(Integer.parseInt(name)); // try to make the capture's datatype granular
							}
							catch (NumberFormatException nfex) {
							}

							UUID puuid = UUID.randomUUID();
							IRI p = GraphManager.getValueFactory().createIRI(Constants.NS_VI, "p-" + puuid);
							results.add(GraphManager.getValueFactory().createStatement(p, GraphManager.rdf_type, GraphManager.via_Projection));
							results.add(GraphManager.getValueFactory().createStatement(p, GraphManager.via_bindName, valueLit));
							results.add(GraphManager.getValueFactory().createStatement(p, GraphManager.via_value, bindingSet.getValue(name)));
							results.add(GraphManager.getValueFactory().createStatement(p, GraphManager.getValueFactory().createIRI(Constants.NS_SIOC + "has_container"), result));
						}
					}
					GraphManager.addToRepository(results, reply); // index each result under its own graph id
				}
				catch (QueryEvaluationException qex) {
					logger.error(qex.getMessage(), qex);
				}
			}
			if (q instanceof GraphQuery) {
				try (GraphQueryResult result = ((GraphQuery) q).evaluate()) {
					Vector<Statement> results = new Vector<Statement>();
					while (result.hasNext()) { // iterate over the result
						Statement st = result.next();
						Resource s = st.getSubject();
						Resource p = st.getPredicate();
						Value o = st.getObject();
						results.add(st);
						// logger.debug("Subject: " + s.stringValue());
						// logger.debug("Property: " + p.stringValue());
						// logger.debug("Value: " + o.stringValue());
					}
					results.add(GraphManager.getValueFactory().createStatement(reply, GraphManager.rdf_type, GraphManager.via_QueryResult));
					results.add(GraphManager.getValueFactory().createStatement(reply, GraphManager.via_timestamp, GraphManager.getValueFactory().createLiteral(new Date())));
					GraphManager.addToRepository(results, reply); // index the result under its own graph id
				}
				catch (QueryEvaluationException qex) {
					logger.error(qex.getMessage(), qex);
				}
			}
		}
		catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
		if (inputProtocol.getEventHandler().getResponseModule() != null) {
			inputProtocol.getEventHandler().getResponseModule().run(reply, inputProtocol, event);
			GraphManager.addToRepository(GraphManager.getValueFactory().createStatement(event.getIri(), GraphManager.getValueFactory().createIRI(Constants.NS_SIOC + "has_reply"), reply), reply); // index the result under its own graph id, only notify user after response module has had an opportunity to modify the graph
		}
	}

}
