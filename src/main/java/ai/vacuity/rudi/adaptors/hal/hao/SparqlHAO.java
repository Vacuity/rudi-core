package ai.vacuity.rudi.adaptors.hal.hao;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
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
import ai.vacuity.rudi.adaptors.bo.InputProtocol;

public class SparqlHAO extends AbstractHAO {
	public final static org.slf4j.Logger logger = LoggerFactory.getLogger(SparqlHAO.class);

	// static String sparqlEndpoint = "http://www.tryrudi.io/sparql";
	// static Repository repo = new SPARQLRepository(sparqlEndpoint);
	final static File dataDir = new File("/users/smonroe/data");
	final static String indexes = "spoc,posc,cosp";
	// static Repository repo = new SailRepository(new
	// ForwardChainingRDFSInferencer(new NativeStore(dataDir, indexes)));
	final static NotifyingRepositoryWrapper repository = new NotifyingRepositoryWrapper(new HTTPRepository("http://localhost:8080/rdf4j-server", "rudi"));
	final static SemanticListener listener = new SemanticListener();
	static final HashMap<String, TupleQuery> queries = new HashMap<String, TupleQuery>();

	static final String QUERY_GET_LISTENER = "get_listener";
	static final String QUERY_GET_ADAPTOR_WITH_CAPTURE = "get_adaptor_with_capture";
	static final String QUERY_GET_ADAPTOR_OUTPUT = "get_adaptor_output";
	static final String QUERY_GET_PATTERN_LABELS = "get_pattern_labels";
	static final String QUERY_GET_ENDPOINT_LABELS = "get_endpoint_labels";
	static final String QUERY_GET_QUERIES = "get_queries";
	private static final int MAX_INPUTS = 100000;
	private static final InputProtocol[] inputs = new InputProtocol[SparqlHAO.MAX_INPUTS];

	static {
		SparqlHAO.getRepository().initialize();
		SparqlHAO.loadRepository(); // load rdf demo instance data, patterns,
									// and schema
		SparqlHAO.load(SparqlHAO.QUERY_GET_LISTENER); // load adaptors
		SparqlHAO.load(SparqlHAO.QUERY_GET_ADAPTOR_WITH_CAPTURE); // load adaptors

		SparqlHAO.getRepository().addRepositoryConnectionListener(listener);

		// QuadStore.load(QuadStore.QUERY_GET_ADAPTOR_OUTPUT);
		// QuadStore.load(QuadStore.QUERY_GET_ENDPOINT_LABELS);
		// QuadStore.load(QuadStore.QUERY_GET_PATTERN_LABELS);
		// QuadStore.load(QuadStore.QUERY_GET_QUERIES);
	}

	public static InputProtocol[] getInputs() {
		return inputs;
	}

	private static int inputsCursor = 0;

	public static int getInputsCursor() {
		return inputsCursor;
	}

	public static void setInputsCursor(int inputsCursor) {
		SparqlHAO.inputsCursor = inputsCursor;
	}

