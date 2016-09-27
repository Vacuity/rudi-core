package ai.vacuity.rudi.adaptors.data;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang.StringUtils;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.slf4j.LoggerFactory;

import com.google.api.client.http.GenericUrl;

import ai.vacuity.rudi.adaptors.bo.Endpoint;
import ai.vacuity.rudi.adaptors.bo.Input;
import ai.vacuity.rudi.adaptors.bo.Output;
import ai.vacuity.rudi.adaptors.controller.APIClient;

public class QuadStore {
	private final static org.slf4j.Logger logger = LoggerFactory.getLogger(QuadStore.class);

	// static String sparqlEndpoint = "http://www.tryrudi.io/sparql";
	// static Repository repo = new SPARQLRepository(sparqlEndpoint);
	final static File dataDir = new File("/users/smonroe/data");
	final static String indexes = "spoc,posc,cosp";
	// static Repository repo = new SailRepository(new
	// ForwardChainingRDFSInferencer(new NativeStore(dataDir, indexes)));
	final static Repository repository = new HTTPRepository("http://localhost:8080/rdf4j-server", "rudi");
	static final HashMap<String, TupleQuery> queries = new HashMap<String, TupleQuery>();

	static final String QUERY_GET_ADAPTOR = "get_adaptor";
	static final String QUERY_GET_ADAPTOR_OUTPUT = "get_adaptor_output";
	static final String QUERY_GET_PATTERN_LABELS = "get_pattern_labels";
	static final String QUERY_GET_ENDPOINT_LABELS = "get_endpoint_labels";
	static final String QUERY_GET_QUERIES = "get_queries";
	private static final int MAX_INPUTS = 100000;
	private static final Input[] inputs = new Input[QuadStore.MAX_INPUTS];

	static {
		QuadStore.getRepository().initialize();
		QuadStore.loadRepository(); // load rdf demo instance data, patterns,
									// and schema
		QuadStore.load(QuadStore.QUERY_GET_ADAPTOR); // load adaptors
		// QuadStore.load(QuadStore.QUERY_GET_ADAPTOR_OUTPUT);
		// QuadStore.load(QuadStore.QUERY_GET_ENDPOINT_LABELS);
		// QuadStore.load(QuadStore.QUERY_GET_PATTERN_LABELS);
		// QuadStore.load(QuadStore.QUERY_GET_QUERIES);
	}

	public static Input[] getInputs() {
		return inputs;
	}

	private static int inputsCursor = 0;

	public static int getInputsCursor() {
		return inputsCursor;
	}

	public static void setInputsCursor(int inputsCursor) {
		QuadStore.inputsCursor = inputsCursor;
	}

	private static void incrementInputsCursor() {
		QuadStore.inputsCursor++;
	}

