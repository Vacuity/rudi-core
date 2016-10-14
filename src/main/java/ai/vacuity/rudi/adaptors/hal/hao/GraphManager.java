package ai.vacuity.rudi.adaptors.hal.hao;

import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.helpers.StatementPatternCollector;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParser;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParserFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.event.base.NotifyingRepositoryWrapper;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;
import org.eclipse.rdf4j.repository.util.Connections;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.slf4j.LoggerFactory;

import com.google.api.client.http.GenericUrl;
import com.google.common.net.MediaType;
import com.openlink.virtuoso.rdf4j.driver.VirtuosoRepository;

import ai.vacuity.rudi.adaptors.bo.Config;
import ai.vacuity.rudi.adaptors.bo.Context;
import ai.vacuity.rudi.adaptors.bo.EventHandler;
import ai.vacuity.rudi.adaptors.bo.InputProtocol;
import ai.vacuity.rudi.adaptors.bo.Label;
import ai.vacuity.rudi.adaptors.bo.Query;
import ai.vacuity.rudi.adaptors.interfaces.impl.DefaultNamespaceProvider;
import ai.vacuity.rudi.adaptors.regex.GraphMaster;
import ai.vacuity.rudi.adaptors.types.Prompt;
import ai.vacuity.rudi.sensor.GraphSensor;

/**
 * Manages the Index.
 * 
 * @author In Lak'ech.
 *
 */
public class GraphManager {
	public final static org.slf4j.Logger logger = LoggerFactory.getLogger(GraphManager.class);

	final static NotifyingRepositoryWrapper repository = new NotifyingRepositoryWrapper(GraphManager.parseSPARQLRepository(Config.SPARQL_ENDPOINT_VIA, Config.SPARQL_ENDPOINT_VIA_LABEL));
	final static GraphSensor sensor = new GraphSensor();
	static final HashMap<String, TupleQuery> queries = new HashMap<String, TupleQuery>();

	static final String QUERY_GET_LISTENER = Constants.NS_VI + "get_listener";
	/**
	 * Tracks altered contexts. Context added to inbox in each GraphMaster.add(... context) method. Context removed from context on each DispatchService.dispatch(... context). Context checked at the start of the GraphListener.add(... context) method.
	 */
	private static final Set<Resource> inbox = new HashSet<Resource>();

	static {
		GraphManager.getRepository().initialize();
		GraphManager.loadRepository(); // load rdf demo instance data, patterns,
										// and schema

		GraphManager.load(getValueFactory().createIRI(GraphManager.QUERY_GET_LISTENER)); // load adaptors

		GraphManager.getRepository().addRepositoryConnectionListener(sensor);
	}

	public final static IRI rdf_label = getValueFactory().createIRI(Constants.NS_RDFS, "label");
	public final static IRI rdf_type = getValueFactory().createIRI(Constants.NS_RDF, "type");
	public final static IRI rdf_List = getValueFactory().createIRI(Constants.NS_RDF, "List");
	public final static IRI rdf_first = getValueFactory().createIRI(Constants.NS_RDF, "first");
	public final static IRI rdf_nil = getValueFactory().createIRI(Constants.NS_RDF, "nil");

	public final static IRI via_bindName = getValueFactory().createIRI(Constants.NS_VIA, "bindName");

	public final static IRI via_value = getValueFactory().createIRI(Constants.NS_VIA, "value");

	public final static IRI via_query = getValueFactory().createIRI(Constants.NS_VIA, "query");

	public final static IRI dc_date = getValueFactory().createIRI(Constants.NS_DC, "date");

	public final static IRI via_Alert = getValueFactory().createIRI(Constants.NS_VIA, "Alert");

	public final static IRI via_Hit = getValueFactory().createIRI(Constants.NS_VIA, "Hit");

	public final static IRI via_QueryResult = getValueFactory().createIRI(Constants.NS_VIA, "QueryResult");

