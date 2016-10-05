package ai.vacuity.rudi.adaptors.hal.hao;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.event.base.NotifyingRepositoryWrapper;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.slf4j.LoggerFactory;

import com.google.api.client.http.GenericUrl;
import com.google.common.net.MediaType;

import ai.vacuity.rudi.adaptors.bo.Config;
import ai.vacuity.rudi.adaptors.bo.EventHandler;
import ai.vacuity.rudi.adaptors.bo.IndexableQuery;
import ai.vacuity.rudi.adaptors.bo.InputProtocol;
import virtuoso.rdf4j.driver.VirtuosoRepository;

public class GraphMaster extends AbstractHAO {
	public final static org.slf4j.Logger logger = LoggerFactory.getLogger(GraphMaster.class);

	// static String sparqlEndpoint = "http://www.tryrudi.io/sparql";
	// static Repository repo = new SPARQLRepository(sparqlEndpoint);
	// final static File dataDir = new File(Constants.DIR_ALERTS);
	// final static String indexes = "spoc,posc,cosp";
	// static Repository repo = new SailRepository(new
	// ForwardChainingRDFSInferencer(new NativeStore(dataDir, indexes)));
	final static NotifyingRepositoryWrapper repository = new NotifyingRepositoryWrapper(GraphMaster.parseSPARQLRepository(Constants.SPARQL_ENDPOINT_VIA));
	final static GraphListener listener = new GraphListener();
	static final HashMap<String, TupleQuery> queries = new HashMap<String, TupleQuery>();

	static final String QUERY_GET_LISTENER = "get_listener";
	static final String QUERY_GET_SUBSCRIPTIONS = "get_subscriptions";
	static final String QUERY_GET_ADAPTOR_WITH_CAPTURE = "get_adaptor_with_capture";
	static final String QUERY_GET_ADAPTOR_OUTPUT = "get_adaptor_output";
	static final String QUERY_GET_PATTERN_LABELS = "get_pattern_labels";
	static final String QUERY_GET_ENDPOINT_LABELS = "get_endpoint_labels";
	static final String QUERY_GET_QUERIES = "get_queries";
	private static final int MAX_INPUTS = 100000;
	private static final InputProtocol[] inputs = new InputProtocol[GraphMaster.MAX_INPUTS];

	static {
		GraphMaster.getRepository().initialize();
		GraphMaster.loadRepository(); // load rdf demo instance data, patterns,
										// and schema
		GraphMaster.load(GraphMaster.QUERY_GET_LISTENER); // load adaptors
		GraphMaster.load(GraphMaster.QUERY_GET_SUBSCRIPTIONS); // load adaptors

		GraphMaster.getRepository().addRepositoryConnectionListener(listener);

		// QuadStore.load(QuadStore.QUERY_GET_ADAPTOR_OUTPUT);
		// QuadStore.load(QuadStore.QUERY_GET_ENDPOINT_LABELS);
		// QuadStore.load(QuadStore.QUERY_GET_PATTERN_LABELS);
		// QuadStore.load(QuadStore.QUERY_GET_QUERIES);
	}

	public final static IRI rdf_type = getValueFactory().createIRI(Constants.NS_RDF, "type");

	public final static IRI via_bindName = getValueFactory().createIRI(Constants.NS_VIA, "bindName");

	public final static IRI via_value = getValueFactory().createIRI(Constants.NS_VIA, "value");

	public final static IRI via_query = getValueFactory().createIRI(Constants.NS_VIA, "query");

	public final static IRI dc_date = getValueFactory().createIRI(Constants.NS_DC, "date");

	public final static IRI via_Alert = getValueFactory().createIRI(Constants.NS_VIA, "Alert");

	public final static IRI via_Hit = getValueFactory().createIRI(Constants.NS_VIA, "Hit");

	public final static IRI via_QueryResult = getValueFactory().createIRI(Constants.NS_VIA, "QueryResult");

	public final static IRI via_Projection = getValueFactory().createIRI(Constants.NS_VIA, "Projection");

	public static InputProtocol[] getInputs() {
		return inputs;
	}

	private static int inputsCursor = 0;

	public static int getInputsCursor() {
		return inputsCursor;
	}

	public static void setInputsCursor(int inputsCursor) {
		GraphMaster.inputsCursor = inputsCursor;
	}

	private static void incrementInputsCursor() {
		GraphMaster.inputsCursor++;
	}

	public static NotifyingRepositoryWrapper getRepository() {
		return repository;
	}

