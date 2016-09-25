package ai.vacuity.rudi.adaptors.bo;

import java.net.URL;

import com.google.api.client.http.HttpRequest;

public class Output {

	String endpointLabel = null;
	HttpRequest request = null;
	URL translator = null;
	String log = "";

	public String getEndpointLabel() {
		return endpointLabel;
	}

	public void setEndpointLabel(String endpointLabel) {
		this.endpointLabel = endpointLabel;
	}

	public HttpRequest getRequest() {
		return request;
	}

	public void setRequest(HttpRequest request) {
		this.request = request;
	}

	public URL getTranslator() {
		return translator;
	}

	public void setTranslator(URL translator) {
		this.translator = translator;
	}

	public String getLog() {
		return log;
	}

	public void setLog(String log) {
		this.log = log;
	}

}
