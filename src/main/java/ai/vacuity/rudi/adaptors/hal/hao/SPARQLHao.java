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
import org.slf4j.LoggerFactory;

public class SPARQLHao extends AbstractHAO {
	public final static org.slf4j.Logger logger = LoggerFactory.getLogger(AbstractHAO.class);

	@Override
	public void run() {
		Query q = getInputProtocol().getEventHandler().getRepository().getConnection().prepareQuery(QueryLanguage.SPARQL, getCall());
		if (q instanceof TupleQuery) {
			try (TupleQueryResult r = ((TupleQuery) q).evaluate()) {
				Vector<Statement> results = new Vector<Statement>();
				UUID ruuid = UUID.randomUUID();
				IRI reply = GraphMaster.getValueFactory().createIRI(Constants.NS_VI, "r-" + ruuid);
				if (r.hasNext()) {
					results.add(GraphMaster.getValueFactory().createStatement(event.getIri(), GraphMaster.getValueFactory().createIRI(Constants.NS_SIOC + "has_reply"), reply));
				}
				while (r.hasNext()) { // iterate over the result
					BindingSet bindingSet = r.next();
					Iterator<String> capture_names = bindingSet.getBindingNames().iterator();
					UUID uuid2 = UUID.randomUUID();
					IRI result = GraphMaster.getValueFactory().createIRI(Constants.NS_VI, "rst-" + uuid2);
					results.add(GraphMaster.getValueFactory().createStatement(result, GraphMaster.rdf_type, GraphMaster.via_QueryResult));
					results.add(GraphMaster.getValueFactory().createStatement(result, GraphMaster.via_timestamp, GraphMaster.getValueFactory().createLiteral(new Date())));
					results.add(GraphMaster.getValueFactory().createStatement(result, GraphMaster.via_query, inputProtocol.getEventHandler().getIri()));
					results.add(GraphMaster.getValueFactory().createStatement(result, GraphMaster.getValueFactory().createIRI(Constants.NS_SIOC + "has_creator"), inputProtocol.getEventHandler().getIri()));
					results.add(GraphMaster.getValueFactory().createStatement(result, GraphMaster.getValueFactory().createIRI(Constants.NS_SIOC + "has_container"), reply));
					while (capture_names.hasNext()) {
						String name = capture_names.next();
						if (bindingSet.getValue(name) == null) {
							logger.error("Null value for capture: " + name + "\nQuery:\n" + getCall());
						}

						// log the hit
						// UUID uuid = UUID.randomUUID();
						Literal valueLit = GraphMaster.getValueFactory().createLiteral(name);

						try {
							valueLit = GraphMaster.getValueFactory().createLiteral(Integer.parseInt(name)); // try to make the capture's datatype granular
						}
						catch (NumberFormatException nfex) {
						}

						UUID puuid = UUID.randomUUID();
						IRI p = GraphMaster.getValueFactory().createIRI(Constants.NS_VI, "p-" + puuid);
						results.add(GraphMaster.getValueFactory().createStatement(p, GraphMaster.rdf_type, GraphMaster.via_Projection));
						results.add(GraphMaster.getValueFactory().createStatement(p, GraphMaster.via_bindName, valueLit));
						results.add(GraphMaster.getValueFactory().createStatement(p, GraphMaster.via_value, bindingSet.getValue(name)));
						results.add(GraphMaster.getValueFactory().createStatement(p, GraphMaster.getValueFactory().createIRI(Constants.NS_SIOC + "has_container"), result));
					}
				}
				GraphMaster.addToRepository(results, reply); // index each result under its own graph id
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
				UUID uuid = UUID.randomUUID();
				IRI reply = GraphMaster.getValueFactory().createIRI(Constants.NS_VI, "rst-" + uuid);
				results.add(GraphMaster.getValueFactory().createStatement(reply, GraphMaster.rdf_type, GraphMaster.via_QueryResult));
				results.add(GraphMaster.getValueFactory().createStatement(reply, GraphMaster.via_timestamp, GraphMaster.getValueFactory().createLiteral(new Date())));
				results.add(GraphMaster.getValueFactory().createStatement(event.getIri(), GraphMaster.getValueFactory().createIRI(Constants.NS_SIOC + "has_reply"), reply));
				GraphMaster.addToRepository(results, reply); // index the result under its own graph id
			}
			catch (QueryEvaluationException qex) {
				logger.error(qex.getMessage(), qex);
			}
		}
		// UUID uuid = UUID.randomUUID();
		// String fxmlStr = "/Users/smonroe/workspace/rudi-adaptors/src/main/webapp/WEB-INF/resources/adaptors/" + new GenericUrl(getInputProtocol().getResponseProtocol().getCall()).getHost() + "-" + uuid + ".rdf";
		// File fxml = new File(fxmlStr);
		// fxml.createNewFile();
		// FileWriter fw = new FileWriter(fxml);
		// fw.write(resp);
		// fw.close();
	}

}
