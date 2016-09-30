package ai.vacuity.rudi.adaptors.interfaces;

/**
 * Swaps placeholders in <via:Output/> elements, e.g. call (json, rdf, etc.), and translator values.
 * 
 * @author In Lak'ech.
 *
 */
public interface IResponseProcessor {
	public void process(String response, String input);

	public String getResponse();

	public String getInput();
}
