/*
 * Copyright (c) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package ai.vacuity.rudi.adaptors.controller;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.UUID;

import javax.xml.transform.TransformerException;

import org.apache.http.client.fluent.Request;
import org.slf4j.LoggerFactory;
import org.zalando.stups.tokens.AccessTokens;
import org.zalando.stups.tokens.Tokens;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Key;

/**
 * Simple example that demonstrates how to use <a href="code.google.com/p/google-http-java-client/">Google HTTP Client Library for Java</a> with the <a href="https://developers.google.com/+/api/">Google+ API</a>.
 *
 * <p>
 * Note that in the case of the Google+ API, there is a much better custom library built on top of this HTTP library that is much easier to use and hides most of these details for you. See <a href= "http://code.google.com/p/google-api-java-client/wiki/APIs#Google+_API"> Google+ API for Java</a>.
 * </p>
 *
 * @author Yaniv Inbar
 */
public class APIClient {
	private final static org.slf4j.Logger logger = LoggerFactory.getLogger(APIClient.class);

	private static final String API_KEY = "AIzaSyAdL9AxLigcHqPqp0sf68bkel4hXQ92KYE";

	private static final String USER_ID = "116899029375914044550";
	private static final int MAX_RESULTS = 3;

	static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
	static final JsonFactory JSON_FACTORY = new JacksonFactory();

	/** Feed of Google+ activities. */
	public static class ActivityFeed {

		/** List of Google+ activities. */
		@Key("items")
		private List<Activity> activities;

		public List<Activity> getActivities() {
			return activities;
		}
	}

	/** Google+ activity. */
	public static class Activity extends GenericJson {

		/** Activity URL. */
		@Key
		private String url;

		public String getUrl() {
			return url;
		}

		/** Activity object. */
		@Key("object")
		private ActivityObject activityObject;

		public ActivityObject getActivityObject() {
			return activityObject;
		}
	}

	/** Google+ activity object. */
	public static class ActivityObject {

		/** HTML-formatted content. */
		@Key
		private String content;

		public String getContent() {
			return content;
		}

		/** People who +1'd this activity. */
		@Key
		private PlusOners plusoners;

		public PlusOners getPlusOners() {
			return plusoners;
		}
	}

	/** People who +1'd an activity. */
	public static class PlusOners {

		/** Total number of people who +1'd this activity. */
		@Key
		private long totalItems;

		public long getTotalItems() {
			return totalItems;
		}
	}

	/** Google+ URL. */
	public static class PlusUrl extends GenericUrl {

		public PlusUrl(String encodedUrl) {
			super(encodedUrl);
		}

		@SuppressWarnings("unused")
		@Key
		private final String key = API_KEY;

		/** Maximum number of results. */
		@Key
		private int maxResults;

		public int getMaxResults() {
			return maxResults;
		}

		public PlusUrl setMaxResults(int maxResults) {
			this.maxResults = maxResults;
			return this;
		}

		/** Lists the public activities for the given Google+ user ID. */
		public static PlusUrl listPublicActivities(String userId) {
			return new PlusUrl("https://www.googleapis.com/plus/v1/people/" + userId + "/activities/public");
		}
	}

	static String xslt = null;
	static String call = null;

	public static String getXslt() {
		return xslt;
	}

	public static void setXslt(String xslt) {
		APIClient.xslt = xslt;
	}

	public static String getCall() {
		return call;
	}

	public static void setCall(String call) {
		APIClient.call = call;
	}

	private static void parseResponse(HttpResponse response) throws IOException {
		// ActivityFeed feed = response.parseAs(ActivityFeed.class);

		ActivityFeed feed = new ActivityFeed();
		if (xslt == null) {
			UUID uuid = UUID.randomUUID();
			String fxmlStr = "/Users/smonroe/workspace/rudi-adaptors/src/main/webapp/WEB-INF/resources/adaptors/" + response.getRequest().getUrl().getHost() + "-" + uuid + ".rdf";
			File fxml = new File(fxmlStr);
			fxml.createNewFile();
			FileWriter fw = new FileWriter(fxml);
			fw.write(response.parseAsString());
			fw.close();
		}
		else {
			try {
				XSLTTransformer.transform(response.parseAsString(), xslt);
			}
			catch (TransformerException tex) {
				logger.debug(tex.getMessage(), tex);
			}
		}

		if (feed.getActivities() == null) return;
		if (feed.getActivities().isEmpty()) {
			System.out.println("No activities found.");
		}
		else {
			if (feed.getActivities().size() == MAX_RESULTS) {
				logger.debug("First ");
			}
			System.out.println(feed.getActivities().size() + " activities found:");
			for (Activity activity : feed.getActivities()) {
				logger.debug("\n");
				logger.debug("-----------------------------------------------");
				logger.debug("HTML Content: " + activity.getActivityObject().getContent());
				logger.debug("+1's: " + activity.getActivityObject().getPlusOners().getTotalItems());
				logger.debug("URL: " + activity.getUrl());
				logger.debug("ID: " + activity.get("id"));
			}
		}
	}

	public static void run() throws IOException {
		HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
			@Override
			public void initialize(HttpRequest request) {
				request.setParser(new JsonObjectParser(JSON_FACTORY));
			}
		});
		PlusUrl url = PlusUrl.listPublicActivities(USER_ID).setMaxResults(MAX_RESULTS);
		url.put("fields", "items(id,url,object(content,plusoners/totalItems))");

		GenericUrl gurl = new GenericUrl(new URL(call));

		HttpRequest request = requestFactory.buildGetRequest(gurl);
		parseResponse(request.execute());
	}

	public static void main(String[] args) {

		try {
			AccessTokens tokens = Tokens.createAccessTokensWithUri(new URI("https://example.com/access_tokens")).manageToken("exampleRW").addScope("read").addScope("write").done().manageToken("exampleRO").addScope("read").done().start();

			while (true) {
				final String token = tokens.get("exampleRO");

				Request.Get("https://api.example.com").addHeader("Authorization", "Bearer " + token).execute();

				Thread.sleep(1000);
			}
		}
		catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}

		// if (API_KEY.startsWith("Enter ")) {
		// System.err.println(API_KEY);
		// System.exit(1);
		// }
		// try {
		// try {
		// run();
		// return;
		// }
		// catch (HttpResponseException e) {
		// System.err.println(e.getMessage());
		// }
		// }
		// catch (Throwable t) {
		// t.printStackTrace();
		// }
		// System.exit(1);
	}
}
