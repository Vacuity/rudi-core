package ai.vacuity.rudi.adaptors.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
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

import ai.vacuity.rudi.adaptors.bo.Endpoint;
import ai.vacuity.rudi.adaptors.bo.Input;
import ai.vacuity.rudi.adaptors.data.QuadStore;

/**
 *
 * @author M.Vasudevarao
 * @author smonroe
 */
public class Call {
	private final static org.slf4j.Logger logger = LoggerFactory.getLogger(Call.class);

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

		// String dataXML = args[0];
		String xslt = args[1];
		String rdf = args[2];

		Call st = new Call();
		// try
		// {
		// st.transform(json, xslt, rdf);
		// }
		// catch (TransformerConfigurationException e)
		// {
		// System.err.println("TransformerConfigurationException");
		// System.err.println(e);
		// }
		// catch (TransformerException e)
		// {
		// System.err.println("TransformerException");
		// System.err.println(e);
		// }
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

		String filePathStr = xslt.replace(".xsl", "-" + uuid + ".rdf").replace("http://localhost:8080/rudi-adaptors/a/", "/Users/smonroe/workspace/rudi-adaptors/src/main/webapp/WEB-INF/resources/adaptors/");
		String fxmlStr = xslt.replace(".xsl", "-" + uuid + ".xml").replace("http://localhost:8080/rudi-adaptors/a/", "/Users/smonroe/workspace/rudi-adaptors/src/main/webapp/WEB-INF/resources/adaptors/");
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

		QuadStore.addToRepository(filePathStr, "http://tryrudi.io/rdf/demo/");

		// QuadStore.main(new String[] {});

	}

	// FROM APICLIENT

	static final int MAX_RESULTS = 3;
	static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
	static final JsonFactory JSON_FACTORY = new JacksonFactory();

	// /** Feed of Google+ activities. */
	// public static class ActivityFeed {
	//
	// /** List of Google+ activities. */
	// @Key("items")
	// private List<Activity> activities;
	//
	// public List<Activity> getActivities() {
	// return activities;
	// }
	// }

	// /** Google+ activity. */
	// public static class Activity extends GenericJson {
	//
	// /** Activity URL. */
	// @Key
	// private String url;
	//
	// public String getUrl() {
	// return url;
	// }
	//
	// /** Activity object. */
	// @Key("object")
	// private ActivityObject activityObject;
	//
	// public ActivityObject getActivityObject() {
	// return activityObject;
	// }
	// }
	//
	// /** Google+ activity object. */
	// public static class ActivityObject {
	//
	// /** HTML-formatted content. */
	// @Key
	// private String content;
	//
	// public String getContent() {
	// return content;
	// }
	//
	// /** People who +1'd this activity. */
	// @Key
	// private PlusOners plusoners;
	//
	// public PlusOners getPlusOners() {
	// return plusoners;
	// }
	// }
	//
	// /** People who +1'd an activity. */
	// public static class PlusOners {
	//
	// /** Total number of people who +1'd this activity. */
	// @Key
	// private long totalItems;
	//
	// public long getTotalItems() {
	// return totalItems;
	// }
	// }

	// /** Google+ URL. */
	// public static class PlusUrl extends GenericUrl {
	//
	// public PlusUrl(String encodedUrl) {
	// super(encodedUrl);
	// }
	//
	// @SuppressWarnings("unused")
	// @Key
	// private final String key = API_KEY;
	//
	// /** Maximum number of results. */
	// @Key
	// private int maxResults;
	//
	// public int getMaxResults() {
	// return maxResults;
	// }
	//
	// public PlusUrl setMaxResults(int maxResults) {
	// this.maxResults = maxResults;
	// return this;
	// }
	//
	// /** Lists the public activities for the given Google+ user ID. */
	// public static PlusUrl listPublicActivities(String userId) {
	// return new PlusUrl("https://www.googleapis.com/plus/v1/people/" + userId + "/activities/public");
	// }
	// }

	// static String xslt = null;
	static String call = null;
	static Input inputProtocols = null;
	static String input = null;
	//
	// public static String getXslt() {
	// return xslt;
	// }
	//
	// public static void setXslt(String xslt) {
	// Call.xslt = xslt;
	// }

	public static String getInput() {
		return input;
	}

	public static void setInput(String target) {
		Call.input = target;
	}

	public static Input getInputProtocols() {
		return inputProtocols;
	}

	public static void setInputProtocols(Input input) {
		Call.inputProtocols = input;
	}

	public static String getCall() {
		return call;
	}

	public static void setCall(String call) {
		Call.call = call;
	}

	public static void run() throws IOException {

		HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
			@Override
			public void initialize(HttpRequest request) {
				request.setParser(new JsonObjectParser(JSON_FACTORY));
			}
		});
		// PlusUrl url = PlusUrl.listPublicActivities(USER_ID).setMaxResults(MAX_RESULTS);
		// url.put("fields", "items(id,url,object(content,plusoners/totalItems))");

		GenericUrl gurl = new GenericUrl(new URL(call));

		HttpRequest request = requestFactory.buildGetRequest(gurl);
		Call.parseResponse(request.execute());
	}

	static void parseResponse(HttpResponse response) throws IOException {
		// ActivityFeed feed = response.parseAs(ActivityFeed.class);

		// Call.ActivityFeed feed = new Call.ActivityFeed();
		try {
			String resp = response.parseAsString();
			if (Endpoint.getEndpointmap().get(inputProtocols.getOutput().getEndpointLabel()).hasResponseProcessor()) {
				Endpoint.getEndpointmap().get(inputProtocols.getOutput().getEndpointLabel()).getResponseProcessor().process(resp, input);
				resp = Endpoint.getEndpointmap().get(inputProtocols.getOutput().getEndpointLabel()).getResponseProcessor().getResponse();
				input = Endpoint.getEndpointmap().get(inputProtocols.getOutput().getEndpointLabel()).getResponseProcessor().getInput();
			}
			// if (StringUtils.isNotBlank(resp)) resp = resp.replace("${0}", input);

			String xslt = null;
			if (inputProtocols.getOutput().hasTranslator()) {
				xslt = inputProtocols.getOutput().getTranslator().build();
				transform(resp, xslt);
			}
			else {
				UUID uuid = UUID.randomUUID();
				String fxmlStr = "/Users/smonroe/workspace/rudi-adaptors/src/main/webapp/WEB-INF/resources/adaptors/" + response.getRequest().getUrl().getHost() + "-" + uuid + ".rdf";
				File fxml = new File(fxmlStr);
				fxml.createNewFile();
				FileWriter fw = new FileWriter(fxml);
				fw.write(resp);
				fw.close();
			}
		}
		catch (TransformerException tex) {
			APIClient.logger.debug(tex.getMessage(), tex);
		}

		// if (feed.getActivities() == null) return;
		// if (feed.getActivities().isEmpty()) {
		// System.out.println("No activities found.");
		// }
		// else {
		// if (feed.getActivities().size() == Call.MAX_RESULTS) {
		// APIClient.logger.debug("First ");
		// }
		// System.out.println(feed.getActivities().size() + " activities found:");
		// for (Call.Activity activity : feed.getActivities()) {
		// APIClient.logger.debug("\n");
		// APIClient.logger.debug("-----------------------------------------------");
		// APIClient.logger.debug("HTML Content: " + activity.getActivityObject().getContent());
		// APIClient.logger.debug("+1's: " + activity.getActivityObject().getPlusOners().getTotalItems());
		// APIClient.logger.debug("URL: " + activity.getUrl());
		// APIClient.logger.debug("ID: " + activity.get("id"));
		// }
		// }
	}
}