	public static RepositoryConnection getConnection() {
		return getRepository().getConnection();
	}

	public static ValueFactory getValueFactory() {
		return getRepository().getValueFactory();
	}

	public static void load() {
		// BindingSet queries = query();
		try (RepositoryConnection con = getConnection()) {
			// First, prepare a query that retrieves all names of persons
			TupleQuery nameQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT ?name WHERE { ?person ex:name ?name . }");
			// Then, prepare another query that retrieves all e-mail addresses
			// of persons:
			TupleQuery mailQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT ?mail WHERE { ?person ex:mail ?mail ; ex:name ?name . }");
			// Evaluate the first query to get all names
			try (TupleQueryResult nameResult = nameQuery.evaluate()) {
				// Loop over all names, and retrieve the corresponding e-mail
				// address.
				while (nameResult.hasNext()) {
					BindingSet bindingSet = nameResult.next();
					Value name = bindingSet.getValue("name");
					// Retrieve the matching mailbox, by setting the binding for
					// the variable 'name' to the retrieved value. Note that we
					// can set the same binding name again for each iteration,
					// it will
					// overwrite the previous setting.
					mailQuery.setBinding("name", name);
					try (TupleQueryResult mailResult = mailQuery.evaluate()) {
						// mailResult now contains the e-mail addresses for one
						// particular person

						// ....
					}

				}
			}
		}
	}

