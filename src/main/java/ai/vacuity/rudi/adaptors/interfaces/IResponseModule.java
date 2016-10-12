package ai.vacuity.rudi.adaptors.interfaces;

/**
 * Swaps placeholders in <via:Output/> elements, e.g. call (json, rdf, etc.), and translator values.
 * 
 * @author In Lak'ech.
 *
 */
public interface IResponseModule {
	public void process(String response, IndexableEvent event);

	public String getResponse();

	public IndexableEvent getEvent();
}
