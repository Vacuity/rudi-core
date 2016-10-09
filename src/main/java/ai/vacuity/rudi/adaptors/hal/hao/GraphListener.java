package ai.vacuity.rudi.adaptors.hal.hao;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.helpers.StatementPatternCollector;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParser;
import org.eclipse.rdf4j.query.parser.sparql.SPARQLParserFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.event.RepositoryConnectionListener;
import org.slf4j.LoggerFactory;

import ai.vacuity.rudi.adaptors.bo.Config;
import ai.vacuity.rudi.adaptors.bo.IndexableQuery;
import ai.vacuity.rudi.adaptors.bo.Tuple;
import ai.vacuity.rudi.adaptors.hal.service.DispatchService;

/**
 * Listens to semantics in the Index on behalf of EventHandlers.
 * 
 * @author In Lak'ech.
 *
 */
public class GraphListener extends Thread implements RepositoryConnectionListener {
	private final static org.slf4j.Logger logger = LoggerFactory.getLogger(GraphListener.class);

	final static String indexDir = Config.DIR_ALERTS + "index" + File.separator;
	final static String queueDir = Config.DIR_ALERTS + "queue" + File.separator;

	final static Repository repository = GraphManager.parseSPARQLRepository(Config.SPARQL_ENDPOINT_ALERTS);
	private final static HashMap<Value, Vector<IndexableQuery>> map = new HashMap<Value, Vector<IndexableQuery>>();

	Vector<Tuple> tuples = new Vector<Tuple>();

	public GraphListener() {
		start(); // start observing the index
	}

	static {
		GraphListener.getRepository().initialize();
		try (RepositoryConnection con = GraphListener.getRepository().getConnection()) {
			Resource context = con.getValueFactory().createIRI(Constants.CONTEXT_DEMO);
			con.clear(context); // TODO careful, the listeners were in there

			File dir = new File(indexDir);
			if (!dir.exists()) {
				dir.mkdirs();
			}
			dir = new File(queueDir);
			if (!dir.exists()) {
				dir.mkdirs();
			}
		}
		catch (RDF4JException e) {
			logger.error(e.getMessage(), e);
		}
	}

	private static final int ITERATIONS = 5;
	private static final double MEG = (Math.pow(1024, 2));
	private static final int RECORD_COUNT = 4000000;
	private static final String RECORD = "Some test text\n";
	private static final int RECSIZE = RECORD.getBytes().length;