	private static void incrementInputsCursor() {
		SparqlHAO.inputsCursor++;
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
			try (TupleQueryResult result = ((TupleQuery) q).evaluate()) {
				while (result.hasNext()) { // iterate over the result
					BindingSet bindingSet = result.next();
					Value s = bindingSet.getValue("s");
					Value p = bindingSet.getValue("p");
					Value o = bindingSet.getValue("o");
					logger.debug("Subject: " + s.stringValue());
					logger.debug("Property: " + p.stringValue());
					logger.debug("Value: " + o.stringValue());

				}
			}
			catch (QueryEvaluationException qex) {
				logger.error(qex.getMessage(), qex);
			}
		}
		if (q instanceof GraphQuery) {
			try (GraphQueryResult result = ((GraphQuery) q).evaluate()) {
				while (result.hasNext()) { // iterate over the result
					Statement st = result.next();
					Resource s = st.getSubject();
					Resource p = st.getPredicate();
					Value o = st.getObject();
					logger.debug("Subject: " + s.stringValue());
					logger.debug("Property: " + p.stringValue());
					logger.debug("Value: " + o.stringValue());

				}
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
					case SparqlHAO.QUERY_GET_LISTENER:
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
								if (bs2.getValue("translator") != null) handler.setTranslator(new GenericUrl(bs2.getValue("translator").stringValue()));
								handler.setCall(bs2.getValue("call").stringValue());
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
											SemanticListener.register(iq);
											i.setQuery(iq);
											i.hasSparqlQuery(true);
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
											handler.setRepository(new SPARQLRepository(handler.getCall()));
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
					case SparqlHAO.QUERY_GET_ADAPTOR_WITH_CAPTURE:
						// // data.setBinding("capturePropertyType", getValueFactory().createIRI("http://www.vacuity.ai/onto/via/capture"));
						// try (TupleQueryResult r2 = data.evaluate()) {
						// while (r2.hasNext()) {
						// BindingSet bs2 = r2.next();
						// logger.debug("Adaptor: " + bs2.getValue("adaptor"));
						// logger.debug("Pattern: " + bs2.getValue("pattern"));
						// logger.debug("Label: " + bs2.getValue("i_label"));
						// logger.debug("Input Query: " + bs2.getValue("i_query"));
						// logger.debug("Config: " + bs2.getValue("config"));
						// logger.debug("Translator: " + bs2.getValue("translator"));
						// logger.debug("Log: " + bs2.getValue("log"));
						// logger.debug("Reply Type Property: " + bs2.getValue("replyTypeProp"));
						// logger.debug("Call: " + bs2.getValue("call"));
						// logger.debug("Media Type: " + bs2.getValue("mediaType"));
						//
						// InputProtocol i = new InputProtocol();
						// if (bs2.getValue("pattern") != null) {
						// Literal pattern = (Literal) bs2.getValue("pattern");
						// i.setTrigger(pattern);
						// i.setDataType(pattern.getDatatype());
						// if (pattern.getDatatype().equals(InputProtocol.PARSE_TYPE_REGEX)) i.setPattern(Pattern.compile(i.getTrigger().stringValue()));
						// else i.setPattern(i.getTrigger());
						// }
						// i.setLabel((Literal) bs2.getValue("i_label"));
						//
						// if (bs2.getValue("capturePropertyType") != null) {
						// String pptStr = ((IRI) bs2.getValue("capturePropertyType")).stringValue();
						// int captureIndex = Integer.parseInt(pptStr.substring(pptStr.lastIndexOf("_") + 1));
						// i.setCaptureIndex(captureIndex);
						// }
						//
						// ResponseProtocol o = new ResponseProtocol();
						// o.setConfigLabel(bs2.getValue("config").stringValue());
						// o.setLog(bs2.getValue("log").stringValue());
						// o.setContentType(MediaType.parse(bs2.getValue("mediaType").stringValue()));
						// if (bs2.getValue("translator") != null) o.setTranslator(new GenericUrl(bs2.getValue("translator").stringValue()));
						// o.setCall(bs2.getValue("call").stringValue());
						// i.setResponseProtocol(o);
						//
						// if (bs2.getValue("i_query") != null) {
						// IRI query = (IRI) bs2.getValue("i_query");
						// TupleQuery ipQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT ?q ?l ?s WHERE { ?q rdf:type <http://www.vacuity.ai/onto/via/1.0/TupleQuery> . ?q <http://www.vacuity.ai/onto/via/1.0/sparql> ?s . }");
						// ipQuery.setBinding("q", query);
						// try (TupleQueryResult r3 = ipQuery.evaluate()) {
						// if (r3.hasNext()) { // expect a single result
						// BindingSet bs3 = r3.next();
						// String queryStr = bs3.getValue("s").stringValue();
						// i.setQueryId(queryStr);
						// con.prepareTupleQuery(QueryLanguage.SPARQL, queryStr);
						// IndexableQuery iq = new IndexableQuery(con.prepareTupleQuery(QueryLanguage.SPARQL, queryStr));
						// SemanticListener.register(iq);
						// i.hasSparqlQuery(true);
						// }
						// }
						// catch (QueryEvaluationException qex) {
						// logger.error(qex.getMessage(), qex);
						// }
						// }
						//
						// getInputs()[getInputsCursor()] = i;
						// incrementInputsCursor();
						// }
						// }
						// catch (QueryEvaluationException qex) {
						// logger.error(qex.getMessage(), qex);
						// }
						// catch (IllegalArgumentException iaex) {
						// logger.error(iaex.getMessage(), iaex);
						// }
						break;
					case SparqlHAO.QUERY_GET_ADAPTOR_OUTPUT:
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
					case SparqlHAO.QUERY_GET_ENDPOINT_LABELS:
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
					case SparqlHAO.QUERY_GET_PATTERN_LABELS:
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
					case SparqlHAO.QUERY_GET_QUERIES:
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
		// repo.initialize();

		// File file = new File("/path/to/example.rdf");
		// String baseURI = "http://example.org/example/local";

		// catch (IOException e) {
		// // handle io exception
		// }
	}

	private static void loadRepository() {
		ValueFactory vf = SparqlHAO.getRepository().getValueFactory();
		try {
			try (RepositoryConnection con = getConnection()) {
				String baseDir = "/Users/smonroe/workspace/rudi-adaptors/src/main/webapp/WEB-INF/resources/listeners/";
				String[] extensions = new String[] { "rdf", "rdfs" };
				IOFileFilter filter = new SuffixFileFilter(extensions, IOCase.INSENSITIVE);
				Iterator<File> iter = FileUtils.iterateFiles(new File(baseDir), filter, DirectoryFileFilter.DIRECTORY);
				Resource context = vf.createIRI("http://tryrudi.io/rdf/demo/");
				con.clear(context);
				con.begin();
				while (iter.hasNext()) {
					File f = iter.next();
					// con.add(file, baseURI, RDFFormat.RDFXML);
					// URL url = new
					// URL("http://example.org/example/remote.rdf");
					logger.debug("Loading file: " + f.getName());
					con.add(f, null, RDFFormat.RDFXML, context);
				}
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

	public static void addToRepository(String filePathStr, String contextStr) {
		ValueFactory vf = SparqlHAO.getRepository().getValueFactory();
		try {
			try (RepositoryConnection con = getConnection()) {
				Resource context = vf.createIRI(contextStr);
				con.clear(context);
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
}
