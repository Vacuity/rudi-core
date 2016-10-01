package ai.vacuity.rudi.adaptors.hal.hao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringUtils;
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
import ai.vacuity.rudi.adaptors.bo.IndexableInput;

/**
 *
 * @author M.Vasudevarao
 * @author In Lak'ech.
 */
public class RestfulHAO extends AbstractHAO {
	private final static org.slf4j.Logger logger = LoggerFactory.getLogger(RestfulHAO.class);

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String[] args) {
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

	public static String transform(String json) {
		if (json.startsWith("[") && json.endsWith("]")) {
			json = "{\"items\":\n" + json + "\n}";
		}
		if (StringUtils.isBlank(json)) return null;
		logger.debug("Truncated JSON:\n" + json);
		return "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" + "<root>\n" + XML.toString(new JSONObject(json)) + "</root>\n";
	}

	public static void transform(String json, String xslt) throws TransformerConfigurationException, TransformerException, IOException {

		logger.debug("Retrieved JSON:\n" + json);
		String xml = transform(json);
		if (xml == null) return;
		logger.debug("The generated XML file is:\n" + xml);
		TransformerFactory factory = TransformerFactory.newInstance();
		// File f = new File(xslt);
		URL oracle = new URL(xslt);
		BufferedReader xslin = new BufferedReader(new InputStreamReader(oracle.openStream()));
		StreamSource xslStream = new StreamSource(xslin);

		Transformer transformer = factory.newTransformer(xslStream);
		StringReader srxml = new StringReader(xml);
		UUID uuid = UUID.randomUUID();

		String filePathStr = xslt.replace(".xsl", "-" + uuid + ".rdf").replace("http://localhost:8080/rudi-adaptors/a/", SparqlHAO.DIR_LISTENERS);
		String fxmlStr = xslt.replace(".xsl", "-" + uuid + ".xml").replace("http://localhost:8080/rudi-adaptors/a/", SparqlHAO.DIR_LISTENERS);
		File fxml = new File(fxmlStr);
		FileWriter fw = new FileWriter(fxml);
		fw.write(xml);
		fw.close();
		File f = new File(filePathStr);
		if (!f.getParentFile().exists()) f.getParentFile().mkdirs();
		StreamSource in = new StreamSource(srxml);
		StreamResult out = new StreamResult(f);
		logger.debug("The generated RDF file is:\n");
		transformer.transform(in, out);

		SparqlHAO.addToRepository(filePathStr, event.getIri());
		// QuadStore.main(new String[] {});

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
			RestfulHAO.parseResponse(request.execute());
		}
		catch (MalformedURLException muex) {
			logger.error(muex.getMessage(), muex);
		}
		catch (IOException iex) {
			logger.error(iex.getMessage(), iex);
		}

	}

	static void parseResponse(HttpResponse response) throws IOException {
		// ActivityFeed feed = response.parseAs(ActivityFeed.class);

		// Call.ActivityFeed feed = new Call.ActivityFeed();
		try {
			String resp = response.parseAsString();
			if (Config.getMap().get(inputProtocol.getEventHandler().getConfigLabel()).hasResponseProcessor()) {
				Config.getMap().get(inputProtocol.getEventHandler().getConfigLabel()).getResponseProcessor().process(resp, event);
				resp = Config.getMap().get(inputProtocol.getEventHandler().getConfigLabel()).getResponseProcessor().getResponse();
				event = Config.getMap().get(inputProtocol.getEventHandler().getConfigLabel()).getResponseProcessor().getEvent();
			}
			// if (StringUtils.isNotBlank(resp)) resp = resp.replace("${0}", input);

			String xslt = null;
			if (inputProtocol.getEventHandler().hasTranslator()) {
				xslt = inputProtocol.getEventHandler().getTranslator().build();
				transform(resp, xslt);
			}
			else {
				UUID uuid = UUID.randomUUID();
				String fxmlStr = SparqlHAO.DIR_LISTENERS + response.getRequest().getUrl().getHost() + "-" + uuid + ".rdf";
				File fxml = new File(fxmlStr);
				fxml.createNewFile();
				FileWriter fw = new FileWriter(fxml);
				fw.write(resp);
				fw.close();
			}
		}
		catch (TransformerException tex) {
			logger.debug(tex.getMessage(), tex);
		}

	}
}