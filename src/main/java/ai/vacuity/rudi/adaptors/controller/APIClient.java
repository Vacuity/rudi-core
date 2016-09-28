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

import java.net.URI;

import org.apache.http.client.fluent.Request;
import org.slf4j.LoggerFactory;
import org.zalando.stups.tokens.AccessTokens;
import org.zalando.stups.tokens.Tokens;

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
	final static org.slf4j.Logger logger = LoggerFactory.getLogger(APIClient.class);

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