	public static Repository getRepository() {
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
			TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT ?q ?l ?s WHERE { ?q rdf:type <http://www.vacuity.ai/onto/via/TupleQuery> . ?q rdfs:label ?l . ?q <http://www.vacuity.ai/onto/via/sparql> ?s . }");
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

	public static void load(String queryLabel) {
		Endpoint.load();
		try (RepositoryConnection con = getConnection()) {
			TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, "SELECT ?q ?l ?s WHERE { ?q rdf:type <http://www.vacuity.ai/onto/via/TupleQuery> . ?q rdfs:label ?l . ?q <http://www.vacuity.ai/onto/via/sparql> ?s . }");
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
					case QuadStore.QUERY_GET_ADAPTOR:
						data.setBinding("patternPropertyType", getValueFactory().createIRI("http://www.vacuity.ai/onto/via/pattern"));
						try (TupleQueryResult r2 = data.evaluate()) {
							while (r2.hasNext()) {
								BindingSet bs2 = r2.next();
								logger.debug("\nAdaptor: " + bs2.getValue("adaptor"));
								logger.debug("Pattern: " + bs2.getValue("pattern"));
								logger.debug("Label: " + bs2.getValue("i_label"));
								logger.debug("Endpoint: " + bs2.getValue("endpoint"));
								logger.debug("Translator: " + bs2.getValue("translator"));
								logger.debug("Log: " + bs2.getValue("log"));
								logger.debug("Reply Type Property: " + bs2.getValue("replyTypeProp"));
								logger.debug("Call: " + bs2.getValue("call"));

								Input i = new Input();
								Literal pattern = (Literal) bs2.getValue("pattern");
								i.setLabel((Literal) bs2.getValue("i_label"));
								i.setTrigger(pattern);
								i.setDataType(pattern.getDatatype());
								if (pattern.getDatatype().equals(Input.PARSE_TYPE_REGEX)) i.setPattern(Pattern.compile(i.getTrigger().stringValue()));
								else i.setPattern(i.getTrigger());
								Output o = new Output();
								o.setEndpointLabel(bs2.getValue("endpoint").stringValue());
								o.setLog(bs2.getValue("log").stringValue());
								if (bs2.getValue("translator") != null) o.setTranslator(new GenericUrl(bs2.getValue("translator").stringValue()));
								o.setCall(bs2.getValue("call").stringValue());
								i.setOutput(o);
								// long startTime = System.currentTimeMillis();
								//
								// for (inputsCursor = 0; getInputsCursor() <
								// QuadStore.MAX_INPUTS;
								// incrementInputsCursor()) {
								// Input store = new Input();
								// store.setLabel((Literal)
								// bs2.getValue("i_label"));
								// store.setTrigger(bs2.getValue("pattern"));
								// store.setPattern(Pattern.compile(i.getTrigger().stringValue()));
								// Output store_out = new Output();
								// store_out.setEndpointLabel(bs2.getValue("endpoint").stringValue());
								// store_out.setLog(bs2.getValue("log").stringValue());
								// store_out.setTranslator(new
								// GenericUrl(bs2.getValue("translator").stringValue()));
								// store_out.setCall(bs2.getValue("call").stringValue());
								// store.setOutput(store_out);
								// getInputs()[getInputsCursor()] = store;
								// }
								// long endTime = System.currentTimeMillis();
								// long totalTime = endTime - startTime;
								// System.out.println("Total load time [" +
								// QuadStore.MAX_INPUTS + " patterns]: " +
								// totalTime + "ms.");
								getInputs()[getInputsCursor()] = i;
								incrementInputsCursor();
							}
						}
						catch (QueryEvaluationException qex) {
							logger.error(qex.getMessage(), qex);
						}
						break;
					case QuadStore.QUERY_GET_ADAPTOR_OUTPUT:
						try (TupleQueryResult r2 = data.evaluate()) {
							while (r2.hasNext()) {
								BindingSet bs2 = r2.next();
								logger.debug("Value: " + bs2.getValue("adaptor"));
								logger.debug("Pattern: " + bs2.getValue("pattern"));
								logger.debug("Label: " + bs2.getValue("i_label"));
								logger.debug("Endpoint: " + bs2.getValue("endpoint"));
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
					case QuadStore.QUERY_GET_ENDPOINT_LABELS:
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
					case QuadStore.QUERY_GET_PATTERN_LABELS:
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
					case QuadStore.QUERY_GET_QUERIES:
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

	public static void processInput(String target) throws IOException, IllegalArgumentException {
		find_matches: for (Input input : getInputs()) {
			if (input == null) break;
			String call = input.getOutput().getCall();
			String log = input.getOutput().getLog();

			call = processTemplate(input, call, target);
			if (call == null) continue find_matches;
			log = processTemplate(input, log, target);

			logger.debug("[Rudi]: " + log);

			// if (call.indexOf("?") > 0) {
			// String path = call;
			// String params = "";
			// path = call.substring(0, call.indexOf("?"));
			// params = URLEncoder.encode(call.substring(call.indexOf("?") + 1), java.nio.charset.StandardCharsets.UTF_8.toString());
			// call = path + "?" + params;
			// }
			APIClient.setCall(call);
			if (input.getOutput().hasTranslator()) APIClient.setXslt(input.getOutput().getTranslator().build());
			APIClient.run();
		}
	}

	private static String processTemplate(Input input, String template, String target) {
		// 1. swap captured groups' placeholders first
		// FIRST MATCH GATE
		boolean found = false;
		if (input.getDataType().equals(Input.PARSE_TYPE_REGEX) && input.getPattern() instanceof Pattern) {
			Matcher matcher = ((Pattern) input.getPattern()).matcher(target);
			while (matcher.find()) {
				if (!found) {
					logger.debug("Match: " + input.getTrigger().stringValue());
					found = true;
				}
				int groups = matcher.groupCount();
				for (int gp = 1; gp <= groups; gp++) {
					logger.debug("group " + gp + ": " + matcher.group(gp));
					template = template.replace("${" + gp + "}", matcher.group(gp));
				}
				template = template.replace("${0}", matcher.group());
			}
		}

		// SECOND MATCH GATE
		if (input.getDataType().equals(getValueFactory().createIRI("http://www.vacuity.ai/onto/via/URL"))) {
			try {
				new URL(target);
				found = true;
			}
			catch (MalformedURLException mfuex) {
				return null;
			}
		}
		if (input.getDataType().equals(getValueFactory().createIRI("http://www.w3.org/2001/XMLSchema#anyURI"))) {
			try {
				getValueFactory().createIRI(target);
				found = true;
			}
			catch (IllegalArgumentException iaex) {
				return null;
			}
		}
		if (input.getDataType().equals(getValueFactory().createIRI("http://www.w3.org/2001/XMLSchema#integer"))) {
			try {
				Integer.parseInt(target);
				found = true;
			}
			catch (NumberFormatException nfex) {
				return null;
			}
		}
		if (input.getDataType().equals(getValueFactory().createIRI("http://www.w3.org/2001/XMLSchema#dateTime"))) {
			try {
				// for type dateTime, the value is the date format
				new SimpleDateFormat(((Value) input.getPattern()).stringValue());
				found = true;
			}
			catch (NumberFormatException nfex) {
				return null;
			}
		}

		if (!found) return null;

		// 2. run the call processor (give customer processors priority over system processor)
		// TODO should the log include the unprocessed placeholder value?
		Endpoint.getEndpointmap().get(input.getOutput().getEndpointLabel()).getTemplateProcessor().process(template, target);
		template = Endpoint.getEndpointmap().get(input.getOutput().getEndpointLabel()).getTemplateProcessor().getTemplate();
		target = Endpoint.getEndpointmap().get(input.getOutput().getEndpointLabel()).getTemplateProcessor().getTarget();
		if (StringUtils.isNotBlank(template)) template = template.replace("${0}", target);

		// 3. swap any remaining reserved placed holders
		if (Endpoint.getEndpointmap().get(input.getOutput().getEndpointLabel()).hasKey()) template = template.replace("${key}", Endpoint.getEndpointmap().get(input.getOutput().getEndpointLabel()).getKey());
		if (Endpoint.getEndpointmap().get(input.getOutput().getEndpointLabel()).hasId()) template = template.replace("${id}", Endpoint.getEndpointmap().get(input.getOutput().getEndpointLabel()).getId());
		if (Endpoint.getEndpointmap().get(input.getOutput().getEndpointLabel()).hasToken()) template = template.replace("${token}", Endpoint.getEndpointmap().get(input.getOutput().getEndpointLabel()).getToken());

		return template;
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
		ValueFactory vf = QuadStore.getRepository().getValueFactory();
		try {
			try (RepositoryConnection con = getConnection()) {
				String baseDir = "/Users/smonroe/workspace/rudi-adaptors/src/main/webapp/WEB-INF/resources/adaptors/";
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
		ValueFactory vf = QuadStore.getRepository().getValueFactory();
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