	public final static IRI via_Results = getValueFactory().createIRI(Constants.NS_VIA, "Results");

	public final static IRI via_Projection = getValueFactory().createIRI(Constants.NS_VIA, "Projection");

	public final static IRI via_timestamp = getValueFactory().createIRI(Constants.NS_VIA, "timestamp");

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

	public static void remove(String queryLabel) {
		try (RepositoryConnection con = getConnection()) {
			TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT ?q ?l ?s WHERE { ?q rdf:type <http://www.vacuity.ai/onto/via/1.0/TupleQuery> . ?q rdfs:label ?l . ?q <http://www.vacuity.ai/onto/via/1.0/sparql> ?s . }");
			tupleQuery.setBinding("l", getValueFactory().createLiteral(queryLabel));
			try (TupleQueryResult result = tupleQuery.evaluate()) {
				while (result.hasNext()) { // iterate over the result
					BindingSet bindingSet = result.next();
					Value q = bindingSet.getValue("q");
					Value label = bindingSet.getValue("l");
					Value sparql = bindingSet.getValue("s");
					// logger.debug("Value: " + q.stringValue());
					// logger.debug("Label: " + label.stringValue());
					// logger.debug("SPARQL: " + sparql.stringValue());

					GraphQuery data = con.prepareGraphQuery(QueryLanguage.SPARQL, sparql.stringValue());
					try (GraphQueryResult gr = ((GraphQuery) data).evaluate()) {
						Model m = QueryResults.asModel(gr);
						con.remove(m);
					}
					catch (QueryEvaluationException qex) {
						logger.error(qex.getMessage(), qex);
					}
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

	public static void execute(String queryLabel) {
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

					GraphQuery data = con.prepareGraphQuery(QueryLanguage.SPARQL, sparql.stringValue());
					try (GraphQueryResult gr = ((GraphQuery) data).evaluate()) {
						Vector<Statement> results = new Vector<Statement>();
						while (result.hasNext()) { // iterate over the result
							Statement st = gr.next();
							// Resource s = st.getSubject();
							// Resource p = st.getPredicate();
							// Value o = st.getObject();
							results.add(st);
							// // logger.debug("Subject: " + s.stringValue());
							// // logger.debug("Property: " + p.stringValue());
							// // logger.debug("Value: " + o.stringValue());
						}
						GraphManager.addToRepository(results, getValueFactory().createIRI(Constants.CONTEXT_DEMO));
					}
					catch (QueryEvaluationException qex) {
						logger.error(qex.getMessage(), qex);
					}
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

	static void loadRepository() {
		try (RepositoryConnection con = getConnection()) {

			IRI listenerContextClassIRI = getValueFactory().createIRI(Constants.NS_VIA + "ListenerContext");
			IRI subClassOfIRI = getValueFactory().createIRI(Constants.NS_RDFS + "subClassOf");

			con.begin();

			// remove all listener data
			List<Statement> listeners = Iterations.asList(con.getStatements(null, subClassOfIRI, listenerContextClassIRI));
			for (Statement listener : listeners) {
				con.remove(null, null, null, new Resource[] { listener.getSubject() });
			}

			// clear the default RUDI context
			String[] extensions = new String[] { "rdf", "rdfs" };
			Iterator<File> iter = FileUtils.iterateFiles(new File(Config.DIR_LISTENERS), extensions, true);
			Resource context = getValueFactory().createIRI(Constants.CONTEXT_RUDI);
			con.clear(context);

			while (iter.hasNext()) {
				File f = iter.next();
				logger.debug("Loading file: " + f.getName());
				try {
					IRI listenerContext = getValueFactory().createIRI("file://" + f.getAbsolutePath());
					con.add(f, null, RDFFormat.RDFXML, listenerContext);
					con.add(listenerContext, subClassOfIRI, listenerContextClassIRI, context);
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

	public static void load(IRI queryIRI) {
		try (RepositoryConnection con = getConnection()) {
			TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT ?q ?l ?s WHERE { ?q <" + Constants.NS_RDFS + "label> ?l . ?q <http://www.vacuity.ai/onto/via/1.0/sparql> ?s . }");
			tupleQuery.setBinding("q", queryIRI);
			try (TupleQueryResult result = tupleQuery.evaluate()) {
				while (result.hasNext()) { // iterate over the result
					BindingSet bindingSet = result.next();
					Value q = bindingSet.getValue("q");
					Value queryLabel = bindingSet.getValue("l");
					Value sparql = bindingSet.getValue("s");
					logger.debug("Value: " + q.stringValue());
					logger.debug("Label: " + queryLabel.stringValue());
					logger.debug("SPARQL: " + sparql.stringValue());

					TupleQuery data = con.prepareTupleQuery(QueryLanguage.SPARQL, sparql.stringValue());
					switch (queryIRI.stringValue()) {
						case GraphManager.QUERY_GET_LISTENER:
							// data.setBinding("patternPropertyType", getValueFactory().createIRI("http://www.vacuity.ai/onto/via/pattern"));
							try (TupleQueryResult r2 = data.evaluate()) {
								Vector<Exception> err = new Vector<Exception>();
								while (r2.hasNext()) {
									try {
										BindingSet bs2 = r2.next();
										logger.debug("Listener: " + bs2.getValue("listener"));
										logger.debug("Pattern: " + bs2.getValue("pattern"));
										logger.debug("Labels: " + bs2.getValue("i_labels"));
										logger.debug("Query type: " + bs2.getValue("qtype"));
										// logger.debug("Input Query Label: " + bs2.getValue("i_query_label"));
										logger.debug("Event Handler: " + bs2.getValue("output"));
										logger.debug("Config: " + bs2.getValue("config"));
										logger.debug("Translator: " + bs2.getValue("translator"));
										logger.debug("Log: " + bs2.getValue("log"));
										logger.debug("Reply Type Property: " + bs2.getValue("replyTypeProp"));
										logger.debug("Endpoint: " + bs2.getValue("endpoint"));
										logger.debug("Media Type: " + bs2.getValue("mediaType"));
										logger.debug("Response Query: " + bs2.getValue("query"));

										// TODO a hack validator in lieu of a better query semantics
										if (bs2.getValue("pattern") == null && bs2.getValue("i_query") == null && bs2.getValue("i_label") == null) {
											IllegalArgumentException ilaex = new IllegalArgumentException("Error on query: " + bs2.getValue("input") + ". via:Input must contain a via:pattern, via:capture_*, or via:query predicate.");
											logger.error(ilaex.getMessage(), ilaex);
											continue;
										}

										// if (bs2.getValue("i_query") != null && !(bs2.getValue("i_query") instanceof IRI)) {
										// IllegalArgumentException ilaex = new IllegalArgumentException("Error on query: " + bs2.getValue("i_query") + ". via:Query must be a IRI resource.");
										// logger.error(ilaex.getMessage(), ilaex);
										// continue;
										// }

										InputProtocol i = new InputProtocol();
										Value pattern = bs2.getValue("pattern");
										if (pattern != null) {
											i.setTrigger(pattern);
											if (pattern instanceof Literal) {
												Literal patternLit = (Literal) pattern;
												i.setDataType(patternLit.getDatatype());
												String patternStr = patternLit.stringValue();
												i.setPatternScore(GraphMaster.score(patternStr));
												if (patternLit.getDatatype().equals(InputProtocol.PARSE_TYPE_REGEX)) i.setPattern(Pattern.compile(i.getTrigger().stringValue()));
												else {
													List<Value> l = new ArrayList<Value>();
													l.add(pattern);
													i.setPatternScore(100.0);
													i.setPattern(l);
												}
											}
											else if (pattern instanceof IRI) {
												try {
													IRI patternIRI = (IRI) pattern;
													Model patternList = Connections.getRDFCollection(getConnection(), patternIRI, new LinkedHashModel());
													Iterator<Statement> m = patternList.iterator();
													List<Value> l = new ArrayList<Value>();
													collect_list_element: while (m.hasNext()) {
														Statement st = m.next();
														Resource s = st.getSubject();
														IRI p = st.getPredicate();
														Value o = st.getObject();

														// ignore non list related properties
														// do not use GraphManager IRI constants in this method, since we're still in the static block of the class scope, which occurs prior to the loading of the static IRI fields
														// check via:pattern, even though right now rdf4j isn't recognizing via:pattern as a sub property of rdf:first (perhaps later they will)
														if (p.equals(getValueFactory().createIRI(Constants.NS_VIA, "pattern")) || p.equals(getValueFactory().createIRI(Constants.NS_RDF, "first"))) {
															if (o.equals(getValueFactory().createIRI(Constants.NS_RDF, "nil"))) {
																l.add(null); // give the nils indices
															}
															else {
																l.add(o);
															}
														}
													}
													i.setPatternScore(100.0);
													i.setPattern(l);
												}
												catch (RepositoryException rex) {
													logger.error(rex.getMessage(), rex);
												}
											}
										}

										if (bs2.getValue("override") != null) {
											String override = bs2.getValue("override").stringValue();
											if (StringUtils.isNotBlank(override)) {
												StringReader sr = new StringReader(override);
												i.getOverrides().load(sr);
											}
										}

										if (bs2.getValue("i_labels") != null) {
											String labelsStr = bs2.getValue("i_labels").stringValue();
											String COMMA_ESCAPE = "||comma-rudi-replacement||";
											String SEMI_COLON_ESCAPE = "||semi-colon-rudi-replacement||";
											labelsStr = labelsStr.replace("\\,", COMMA_ESCAPE);
											labelsStr = labelsStr.replace("\\:", SEMI_COLON_ESCAPE);
											StringTokenizer st = new StringTokenizer(labelsStr, ",");
											List<Label> labels = new ArrayList<Label>();
											for (int idx = 0; st.hasMoreTokens(); idx++) {
												Label label = new Label();
												String labelStr = st.nextToken();
												label.setLabel(labelStr);
												int scidx = labelStr.indexOf(":");
												if (scidx > 0) {
													if (scidx == labelStr.length() - 1) {
														IllegalArgumentException iaex = new IllegalArgumentException("Label qName missing local part: " + bs2.getValue("input"));
														logger.error(iaex.getMessage(), iaex);
													}
													else {
														Namespace ns = new DefaultNamespaceProvider().getNamespace(labelStr.substring(0, labelStr.indexOf(":")));
														if (ns != null) {
															label.setNamespace(ns);
															label.setLocalPart(labelStr.substring(labelStr.indexOf(":") + 1));
															IRI labelIRI = getValueFactory().createIRI(label.getIRI());
															List<Statement> nsLabels = Iterations.asList(con.getStatements(labelIRI, getValueFactory().createIRI(Constants.NS_RDFS + "label"), null));
															for (Statement nsl : nsLabels) {
																label.setLabel(nsl.getObject().stringValue());
																break;
															}
														}
													}
												}

												labelStr = label.getLabel();
												labelStr = labelStr.replace(COMMA_ESCAPE, ",");
												labelStr = labelStr.replace(SEMI_COLON_ESCAPE, ":");
												label.setLabel(labelStr);
												labels.add(label);
											}
											i.setLabels(labels);
										}

										// load contexts

										IRI[] tests = new IRI[] { getValueFactory().createIRI(Constants.NS_VIA + "isEmpty"), getValueFactory().createIRI(Constants.NS_VIA + "isNotEmpty") };

										for (IRI test : tests) {
											if (bs2.getValue("input") instanceof IRI) {
												IRI inputIRI = (IRI) bs2.getValue("input");
												List<Statement> contexts = Iterations.asList(con.getStatements(inputIRI, test, null));
												for (Statement context : contexts) {
													if (context.getObject() instanceof Resource) {
														Resource qiri = (Resource) context.getObject();
														TupleQuery ctxq = con.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT ?q ?l ?s ?listener WHERE { ?q rdf:type <" + Constants.NS_VIA + "TupleQuery> . ?q <" + Constants.NS_VIA + "sparql> ?s . ?input <" + Constants.NS_VIA + "context> ?q . ?listener <" + Constants.NS_VIA + "event> ?input.}");
														ctxq.setBinding("q", qiri);
														try (TupleQueryResult r3 = ctxq.evaluate()) {
															while (r3.hasNext()) {
																BindingSet bs3 = r3.next();
																String queryStr = bs3.getValue("s").stringValue();
																Context ctx = new Context(con.prepareTupleQuery(QueryLanguage.SPARQL, queryStr));
																ctx.setId(bs3.getValue("q").stringValue().hashCode());
																ctx.setLabel(bs3.getValue("l").stringValue());
																ctx.setIri((IRI) bs3.getValue("q"));
																if (test.equals(getValueFactory().createIRI(Constants.NS_VIA + "isEmpty"))) ctx.setHasIsEmpty(true);
																if (bs3.getValue("listener") instanceof IRI) ctx.setListenerContext((IRI) bs3.getValue("listener"));
																ctx.setOwnerIri(inputIRI);

																Iterator bnames = bs3.getBindingNames().iterator();
																while (bnames.hasNext()) {
																	SPARQLParserFactory factory = new SPARQLParserFactory();
																	QueryParser parser = factory.getParser();
																	ParsedQuery parsedQuery = parser.parseQuery(queryStr, null);

																	StatementPatternCollector collector = new StatementPatternCollector();
																	TupleExpr tupleExpr = parsedQuery.getTupleExpr();
																	tupleExpr.visit(collector);

																	for (StatementPattern pt : collector.getStatementPatterns()) {
																		logger.debug(pt.toString());
																		// Value s = pt.getSubjectVar().getValue();
																		Value p = pt.getPredicateVar().getValue();
																		// Value o = pt.getObjectVar().getValue();

																		TupleQuery pq = con.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT ?prompt WHERE { ?q ?listener <" + Constants.NS_VIA + "prompt> ?prompt . }");
																		pq.setBinding("q", p);
																		try (TupleQueryResult r4 = pq.evaluate()) {
																			while (r4.hasNext()) {
																				Prompt prompt = new Prompt();
																				prompt.setLabel(bs3.getValue("prompt").stringValue());
																				if (bs3.getValue("q") instanceof IRI) {
																					prompt.setIri(((IRI) bs3.getValue("q")).stringValue());
																				}
																				ctx.getPrompts().add(prompt);
																			}
																		}
																		catch (QueryEvaluationException qex) {
																			logger.error(qex.getMessage(), qex);
																		}
																	}

																}

																i.getContexts().add(ctx);
															}
														}
														catch (QueryEvaluationException qex) {
															logger.error(qex.getMessage(), qex);
														}
													}
												}
											}
										}

										// event handlers are loaded only if they are called by a listeners, see the vi:get_listener SPARQL query
										EventHandler handler = new EventHandler();
										handler.setConfigLabel(bs2.getValue("config").stringValue());
										handler.setLog(bs2.getValue("log").stringValue());
										if (bs2.getValue("mediaType") != null) handler.setContentType(MediaType.parse(bs2.getValue("mediaType").stringValue()));
										handler.setIri((IRI) bs2.getValue("output"));
										if (bs2.getValue("translator") != null) {
											handler.setTranslator(new GenericUrl(bs2.getValue("translator").stringValue().replace("http://rudi.endpoint.placeholders.vacuity.ai", Config.getRudiEndpoint()).replace("http://rudi.host.placeholders.vacuity.ai", (Boolean.parseBoolean(Config.RUDI_SECURE) ? "https://" : "http://") + Config.RUDI_HOST)));
										}
										// ENDPOINT VALIDATION START //
										if (bs2.getValue("endpoint") != null) {
											handler.setCall(bs2.getValue("endpoint").stringValue());
											boolean ok = false;
											List<GenericUrl> l = Config.get(handler.getConfigLabel()).getSandboxedEndpoints();
											for (GenericUrl g : l) {
												if (StringUtils.startsWith(handler.getCall(), g.toURL().toString())) {
													ok = true;
													break;
												}
											}
											if (!ok) {
												SecurityException sex = new SecurityException("Error loading '" + bs2.getValue("output") + "'. Endpoint " + handler.getCall() + " not allowed by configuration: " + handler.getConfigLabel());
												logger.error(sex.getMessage(), sex);
												continue;
											}
										}
										// ENDPOINT VALIDATION END //

										// handler.setCall(Config.getSettings().getProperty(handler.getConfigLabel() + Config.PROPERTY_SUFFIX_URL) + bs2.getValue("call").stringValue());
										i.setEventHandler(handler);

										// fetch imports
										List<Statement> imports = Iterations.asList(con.getStatements(handler.getIri(), getValueFactory().createIRI(Constants.NS_VIA, "import"), null));
										for (Statement imp : imports) {
											TupleQuery ipQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT ?q ?l ?s WHERE { ?q rdf:type <" + Constants.NS_VIA + "TupleQuery> . ?q <" + Constants.NS_VIA + "sparql> ?s . }");
											ipQuery.setBinding("q", imp.getObject());
											try (TupleQueryResult r3 = ipQuery.evaluate()) {
												if (r3.hasNext()) { // expect a single result
													BindingSet bs3 = r3.next();
													handler.getImports().add(bs3.getValue("s").stringValue());
												}
											}
											catch (QueryEvaluationException qex) {
												logger.error(qex.getMessage(), qex);
											}
										}

										// if sparql
										// Create a new Repo, set its URL to the value o.getCall()
										// retrive query resource from repo
										if (bs2.getValue("qtype") != null) {
											IRI query = (IRI) bs2.getValue("input");
											TupleQuery ipQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT ?q ?l ?s WHERE { ?q rdf:type <" + Constants.NS_VIA + "TupleQuery> . ?q <" + Constants.NS_VIA + "sparql> ?s . }");
											ipQuery.setBinding("q", query);
											try (TupleQueryResult r3 = ipQuery.evaluate()) {
												if (r3.hasNext()) { // expect a single result
													BindingSet bs3 = r3.next();
													String queryStr = bs3.getValue("s").stringValue();
													Query iq = new Query(con.prepareTupleQuery(QueryLanguage.SPARQL, queryStr));
													iq.setId(bs2.getValue("input").stringValue().hashCode());
													iq.setLabel(bs2.getValue("i_label").stringValue());
													iq.setIri((IRI) bs2.getValue("input"));
													GraphSensor.register(iq);
													i.setQuery(iq);
													// i.hasSparqlQuery(true);
												}
											}
											catch (QueryEvaluationException qex) {
												logger.error(qex.getMessage(), qex);
											}
										}

										// response query
										if (bs2.getValue("query") != null) {
											IRI query = (IRI) bs2.getValue("query");
											TupleQuery rpQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT ?q ?l ?s WHERE { ?q rdf:type <" + Constants.NS_VIA + "TupleQuery> . ?q <" + Constants.NS_VIA + "sparql> ?s . }");
											rpQuery.setBinding("q", query);
											try (TupleQueryResult r3 = rpQuery.evaluate()) {
												if (r3.hasNext()) { // expect a single result
													BindingSet bs3 = r3.next();
													handler.setSparql(bs3.getValue("s").stringValue());
													handler.addRepository(GraphManager.parseSPARQLRepository(handler.getCall(), bs2.getValue("config").stringValue()));
													handler.hasSparqlQuery(true);
												}
											}
											catch (QueryEvaluationException qex) {
												logger.error(qex.getMessage(), qex);
											}
										}

										// do o.setSparql() for the sparql of the query URI
										// set o.isSparqlEndpoint = true

										if (i.getPattern() instanceof List) {
											GraphMaster.getTypedPatterns()[GraphMaster.getTypedInputsCursor()] = i;
											GraphMaster.incrementTypedInputsCursor();
										}
										else if (i.getPattern() != null) {
											GraphMaster.getRegexPatterns()[GraphMaster.getRegexInputsCursor()] = i;
											GraphMaster.incrementRegexInputsCursor();
										}
										else if (i.hasSparqlQuery()) {
											GraphMaster.getQueryPatterns()[GraphMaster.getQueryInputsCursor()] = i;
											GraphMaster.incrementQueryInputsCursor();
										}

									}
									catch (Exception ex) {
										logger.error(ex.getMessage(), ex);
									}
								} // while query hasResults
							}
							catch (QueryEvaluationException qex) {
								logger.error(qex.getMessage(), qex);
							}
							catch (IllegalArgumentException iaex) {
								logger.error(iaex.getMessage(), iaex);
							}
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

	public static void addToRepository(InputStream is, Resource context) {
		try {
			try (RepositoryConnection con = getConnection()) {
				con.begin();
				con.add(is, context.stringValue(), RDFFormat.JSONLD, context);
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

	public static void addToRepository(Iterable<Statement> i, Resource context) {
		try {
			try (RepositoryConnection con = getConnection()) {
				con.begin();
				con.add(i, context);
				con.commit();
				GraphManager.updateInbox(context);
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
				GraphManager.updateInbox(context);
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
				GraphManager.updateInbox(context);
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

	/**
	 * Kept getting "Missing parameter: query" error on commits to rdf4j repos configured with SPARQLRepository. This method configures rdf4j URLs to use HTTPRepository instead.
	 * 
	 * See this post: https://groups.google.com/forum/#!searchin/rdf4j-users/"Missing$20parameter$3A$20query"|sort:relevance/rdf4j-users/7hS_U8MzXpI/osJNrzmfBQAJ
	 * 
	 * @param call
	 * @return a Repository appropriate for the given URL
	 */
	public static Repository parseSPARQLRepository(String call, String configLabel) throws IllegalArgumentException {
		String rdf4jPathIndicator = "rdf4j-server/repositories";
		if (call.indexOf(rdf4jPathIndicator) > 0) { return new HTTPRepository(call); }
		if (configLabel == null) throw new IllegalArgumentException("Empty configuration label for '" + call + "'. Configuration label required for custom repositories.");
		Config config = Config.get(configLabel);
		if (configLabel.startsWith(Config.PREFIX_RUDI_REPO) && config == null) {
			config = Config.get(Config.PREFIX_RUDI_REPO);
		}
		if (config != null) {
			String repoType = config.getRepoType();
			if (StringUtils.isNotBlank(repoType)) {
				switch (repoType) {
					case Config.REPO_TYPE_VIRTUOSO: {
						return new VirtuosoRepository("jdbc:virtuoso://" + config.getHost() + ":" + config.getPort() + "/log_enable=0", config.getUserName(), config.getPassword());
					}
					case Config.REPO_TYPE_RDF4J: {
						return new HTTPRepository(call);
					}
				}
			}
		}
		return new SPARQLRepository(call);
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

	public static Set<Resource> getInbox() {
		return GraphManager.inbox;
	}

	public static void removeFromInbox(Resource r) {
		GraphManager.getInbox().remove(r);
	}

	private static void updateInbox(Resource r) {
		GraphManager.getInbox().add(r);
	}

}