	public static void getQueries() {
		try (RepositoryConnection con = getConnection()) {
			TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT ?q ?l ?s WHERE { ?q rdf:type <http://www.vacuity.ai/onto/via/1.0/TupleQuery> . ?q rdfs:label ?l . ?q <http://www.vacuity.ai/onto/via/1.0/sparql> ?s . }");
			// tupleQuery.setBinding("label",
			// getValueFactory().createLiteral("get_queries"));
			try (TupleQueryResult result = tupleQuery.evaluate()) {
				while (result.hasNext()) { // iterate over the result
					BindingSet bindingSet = result.next();
					Value q = bindingSet.getValue("q");
					Value label = bindingSet.getValue("l");
					Value sparql = bindingSet.getValue("s");
					logger.debug("Value: " + q.stringValue());
					logger.debug("Label: " + label.stringValue());
					logger.debug("SPARQL: " + sparql.stringValue());
					// do something interesting with the values here...
				}
			}
			catch (QueryEvaluationException qex) {
				logger.error(qex.getMessage(), qex);
			}
		}
		catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
	}

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
					results.add(GraphMaster.getValueFactory().createStatement(result, GraphMaster.dc_date, GraphMaster.getValueFactory().createLiteral(new Date())));
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
				results.add(GraphMaster.getValueFactory().createStatement(reply, GraphMaster.dc_date, GraphMaster.getValueFactory().createLiteral(new Date())));
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

	static void loadRepository() {
		try (RepositoryConnection con = getConnection()) {
			String[] extensions = new String[] { "rdf", "rdfs" };
			// IOFileFilter filter = new SuffixFileFilter(extensions, IOCase.INSENSITIVE);
			Iterator<File> iter = FileUtils.iterateFiles(new File(Constants.DIR_LISTENERS), extensions, true);
			Resource context = getValueFactory().createIRI(Constants.CONTEXT_VIA);
			con.clear(context);
			con.begin();
			while (iter.hasNext()) {
				File f = iter.next();
				logger.debug("Loading file: " + f.getName());
				try {
					con.add(f, null, RDFFormat.RDFXML, context);
				}
				catch (RDF4JException e) {
					logger.error(e.getMessage(), e);
				}
				catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
			con.commit();
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public static void load(String queryLabel) {
		Config.load();
		try (RepositoryConnection con = getConnection()) {
			TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT ?q ?l ?s WHERE { ?q rdf:type <http://www.vacuity.ai/onto/via/1.0/TupleQuery> . ?q rdfs:label ?l . ?q <http://www.vacuity.ai/onto/via/1.0/sparql> ?s . }");
			tupleQuery.setBinding("l", getValueFactory().createLiteral(queryLabel));
			try (TupleQueryResult result = tupleQuery.evaluate()) {
				while (result.hasNext()) { // iterate over the result
					BindingSet bindingSet = result.next();
					Value q = bindingSet.getValue("q");
					Value label = bindingSet.getValue("l");
					Value sparql = bindingSet.getValue("s");
					logger.debug("Value: " + q.stringValue());
					logger.debug("Label: " + label.stringValue());
					logger.debug("SPARQL: " + sparql.stringValue());

					TupleQuery data = con.prepareTupleQuery(QueryLanguage.SPARQL, sparql.stringValue());
					switch (queryLabel) {
					case GraphMaster.QUERY_GET_LISTENER:
						// data.setBinding("patternPropertyType", getValueFactory().createIRI("http://www.vacuity.ai/onto/via/pattern"));
						try (TupleQueryResult r2 = data.evaluate()) {
							Vector<Exception> err = new Vector<Exception>();
							while (r2.hasNext()) {
								BindingSet bs2 = r2.next();
								logger.debug("Listener: " + bs2.getValue("listener"));
								logger.debug("Pattern: " + bs2.getValue("pattern"));
								logger.debug("Label: " + bs2.getValue("i_label"));
								logger.debug("Input Query: " + bs2.getValue("i_query"));
								logger.debug("Input Query Label: " + bs2.getValue("i_query_label"));
								logger.debug("Event Handler: " + bs2.getValue("output"));
								logger.debug("Config: " + bs2.getValue("config"));
								logger.debug("Translator: " + bs2.getValue("translator"));
								logger.debug("Log: " + bs2.getValue("log"));
								logger.debug("Reply Type Property: " + bs2.getValue("replyTypeProp"));
								logger.debug("Call: " + bs2.getValue("call"));
								logger.debug("Media Type: " + bs2.getValue("mediaType"));
								logger.debug("Response Query: " + bs2.getValue("query"));

								// TODO a hack validator in lieu of a better query semantics
								if (bs2.getValue("pattern") == null && bs2.getValue("i_query") == null && bs2.getValue("i_label") == null) {
									IllegalArgumentException ilaex = new IllegalArgumentException("Error on query: " + bs2.getValue("input") + ". via:Input must contain a via:pattern, via:capture_*, or via:query predicate.");
									logger.error(ilaex.getMessage(), ilaex);
									continue;
								}

								if (bs2.getValue("i_query") != null && !(bs2.getValue("i_query") instanceof IRI)) {
									IllegalArgumentException ilaex = new IllegalArgumentException("Error on query: " + bs2.getValue("i_query") + ". via:Query must be a IRI resource.");
									logger.error(ilaex.getMessage(), ilaex);
									continue;
								}

								InputProtocol i = new InputProtocol();
								if (bs2.getValue("pattern") != null) {
									Literal pattern = (Literal) bs2.getValue("pattern");
									i.setTrigger(pattern);
									i.setDataType(pattern.getDatatype());
									if (pattern.getDatatype().equals(InputProtocol.PARSE_TYPE_REGEX)) i.setPattern(Pattern.compile(i.getTrigger().stringValue()));
									else i.setPattern(i.getTrigger());
								}
								i.setLabel((Literal) bs2.getValue("i_label"));

								if (bs2.getValue("capturePropertyType") != null) {
									String pptStr = ((IRI) bs2.getValue("capturePropertyType")).stringValue();
									int captureIndex = Integer.parseInt(pptStr.substring(pptStr.lastIndexOf("_") + 1));
									i.setCaptureIndex(captureIndex);
								}

								// event handlers are loaded only if they are called by a listeners, see the vi:get_listener SPARQL query
								EventHandler handler = new EventHandler();
								handler.setConfigLabel(bs2.getValue("config").stringValue());
								handler.setLog(bs2.getValue("log").stringValue());
								handler.setContentType(MediaType.parse(bs2.getValue("mediaType").stringValue()));
								handler.setIri((IRI) bs2.getValue("output"));
								if (bs2.getValue("translator") != null) handler.setTranslator(new GenericUrl(bs2.getValue("translator").stringValue()));
								handler.setCall(bs2.getValue("call").stringValue());
								// handler.setCall(Config.getSettings().getProperty(handler.getConfigLabel() + Config.PROPERTY_SUFFIX_URL) + bs2.getValue("call").stringValue());
								i.setEventHandler(handler);

								// if sparql
								// Create a new Repo, set its URL to the value o.getCall()
								// retrive query resource from repo
								if (bs2.getValue("i_query") != null) {
									IRI query = (IRI) bs2.getValue("i_query");
									TupleQuery ipQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT ?q ?l ?s WHERE { ?q rdf:type <http://www.vacuity.ai/onto/via/1.0/TupleQuery> . ?q <http://www.vacuity.ai/onto/via/1.0/sparql> ?s . }");
									ipQuery.setBinding("q", query);
									try (TupleQueryResult r3 = ipQuery.evaluate()) {
										if (r3.hasNext()) { // expect a single result
											BindingSet bs3 = r3.next();
											String queryStr = bs3.getValue("s").stringValue();
											IndexableQuery iq = new IndexableQuery(con.prepareTupleQuery(QueryLanguage.SPARQL, queryStr));
											iq.setId(bs2.getValue("i_query").stringValue().hashCode());
											iq.setLabel(bs2.getValue("i_query_label").stringValue());
											iq.setIri((IRI) bs2.getValue("i_query"));
											GraphListener.register(iq);
											i.setQuery(iq);
											// i.hasSparqlQuery(true);
										}
									}
									catch (QueryEvaluationException qex) {
										logger.error(qex.getMessage(), qex);
									}
								}

								if (bs2.getValue("query") != null) {
									IRI query = (IRI) bs2.getValue("query");
									TupleQuery rpQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT ?q ?l ?s WHERE { ?q rdf:type <http://www.vacuity.ai/onto/via/1.0/TupleQuery> . ?q <http://www.vacuity.ai/onto/via/1.0/sparql> ?s . }");
									rpQuery.setBinding("q", query);
									try (TupleQueryResult r3 = rpQuery.evaluate()) {
										if (r3.hasNext()) { // expect a single result
											BindingSet bs3 = r3.next();
											handler.setSparql(bs3.getValue("s").stringValue());
											handler.setRepository(GraphMaster.parseSPARQLRepository(handler.getCall(), bs2.getValue("config").stringValue()));
											handler.hasSparqlQuery(true);
										}
									}
									catch (QueryEvaluationException qex) {
										logger.error(qex.getMessage(), qex);
									}
								}

								// do o.setSparql() for the sparql of the query URI
								// set o.isSparqlEndpoint = true

								getInputs()[getInputsCursor()] = i;
								incrementInputsCursor();
							}
						}
						catch (QueryEvaluationException qex) {
							logger.error(qex.getMessage(), qex);
						}
						catch (IllegalArgumentException iaex) {
							logger.error(iaex.getMessage(), iaex);
						}
						break;
					case GraphMaster.QUERY_GET_SUBSCRIPTIONS:
						// data.setBinding("patternPropertyType", getValueFactory().createIRI("http://www.vacuity.ai/onto/via/pattern"));
						try (TupleQueryResult r2 = data.evaluate()) {
							Vector<Exception> err = new Vector<Exception>();
							while (r2.hasNext()) {
								BindingSet bs2 = r2.next();
								logger.debug("Listener: " + bs2.getValue("listener"));
								logger.debug("Pattern: " + bs2.getValue("pattern"));
								logger.debug("Label: " + bs2.getValue("i_label"));
								logger.debug("Input Query: " + bs2.getValue("i_query"));
								logger.debug("Input Query Label: " + bs2.getValue("i_query_label"));
								logger.debug("Notifies: " + bs2.getValue("notifies"));
								// logger.debug("Config: " + bs2.getValue("config"));
								// logger.debug("Translator: " + bs2.getValue("translator"));
								// logger.debug("Log: " + bs2.getValue("log"));
								// logger.debug("Reply Type Property: " + bs2.getValue("replyTypeProp"));
								// logger.debug("Call: " + bs2.getValue("call"));
								// logger.debug("Media Type: " + bs2.getValue("mediaType"));
								// logger.debug("Response Query: " + bs2.getValue("query"));

								// TODO a hack validator in lieu of a better query semantics
								// if (bs2.getValue("i_query") == null && bs2.getValue("i_label") == null) {
								// IllegalArgumentException ilaex = new IllegalArgumentException("Error on query: " + bs2.getValue("input") + ". via:Input must contain a via:pattern, via:capture_*, or via:query predicate.");
								// logger.error(ilaex.getMessage(), ilaex);
								// continue;
								// }

								if (bs2.getValue("i_query") != null && !(bs2.getValue("i_query") instanceof IRI)) {
									IllegalArgumentException ilaex = new IllegalArgumentException("Error on query: " + bs2.getValue("i_query") + ". via:Query must be a IRI resource.");
									logger.error(ilaex.getMessage(), ilaex);
									continue;
								}

								InputProtocol i = new InputProtocol();
								// if (bs2.getValue("pattern") != null) {
								// Literal pattern = (Literal) bs2.getValue("pattern");
								// i.setTrigger(pattern);
								// i.setDataType(pattern.getDatatype());
								// if (pattern.getDatatype().equals(InputProtocol.PARSE_TYPE_REGEX)) i.setPattern(Pattern.compile(i.getTrigger().stringValue()));
								// else i.setPattern(i.getTrigger());
								// }
								// i.setLabel((Literal) bs2.getValue("i_label"));

								if (bs2.getValue("capturePropertyType") != null) {
									String pptStr = ((IRI) bs2.getValue("capturePropertyType")).stringValue();
									int captureIndex = Integer.parseInt(pptStr.substring(pptStr.lastIndexOf("_") + 1));
									i.setCaptureIndex(captureIndex);
								}

								// event handlers are loaded only if they are called by a listeners, see the vi:get_listener SPARQL query
								// EventHandler handler = new EventHandler();
								// handler.setConfigLabel(bs2.getValue("config").stringValue());
								// handler.setLog(bs2.getValue("log").stringValue());
								// handler.setContentType(MediaType.parse(bs2.getValue("mediaType").stringValue()));
								// if (bs2.getValue("translator") != null) handler.setTranslator(new GenericUrl(bs2.getValue("translator").stringValue()));
								// handler.setCall(bs2.getValue("call").stringValue());
								// i.setEventHandler(handler);
								//
								// // if sparql
								// // Create a new Repo, set its URL to the value o.getCall()
								// // retrive query resource from repo
								if (bs2.getValue("i_query") != null) {
									IRI query = (IRI) bs2.getValue("i_query");
									TupleQuery ipQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT ?q ?l ?s WHERE { ?q rdf:type <http://www.vacuity.ai/onto/via/1.0/TupleQuery> . ?q <http://www.vacuity.ai/onto/via/1.0/sparql> ?s . }");
									ipQuery.setBinding("q", query);
									try (TupleQueryResult r3 = ipQuery.evaluate()) {
										if (r3.hasNext()) { // expect a single result
											BindingSet bs3 = r3.next();
											String queryStr = bs3.getValue("s").stringValue();
											IndexableQuery iq = new IndexableQuery(con.prepareTupleQuery(QueryLanguage.SPARQL, queryStr));
											iq.setId(bs2.getValue("i_query").stringValue().hashCode());
											iq.setLabel(bs2.getValue("i_query_label").stringValue());
											iq.setIri((IRI) bs2.getValue("i_query"));
											iq.setOwnerIri((IRI) bs2.getValue("notifies")); // this notifier is a non-EventHandler, see #get_subscriptions query
											GraphListener.register(iq);
											i.setQuery(iq);
											// i.hasSparqlQuery(true);
										}
									}
									catch (QueryEvaluationException qex) {
										logger.error(qex.getMessage(), qex);
									}
								}

								// if (bs2.getValue("query") != null) {
								// IRI query = (IRI) bs2.getValue("query");
								// TupleQuery rpQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT ?q ?l ?s WHERE { ?q rdf:type <http://www.vacuity.ai/onto/via/1.0/TupleQuery> . ?q <http://www.vacuity.ai/onto/via/1.0/sparql> ?s . }");
								// rpQuery.setBinding("q", query);
								// try (TupleQueryResult r3 = rpQuery.evaluate()) {
								// if (r3.hasNext()) { // expect a single result
								// BindingSet bs3 = r3.next();
								// handler.setSparql(bs3.getValue("s").stringValue());
								// handler.setRepository(new SPARQLRepository(handler.getCall()));
								// handler.hasSparqlQuery(true);
								// }
								// }
								// catch (QueryEvaluationException qex) {
								// logger.error(qex.getMessage(), qex);
								// }
								// }

								// do o.setSparql() for the sparql of the query URI
								// set o.isSparqlEndpoint = true

								getInputs()[getInputsCursor()] = i;
								incrementInputsCursor();
							}
						}
						catch (QueryEvaluationException qex) {
							logger.error(qex.getMessage(), qex);
						}
						catch (IllegalArgumentException iaex) {
							logger.error(iaex.getMessage(), iaex);
						}
						break;
					case GraphMaster.QUERY_GET_ADAPTOR_WITH_CAPTURE:
						break;
					case GraphMaster.QUERY_GET_ADAPTOR_OUTPUT:
						try (TupleQueryResult r2 = data.evaluate()) {
							while (r2.hasNext()) {
								BindingSet bs2 = r2.next();
								logger.debug("Value: " + bs2.getValue("adaptor"));
								logger.debug("Pattern: " + bs2.getValue("pattern"));
								logger.debug("Label: " + bs2.getValue("i_label"));
								logger.debug("Config: " + bs2.getValue("config"));
								logger.debug("Translator: " + bs2.getValue("translator"));
								logger.debug("Log: " + bs2.getValue("log"));
								logger.debug("Reply Type Property: " + bs2.getValue("replyTypeProp"));
								logger.debug("Call: " + bs2.getValue("call"));
							}
						}
						catch (QueryEvaluationException qex) {
							logger.error(qex.getMessage(), qex);
						}
						break;
					case GraphMaster.QUERY_GET_ENDPOINT_LABELS:
						try (TupleQueryResult r2 = data.evaluate()) {
							while (r2.hasNext()) {
								BindingSet bs2 = r2.next();
								logger.debug("X: " + bs2.getValue("x"));
								logger.debug("Y: " + bs2.getValue("y"));
							}
						}
						catch (QueryEvaluationException qex) {
							logger.error(qex.getMessage(), qex);
						}
						break;
					case GraphMaster.QUERY_GET_PATTERN_LABELS:
						try (TupleQueryResult r2 = data.evaluate()) {
							while (r2.hasNext()) {
								BindingSet bs2 = r2.next();
								logger.debug("Pattern: " + bs2.getValue("pattern"));
								logger.debug("Label: " + bs2.getValue("i_label"));
							}
						}
						catch (QueryEvaluationException qex) {
							logger.error(qex.getMessage(), qex);
						}
						break;
					case GraphMaster.QUERY_GET_QUERIES:
						break;
					}

					// do something interesting with the values here...
				}
			}
			catch (QueryEvaluationException qex) {
				logger.error(qex.getMessage(), qex);
			}
		}
		catch (

		Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public static void main(String[] args) {
	}

	public static void addToRepository(Iterable<Statement> i, Resource context) {
		try {
			try (RepositoryConnection con = getConnection()) {
				con.begin();
				con.add(i, context);
				con.commit();
			}
			catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		catch (RDF4JException e) {
			logger.error(e.getMessage(), e);
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public static void addToRepository(Statement st, Resource context) {
		try {
			try (RepositoryConnection con = getConnection()) {
				con.begin();
				con.add(st, context);
				con.commit();
			}
			catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		catch (RDF4JException e) {
			logger.error(e.getMessage(), e);
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public static void addToRepository(String filePathStr, Resource context) {
		try {
			try (RepositoryConnection con = getConnection()) {
				con.begin();
				File f = new File(filePathStr);
				con.add(f, null, RDFFormat.RDFXML, context);
				con.commit();
			}
			catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		catch (RDF4JException e) {
			logger.error(e.getMessage(), e);
		}
		catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public static Repository parseSPARQLRepository(String url) {
		return parseSPARQLRepository(url, null);
	}

	/**
	 * Kept getting "Missing parameter: query" error on commits to rdf4j repos configured with SPARQLRepository. This method configures rdf4j URLs to use HTTPRepository instead.
	 * 
	 * See this post: https://groups.google.com/forum/#!searchin/rdf4j-users/"Missing$20parameter$3A$20query"|sort:relevance/rdf4j-users/7hS_U8MzXpI/osJNrzmfBQAJ
	 * 
	 * @param url
	 * @return a Repository appropriate for the given URL
	 */
	public static Repository parseSPARQLRepository(String url, String config) {
		String repoType = Config.getSettings().getProperty(config + "." + Config.getSettings().getProperty(Config.PROPERTY_REPO_TYPE_SUFFIX));
		if (config == null || repoType == null) repoType = Config.REPO_TYPE_SPARQL;
		if (StringUtils.isNoneBlank(repoType)) {
			switch (repoType) {
			case Config.REPO_TYPE_VIRTUOSO: {
				String user = Config.getSettings().getProperty(config + "." + Config.getSettings().getProperty(Config.PROPERTY_SUFFIX_USER));
				String pass = Config.getSettings().getProperty(config + "." + Config.getSettings().getProperty(Config.PROPERTY_SUFFIX_PASS));
				return new VirtuosoRepository("jdbc:virtuoso://" + url + "/log_enable=0", user, pass);
			}
			}
		}

		String rdf4jPathIndicator = "rdf4j-server/repositories";
		if (url.indexOf(rdf4jPathIndicator) > 0 || repoType.equals(Config.REPO_TYPE_RDF4J)) { return new HTTPRepository(url); }
		return new SPARQLRepository(url);
	}
}
