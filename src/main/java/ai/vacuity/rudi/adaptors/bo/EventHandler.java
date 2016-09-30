package ai.vacuity.rudi.adaptors.bo;

import org.eclipse.rdf4j.repository.Repository;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.common.net.MediaType;

public class EventHandler {

	String configLabel = null;
	HttpRequest request = null;
	String call = "";
	MediaType contentType = null;

	Repository repository = null;
	String sparql = null;
	boolean hasSparqlQuery = false;

	public String getCall() {
		return call;
	}

	public void setCall(String call) {
		this.call = call;
	}

	GenericUrl translator = null;
	String log = "";

	public String getConfigLabel() {
		return configLabel;
	}

	public void setConfigLabel(String configLabel) {
		this.configLabel = configLabel;
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

	public MediaType getContentType() {
		return contentType;
	}

	public void setContentType(MediaType contentType) {
		this.contentType = contentType;
	}

	public Repository getRepository() {
		return repository;
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
		this.repository.initialize();
	}

	public String getSparql() {
		return sparql;
	}

	public void setSparql(String sparql) {
		this.sparql = sparql;
	}

	public boolean hasSparqlQuery() {
		return hasSparqlQuery;
	}

	public void hasSparqlQuery(boolean isSparqlQuery) {
		this.hasSparqlQuery = isSparqlQuery;
	}

}