	public static void main(String[] args) throws Exception {
		// List<String> records = new ArrayList<String>(RECORD_COUNT);
		// int size = 0;
		// for (int i = 0; i < RECORD_COUNT; i++) {
		// records.add(RECORD);
		// size += RECSIZE;
		// }
		// System.out.println(records.size() + " 'records'");
		// System.out.println(size / MEG + " MB");
		//
		// for (int i = 0; i < ITERATIONS; i++) {
		// System.out.println("\nIteration " + i);
		// File file = File.createTempFile("foo", ".txt");
		//
		// writeRaw(records, file);
		// writeBuffered(records, file, 8192);
		// writeBuffered(records, file, (int) MEG);
		// writeBuffered(records, file, 4 * (int) MEG);
		// }
	}

	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(5000);
			}
			catch (InterruptedException e) {
				logger.error(e.getMessage(), e);
			}
			while (getTuples().size() > 0) {
				add(getTuples().remove(0));
			}
		}
	}

	private static void writeRaw(List<String> records, File file) throws IOException {
		try {
			FileWriter writer = new FileWriter(file);
			logger.debug("Writing raw... ");
			write(records, writer);
		}
		finally {
			// comment this out if you want to inspect the files afterward
			// file.delete();
		}
	}

	private static void writeBuffered(List<String> records, File file, int bufSize) throws IOException {
		try {
			FileWriter writer = new FileWriter(file);
			BufferedWriter bufferedWriter = new BufferedWriter(writer, bufSize);

			logger.debug("Writing buffered (buffer size: " + bufSize + ")... ");
			write(records, bufferedWriter);
		}
		finally {
			// comment this out if you want to inspect the files afterward
			// file.delete();
		}
	}

	private static void write(List<String> records, Writer writer) throws IOException {
		long start = System.currentTimeMillis();
		for (String record : records) {
			writer.write(record);
		}
		writer.flush();
		writer.close();
		long end = System.currentTimeMillis();
		logger.debug((end - start) / 1000f + " seconds");
	}

	public static final void register(IndexableQuery q) {
		SPARQLParserFactory factory = new SPARQLParserFactory();
		QueryParser parser = factory.getParser();
		ParsedQuery parsedQuery = parser.parseQuery(q.getDelegate().toString(), null);

		StatementPatternCollector collector = new StatementPatternCollector();
		TupleExpr tupleExpr = parsedQuery.getTupleExpr();
		tupleExpr.visit(collector);

		for (StatementPattern pattern : collector.getStatementPatterns()) {
			logger.debug(pattern.toString());
			Value s = pattern.getSubjectVar().getValue();
			Value p = pattern.getPredicateVar().getValue();
			Value o = pattern.getObjectVar().getValue();

			index(q, s);
			index(q, p);
			index(q, o);

			if (!getMap().containsKey(s)) {
				getMap().put(s, new Vector<IndexableQuery>());
			}
			if (!getMap().containsKey(p)) {
				getMap().put(p, new Vector<IndexableQuery>());
			}
			if (!getMap().containsKey(o)) {
				getMap().put(o, new Vector<IndexableQuery>());
			}
			getMap().get(s).add(q);
			getMap().get(p).add(q);
			getMap().get(o).add(q);
		}
	}

	private static void index(IndexableQuery query, Value value) {
		if (value != null) { // only index Values (i.e. not query variables)
			// logger.debug("Query: " + query.getId() + "" + query.getLabel() + " for value " + value.hashCode() + ":" + value.stringValue());
			try {
				File indexDirFile = new File(indexDir + value.hashCode() + File.separator);
				if (!indexDirFile.exists()) { // the two files are logically paired
					indexDirFile.mkdir();
				}
				File queryIndexFile = new File(indexDirFile.getAbsolutePath() + File.separator + query.getId());
				queryIndexFile.createNewFile();
				if (queryIndexFile.exists()) {
					for (int tries = 10; !queryIndexFile.delete(); tries--) {
						if (tries == 0) {
							logger.error("Failed to delete query index file: " + queryIndexFile.getAbsolutePath());
						}
					}
				}
				queryIndexFile.createNewFile();

				File queueQueueSrc = new File(queueDir + query.getId() + File.separator + "src" + File.separator + value.hashCode());
				if (!queueQueueSrc.getParentFile().exists()) {
					queueQueueSrc.getParentFile().mkdirs();
				}
				if (!queueQueueSrc.exists()) {
					queueQueueSrc.createNewFile();
				}
				File queryQueueTar = new File(queueDir + query.getId() + File.separator + "tar" + File.separator);
				if (!queryQueueTar.exists()) {
					queryQueueTar.mkdirs();
				}
				File queryQueueRQFile = new File(queueDir + query.getId() + ".rq");
				if (queryQueueRQFile.exists()) {
					for (int tries = 10; !queryQueueRQFile.delete(); tries--) {
						if (tries == 0) {
							logger.error("Failed to delete value queue file: " + queryQueueRQFile.getAbsolutePath());
						}
					}
				}
				queryQueueRQFile.createNewFile();
				ArrayList<String> al = new ArrayList<String>();
				al.add(query.getDelegate().toString());
				writeBuffered(al, queryQueueRQFile, (int) MEG);

				// renew(query.getId());
			}
			catch (IOException ioex) {
				logger.debug(ioex.getMessage(), ioex);
			}
		}
	}

	@Override
	public void close(RepositoryConnection conn) {
	}

	@Override
	public void setAutoCommit(RepositoryConnection conn, boolean autoCommit) {
	}

	@Override
	public void begin(RepositoryConnection conn) {
	}

	@Override
	public void commit(RepositoryConnection conn) {
	}

	@Override
	public void rollback(RepositoryConnection conn) {
	}

	@Override
	public void add(RepositoryConnection conn, Resource subject, IRI predicate, Value object, Resource... contexts) {
		// HashSet<String> seen = new HashSet<String>();
		// Vector<IndexableQuery> qs = getMap().get(subject);
		// Vector<IndexableQuery> qp = getMap().get(predicate);
		// Vector<IndexableQuery> qo = getMap().get(object);

		Tuple t = new Tuple();
		t.setConnection(conn);
		t.setSubject(subject);
		t.setPredicate(predicate);
		t.setObject(object);
		t.setContexts(contexts);
		getTuples().add(t); // will need to wait until the triple is added to the index before dispatching, since the dispatcher will need to execute a query to retrieve the triple
		// add(t);

		// int phash = predicate.hashCode();
		// int ohash = object.hashCode();
		//
		// for (int i = 0; i < qs.size(); i++) {
		// if (seen.contains(qp.get(i))) continue;
		// // execute the query
		// // seen.add(qs.get(i).getId());
		// }
		// for (int i = 0; i < qp.size(); i++) {
		// if (seen.contains(qp.get(i))) continue;
		// // seen.add(qp.get(i).getId());
		// }
		// for (int i = 0; i < qo.size(); i++) {
		// if (seen.contains(qo.get(i))) continue;
		// // seen.add(qo.get(i).getId());
		// }
	}

	private void add(Tuple t) {
		for (Resource context : t.getContexts()) {
			if (!GraphManager.getInbox().contains(context)) return;
			add(t.getSubject(), context);
			add(t.getPredicate(), context);
			add(t.getObject(), context);
		}
	}

	private void add(Value value, Resource context) {
		int shash = value.hashCode();
		File indexDirFile = new File(indexDir + shash + File.separator);
		if (indexDirFile.exists()) {
			// logger.debug("Hash value: " + shash + ":" + value.stringValue() + " @ " + context.hashCode() + ":" + context.stringValue());
			File[] queryLabelHashes = indexDirFile.listFiles();
			for (File queryLabelHashFile : queryLabelHashes) {
				int qhash = Integer.parseInt(queryLabelHashFile.getName());
				File queryQueue = new File(queueDir + queryLabelHashFile.getName() + File.separator + "tar" + File.separator + context.hashCode() + File.separator);
				File queuedValue = new File(queueDir + queryLabelHashFile.getName() + File.separator + "tar" + File.separator + context.hashCode() + File.separator + shash);
				if (!queuedValue.getParentFile().exists()) {
					queuedValue.getParentFile().mkdirs();
					renew(qhash, context); // renew the queue
				}
				if (queuedValue.exists()) {
					for (int tries = 10; !queuedValue.delete(); tries--) {
						if (tries == 0) {
							logger.error("Failed to delete value queue file: " + queuedValue.getAbsolutePath());
						}
					}
					if (queryQueue.list() != null && queryQueue.list().length <= 0) {
						// match found
						logger.debug("Match found: " + queryLabelHashFile + " for value '" + value.stringValue() + "' @ " + context.hashCode() + ":" + context.stringValue());
						// File queryQueueRQFile = new File(queueDir + queryLabelHashFile.getName() + ".rq"); // fetch the query and run it'

						try {
							DispatchService.process(qhash, context);
							renew(qhash, context); // renew the queue
							return;
						}
						catch (IllegalArgumentException e) {
							logger.error(e.getMessage(), e);
						}
						catch (IOException e) {
							logger.error(e.getMessage(), e);
						}
					}
				}
				else {
					// logger.debug("Queue doesn't exist: " + context.hashCode() + File.separator + shash + " for " + value.stringValue());
				}
			}
		}
	}

	public static void renew(int queryLabelHash, Resource context) {
		try {
			File src = new File(queueDir + queryLabelHash + File.separator + "src" + File.separator);
			if (!src.exists()) return;
			File[] files = src.listFiles();
			for (File f : files) {
				File tar = new File(queueDir + queryLabelHash + File.separator + "tar" + File.separator + context.hashCode() + File.separator + f.getName());
				tar.createNewFile();
				if (!tar.exists()) {
					logger.error("Could not create tar file: " + tar.getAbsolutePath());
				}
				Files.copy(f.toPath(), tar.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
		}
		catch (IOException ioex) {
			logger.error(ioex.getMessage(), ioex);
		}
	}

	public void query(Query q) {
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
	}

	@Override
	public void remove(RepositoryConnection conn, Resource subject, IRI predicate, Value object, Resource... contexts) {
	}

	@Override
	public void clear(RepositoryConnection conn, Resource... contexts) {
	}

	@Override
	public void setNamespace(RepositoryConnection conn, String prefix, String name) {
	}

	@Override
	public void removeNamespace(RepositoryConnection conn, String prefix) {
	}

	@Override
	public void clearNamespaces(RepositoryConnection conn) {
	}

	@Override
	public void execute(RepositoryConnection conn, QueryLanguage ql, String update, String baseURI, Update operation) {
	}

	public static HashMap<Value, Vector<IndexableQuery>> getMap() {
		return map;
	}

	public static Repository getRepository() {
		return repository;
	}

	public Vector<Tuple> getTuples() {
		return tuples;
	}

	public void setTuples(Vector<Tuple> tuples) {
		this.tuples = tuples;
	}

}
