package ai.vacuity.rudi.adaptors.data;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.eclipse.rdf4j.RDF4JException;
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

public class QuadStore {

	// static String sparqlEndpoint = "http://www.tryrudi.io/sparql";
	// static Repository repo = new SPARQLRepository(sparqlEndpoint);
	final static File dataDir = new File("/users/smonroe/data");
	final static String indexes = "spoc,posc,cosp";
	// static Repository repo = new SailRepository(new
	// ForwardChainingRDFSInferencer(new NativeStore(dataDir, indexes)));
	final static Repository repository = new HTTPRepository("http://localhost:8080/rdf4j-server", "rudi");
	static final HashMap<String, TupleQuery> queries = new HashMap<String, TupleQuery>();

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
					System.out.println("Value: " + q.stringValue());
					System.out.println("Label: " + label.stringValue());
					System.out.println("SPARQL: " + sparql.stringValue());
					// do something interesting with the values here...
				}
			}
			catch (QueryEvaluationException qex) {
				qex.printStackTrace();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// repo.initialize();

		QuadStore.getRepository().initialize();

		// File file = new File("/path/to/example.rdf");
		// String baseURI = "http://example.org/example/local";
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
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		catch (RDF4JException e) {
			e.printStackTrace();
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		QuadStore.getQueries();
		// catch (IOException e) {
		// // handle io exception
		// }

	}

}
