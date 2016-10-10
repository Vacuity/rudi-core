package ai.vacuity.rudi.adaptors.bo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.Repository;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.common.net.MediaType;

import ai.vacuity.rudi.adaptors.interfaces.IResponseModule;
import ai.vacuity.rudi.adaptors.interfaces.ITemplateModule;

public class EventHandler {

	private static HashMap<String, Repository> repos = new HashMap<String, Repository>();

	String configLabel = null;
	HttpRequest request = null;
	String call = "";
	MediaType contentType = null;
	IRI iri = null;
	List<String> imports = new ArrayList<String>();

	// Repository repository = null;
	String sparql = null;
	boolean hasSparqlQuery = false;

	public String getCall() {
		return call;
	}

	public void setCall(String call) {
		this.call = call;
	}

	public boolean isSecure() {
		return Config.get(getConfigLabel()).isSecure();
	}

	public boolean hasEndpointTemplateModule() {
		return Config.get(getConfigLabel()).hasTemplateModule();
	}

	public ITemplateModule getEndpointTemplateModule() {
		return Config.get(getConfigLabel()).getTemplateModule();
	}

	public boolean hasEndpointResponseModule() {
		return Config.get(getConfigLabel()).hasResponseModule();
	}

	public IResponseModule getEndpointResponseModule() {
		return Config.get(getConfigLabel()).getResponseModule();
	}

	public boolean hasEndpointKey() {
		return Config.get(getConfigLabel()).hasKey();
	}

	public boolean hasEndpointId() {
		return Config.get(getConfigLabel()).hasId();
	}

	public boolean hasEndpointToken() {
		return Config.get(getConfigLabel()).hasToken();
	}

	public String getEndpointKey() {
		return Config.get(getConfigLabel()).getKey();
	}

	public String getEndpointId() {
		return Config.get(getConfigLabel()).getId();
	}

	public String getEndpointDomain() {
		return Config.get(getConfigLabel()).getHost();
	}

	public int getEndpointPort() {
		return Config.get(getConfigLabel()).getPort();
	}

	public String getEndpointToken() {
		return Config.get(getConfigLabel()).getToken();
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
		return EventHandler.repos.get(getConfigLabel());
	}

	public void addRepository(Repository repository) {
		if (getRepository() != null) return;
		repository.initialize();
		EventHandler.repos.put(getConfigLabel(), repository);
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

	public IRI getIri() {
		return iri;
	}

	public void setIri(IRI iri) {
		this.iri = iri;
	}

	public List<String> getImports() {
		return imports;
	}

	public void setImports(List<String> imports) {
		this.imports = imports;
	}

}
