package ai.vacuity.rudi.adaptors.interfaces;

import org.eclipse.rdf4j.model.IRI;

import ai.vacuity.rudi.adaptors.bo.InputProtocol;

/**
 * Swaps placeholders in <via:Output/> elements, e.g. call (json, rdf, etc.), and translator values.
 * 
 * @author In Lak'ech.
 *
 */
public interface IResponseModule {
	public void run(IRI responseIRI, InputProtocol ip, IndexableEvent event);

	public void process(String response, InputProtocol ip, IndexableEvent event);

	public String getResponse();

	public IndexableEvent getEvent();
}
