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

package ai.vacuity.rudi.adaptors.hal.service;

import java.io.IOException;
import java.util.Arrays;

import org.slf4j.LoggerFactory;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;

import ai.vacuity.rudi.adaptors.bo.Config;

/**
 * A sample application that demonstrates how the Google OAuth2 library can be used to authenticate against Daily Motion.
 *
 * @author Ravi Mistry, In Lak'ech.
 */
public class TokenService {
	private final static org.slf4j.Logger logger = LoggerFactory.getLogger(TokenService.class);

	static final String CONFIG_LABEL = "dm";
	static final String PROPERTY_KEY = "dm.key";
	static final String PROPERTY_TOKEN = "dm.token";
	static final String PROPERTY_PORT = "dm.port";
	static final String PROPERTY_DOMAIN = "dm.domain";

	/** Directory to store user credentials. */
	private static final java.io.File DATA_STORE_DIR = new java.io.File(System.getProperty("user.home"), ".store/dailymotion_sample");

	/**
	 * Global instance of the {@link DataStoreFactory}. The best practice is to make it a single globally shared instance across your application.
	 */
	private static FileDataStoreFactory DATA_STORE_FACTORY;

	/** OAuth 2 scope. */
	private static final String SCOPE = "read";

	/** Global instance of the HTTP transport. */
	private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

	/** Global instance of the JSON factory. */
	static final JsonFactory JSON_FACTORY = new JacksonFactory();

	private static final String TOKEN_SERVER_URL = "https://api.dailymotion.com/oauth/token";
	private static final String AUTHORIZATION_SERVER_URL = "https://api.dailymotion.com/oauth/authorize";

	/** Authorizes the installed application to access user's protected data. */
	private static Credential authorize() throws Exception {
		// OAuth2ClientCredentials.errorIfNotSpecified();
		// set up authorization code flow
		Config config = Config.get(TokenService.CONFIG_LABEL);
		AuthorizationCodeFlow flow = new AuthorizationCodeFlow.Builder(BearerToken.authorizationHeaderAccessMethod(), HTTP_TRANSPORT, JSON_FACTORY, new GenericUrl(TOKEN_SERVER_URL), new ClientParametersAuthentication(config.getKey(), config.getToken()), config.getKey(), AUTHORIZATION_SERVER_URL).setScopes(Arrays.asList(SCOPE)).setDataStoreFactory(DATA_STORE_FACTORY).build();
		// authorize
		LocalServerReceiver receiver = new LocalServerReceiver.Builder().setHost(config.getHost()).setPort(config.getPort()).build();
		return new AuthorizationCodeInstalledApp(flow, receiver).authorize("me");
	}

	private static void run(HttpRequestFactory requestFactory) throws IOException {
		ServiceUrl url = new ServiceUrl("https://api.dailymotion.com/videos/favorites");
		url.setFields("id,tags,title,url");

		// HttpRequest request = requestFactory.buildGetRequest(url);
		// VideoFeed videoFeed = request.execute().parseAs(VideoFeed.class);
		// if (videoFeed.list.isEmpty()) {
		// logger.debug("No favorite videos found.");
		// }
		// else {
		// if (videoFeed.hasMore) {
		// logger.debug("First ");
		// }
		// logger.debug(videoFeed.list.size() + " favorite videos found:");
		// for (Video video : videoFeed.list) {
		// logger.debug("\n");
		// logger.debug("-----------------------------------------------");
		// logger.debug("ID: " + video.id);
		// logger.debug("Title: " + video.title);
		// logger.debug("Tags: " + video.tags);
		// logger.debug("URL: " + video.url);
		// }
		// }
	}

	public static void main(String[] args) {
		try {
			DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
			final Credential credential = authorize();
			HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
				@Override
				public void initialize(HttpRequest request) throws IOException {
					credential.initialize(request);
					request.setParser(new JsonObjectParser(JSON_FACTORY));
				}
			});
			run(requestFactory);
			// Success!
			return;
		}
		catch (IOException e) {
			logger.debug(e.getMessage(), e);
		}
		catch (Throwable t) {
			logger.debug(t.getMessage(), t);
		}
		System.exit(1);
	}
}
