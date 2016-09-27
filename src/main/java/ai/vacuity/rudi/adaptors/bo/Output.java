package ai.vacuity.rudi.adaptors.bo;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;

public class Output {

	String endpointLabel = null;
	HttpRequest request = null;
	String call = "";

	public String getCall() {
		return call;
	}

	public void setCall(String call) {
		this.call = call;
	}

	GenericUrl translator = null;
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

	public GenericUrl getTranslator() {
		return translator;
	}

	public void setTranslator(GenericUrl translator) {
		this.translator = translator;
	}

	public boolean hasTranslator() {
		return this.translator != null;
	}

	public String getLog() {
		return log;
	}

	public void setLog(String log) {
		this.log = log;
	}

}
