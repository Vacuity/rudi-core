package ai.vacuity.rudi.adaptors.bo;

import org.eclipse.rdf4j.model.Namespace;

public class Label {

	private Namespace namespace;
	private String localPart;
	private String label;
	int index;

	public Namespace getNamespace() {
		return namespace;
	}

	public void setNamespace(Namespace iri) {
		this.namespace = iri;
	}

	public String getQname() {
		return getNamespace().getName() + ":" + getLocalPart();
	}

	public String getIRI() {
		return getNamespace().getPrefix() + getLocalPart();
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String text) {
		this.label = text;
	}

	public String getLocalPart() {
		return localPart;
	}

	public void setLocalPart(String localName) {
		this.localPart = localName;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

}
