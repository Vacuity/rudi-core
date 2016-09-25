package ai.vacuity.rudi.adaptors.data;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
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
	static Repository repo = new HTTPRepository("http://localhost:8080/rdf4j-server", "rudi");

	public static Repository getRepo() {
		return repo;
	}

	public static void setRepo(Repository repo) {
		QuadStore.repo = repo;
	}

	public static void main(String[] args) {
		// repo.initialize();

		QuadStore.getRepo().initialize();

		File file = new File("/path/to/example.rdf");
		String baseURI = "http://example.org/example/local";
		ValueFactory vf = QuadStore.getRepo().getValueFactory();
		try {
			RepositoryConnection con = repo.getConnection();
			try {

				String baseDir = "/Users/smonroe/workspace/rudi-adaptors/src/main/webapp/WEB-INF/resources/adaptors/";
				String[] extensions = new String[] { "rdf" };
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
					con.add(f, "http://tryrudi.io/rdf/demo/", RDFFormat.RDFXML, context);
				}

			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				con.commit();
				con.close();
			}
		} catch (RDF4JException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		// catch (IOException e) {
		// // handle io exception
		// }

	}

}
