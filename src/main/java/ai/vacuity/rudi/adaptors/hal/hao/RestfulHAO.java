package ai.vacuity.rudi.adaptors.hal.hao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.UUID;
import java.util.Vector;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.json.JSONObject;
import org.json.XML;
import org.slf4j.LoggerFactory;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;

import ai.vacuity.rudi.adaptors.bo.Config;

/**
 *
 * @author M.Vasudevarao
 * @author In Lak'ech.
 */
public class RestfulHAO extends AbstractHAO {
	private final static org.slf4j.Logger logger = LoggerFactory.getLogger(RestfulHAO.class);

	private float duration = -1;

	/**
	 * @param args
	 *            the command line arguments
	 */
	public void main(String[] args) {
		if (args.length != 3) {
			System.err.println("give command as follows : ");
			System.err.println("XSLTTest data.xml converted.xsl converted.html");
			return;
		}

		String json = "";
		String xslt = args[1];
		String rdf = args[2];

		RestfulHAO st = new RestfulHAO();
	}

	public String transform(String json) {
		if (json.startsWith("[") && json.endsWith("]")) {
			json = "{\"items\":\n" + json + "\n}";
		}
		if (StringUtils.isBlank(json)) return null;
		// logger.debug("Truncated JSON:\n" + json);
		return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + "<root>\n" + XML.toString(new JSONObject(json)) + "</root>\n";
	}

	public void transform(String json, String xslt, String host) throws TransformerConfigurationException, TransformerException, IOException {

		// logger.debug("Retrieved JSON:\n" + json);
		String xml = transform(json);
		if (xml == null) return;
		UUID uuid = UUID.randomUUID();
		String fxmlStr = Config.DIR_RESPONSES + host + File.separator + uuid + ".xml";

		File fxml = new File(fxmlStr);
		if (!fxml.getParentFile().exists()) {
			fxml.getParentFile().mkdirs();
		}
		// logger.debug("The generated XML file is:\n" + xml);

		TransformerFactory factory = TransformerFactory.newInstance();
		// File f = new File(xslt);
		URL oracle = new URL(xslt);
		BufferedReader xslin = new BufferedReader(new InputStreamReader(oracle.openStream()));
		StreamSource xslStream = new StreamSource(xslin);

		Transformer transformer = factory.newTransformer(xslStream);
		StringReader srxml = new StringReader(xml);

		String filePathStr = Config.DIR_RESPONSES + host + File.separator + uuid + ".rdf";
		FileWriter fw = new FileWriter(fxml);
		fw.write(xml);
		fw.close();
		File f = new File(filePathStr);
		if (!f.getParentFile().exists()) f.getParentFile().mkdirs();
		StreamSource in = new StreamSource(srxml);
		StreamResult out = new StreamResult(f);
		// logger.debug("The generated RDF file is: " + f.getAbsolutePath());
		transformer.transform(in, out);

		index(filePathStr);
		// QuadStore.main(new String[] {});

	}

