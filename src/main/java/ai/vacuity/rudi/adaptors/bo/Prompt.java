package ai.vacuity.rudi.adaptors.bo;

import org.eclipse.rdf4j.model.IRI;

public class Prompt {

	IRI iri = null;
	String label = null;

	public IRI getIri() {
		return iri;
	}

	public void setIri(IRI iri) {
		this.iri = iri;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

}
