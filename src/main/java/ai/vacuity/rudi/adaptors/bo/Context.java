package ai.vacuity.rudi.adaptors.bo;

import java.util.Collection;
import java.util.HashMap;

import org.eclipse.rdf4j.model.IRI;

import ai.vacuity.rudi.adaptors.types.Prompt;

public class Context extends Query {

	public Context(org.eclipse.rdf4j.query.Query delegate) {
		super(delegate);
	}

	HashMap<String, Prompt> prompts = new HashMap<String, Prompt>();

	IRI listenerContext = null;

	public boolean hasPrompts() {
		return getPrompts().size() > 0;
	}

	public void addPrompt(Prompt prompt) {
		this.prompts.put(prompt.getIri(), prompt);
	}

	public Prompt getPrompt(String iri) {
		return this.prompts.get(iri);
	}

	public Collection<Prompt> getPrompts() {
		return this.prompts.values();
	}

	public void setPrompts(HashMap<String, Prompt> prompts) {
		this.prompts = prompts;
	}

	public IRI getListenerContext() {
		return listenerContext;
	}

	public void setListenerContext(IRI listenerContext) {
		this.listenerContext = listenerContext;
	}

}
