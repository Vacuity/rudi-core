package ai.vacuity.rudi.adaptors.interfaces;

import org.eclipse.rdf4j.model.Namespace;

public interface INamespaceProvider {

	public Namespace getNamespace(String qname);

}