	private void index(String response) {
		UUID replyId = UUID.randomUUID();
		IRI replyIRI = GraphManager.getValueFactory().createIRI(Constants.NS_VI + "r-" + replyId);

		UUID responseId = UUID.randomUUID();
		IRI responseIRI = GraphManager.getValueFactory().createIRI(Constants.NS_VI + "r-" + responseId);
		GraphManager.addToRepository(response, responseIRI);
		Vector<Statement> tuples = new Vector<Statement>();
		tuples.add(GraphManager.getValueFactory().createStatement(responseIRI, GraphManager.getValueFactory().createIRI(Constants.NS_RDF + "type"), GraphManager.getValueFactory().createIRI(Constants.NS_VIA + "Response")));
		tuples.add(GraphManager.getValueFactory().createStatement(responseIRI, GraphManager.getValueFactory().createIRI(Constants.NS_RDF + "type"), GraphManager.getValueFactory().createIRI(Constants.NS_NIX + "Communication_response")));
		tuples.add(GraphManager.getValueFactory().createStatement(responseIRI, GraphManager.getValueFactory().createIRI(Constants.NS_NIX + "response"), replyIRI));
		tuples.add(GraphManager.getValueFactory().createStatement(responseIRI, GraphManager.via_timestamp, GraphManager.getValueFactory().createLiteral(new Date())));
		tuples.add(GraphManager.getValueFactory().createStatement(responseIRI, GraphManager.getValueFactory().createIRI(Constants.NS_NIX + "duration"), GraphManager.getValueFactory().createLiteral(this.duration)));
		tuples.add(GraphManager.getValueFactory().createStatement(event.getIri(), GraphManager.getValueFactory().createIRI(Constants.NS_SIOC + "has_reply"), responseIRI));
		tuples.add(GraphManager.getValueFactory().createStatement(responseIRI, GraphManager.getValueFactory().createIRI(Constants.NS_NIX + "trigger"), event.getIri()));
		tuples.add(GraphManager.getValueFactory().createStatement(responseIRI, GraphManager.getValueFactory().createIRI(Constants.NS_NIX + "agent"), inputProtocol.getEventHandler().getIri()));
		tuples.add(GraphManager.getValueFactory().createStatement(responseIRI, GraphManager.getValueFactory().createIRI(Constants.NS_NIX + "means"), GraphManager.getValueFactory().createIRI((inputProtocol.getEventHandler().isSecure() ? "https://" : "http://") + inputProtocol.getEventHandler().getEndpointDomain())));
		GraphManager.addToRepository(tuples, event.getIri());

		if (inputProtocol.getEventHandler().getResponseModule() != null) {
			inputProtocol.getEventHandler().getResponseModule().run(responseIRI, inputProtocol, event);
		}
	}

	// FROM API CLIENT

	static final int MAX_RESULTS = 3;
	static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
	static final JsonFactory JSON_FACTORY = new JacksonFactory();

	@Override
	public void run() {

		HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
			@Override
			public void initialize(HttpRequest request) {
				request.setParser(new JsonObjectParser(JSON_FACTORY));
			}
		});
		// PlusUrl url = PlusUrl.listPublicActivities(USER_ID).setMaxResults(MAX_RESULTS);
		// url.put("fields", "items(id,url,object(content,plusoners/totalItems))");

		try {
			GenericUrl gurl = new GenericUrl(new URL(call));
			HttpRequest request = requestFactory.buildGetRequest(gurl);
			long start = System.currentTimeMillis();
			HttpResponse hr = request.execute();
			long end = System.currentTimeMillis();
			this.duration = ((end - start) / 1000f);
			parseResponse(hr);
		}
		catch (MalformedURLException muex) {
			logger.error(muex.getMessage(), muex);
		}
		catch (IOException iex) {
			logger.error(iex.getMessage(), iex);
		}

	}

	void parseResponse(HttpResponse response) throws IOException {
		// ActivityFeed feed = response.parseAs(ActivityFeed.class);

		// Call.ActivityFeed feed = new Call.ActivityFeed();
		try {
			String resp = response.parseAsString();
			if (inputProtocol.getEventHandler().hasEndpointResponseModule()) {
				inputProtocol.getEventHandler().getEndpointResponseModule().process(resp, inputProtocol, event);
				resp = inputProtocol.getEventHandler().getEndpointResponseModule().getResponse();
				event = inputProtocol.getEventHandler().getEndpointResponseModule().getEvent();
			}
			// if (StringUtils.isNotBlank(resp)) resp = resp.replace("${0}", input);

			String xslt = null;
			if (inputProtocol.getEventHandler().hasTranslator()) {
				xslt = inputProtocol.getEventHandler().getTranslator().build();
				transform(resp, xslt, response.getRequest().getUrl().getHost());
			}
			else {
				UUID uuid = UUID.randomUUID();
				String fxmlStr = Config.DIR_RESPONSES + response.getRequest().getUrl().getHost() + File.separator + uuid + ".rdf";
				File fxml = new File(fxmlStr);
				if (!fxml.getParentFile().exists()) {
					fxml.getParentFile().mkdirs();
				}
				fxml.createNewFile();
				FileWriter fw = new FileWriter(fxml);
				fw.write(resp);
				fw.close();

				index(fxmlStr);
			}
		}
		catch (TransformerException tex) {
			logger.debug(tex.getMessage(), tex);
		}

	}
}