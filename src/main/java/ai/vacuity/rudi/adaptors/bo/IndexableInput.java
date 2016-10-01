package ai.vacuity.rudi.adaptors.bo;

import org.eclipse.rdf4j.model.IRI;

import ai.vacuity.rudi.adaptors.interfaces.IEvent;

public class IndexableInput implements IEvent {

	private IRI ownerIri = null;
	private IRI iri = null;
	private String input = null;

	public IndexableInput(IRI userIri, IRI iri, String input) {
		this.input = input;
		this.iri = iri;
		this.ownerIri = userIri;
	}

	@Override
	public String getLabel() {
		return this.input;
	}

	@Override
	public void setLabel(String input) {
		this.input = input;
	}

	@Override
	public IRI getIri() {
		return this.iri;
	}

	@Override
	public void setIri(IRI iri) {
		this.iri = iri;
	}

	public IRI getOwnerIri() {
		return ownerIri;
	}

	public void setOwnerIri(IRI userIri) {
		this.ownerIri = userIri;
	}

}
