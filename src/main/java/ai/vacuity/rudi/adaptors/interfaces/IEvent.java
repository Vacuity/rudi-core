package ai.vacuity.rudi.adaptors.interfaces;

import org.eclipse.rdf4j.model.IRI;

public interface IEvent {

	String getLabel();

	void setLabel(String label);

	IRI getIri();

	void setIri(IRI iri);

	IRI getOwnerIri();

	void setOwnerIri(IRI iri);

}