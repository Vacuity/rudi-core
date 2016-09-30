package ai.vacuity.rudi.adaptors.interfaces;

/**
 * Swaps placeholders in <via:Output/> elements, e.g. call (json, rdf, etc.) and/or translator values.
 * 
 * @author In Lak'ech.
 *
 */
public interface ITemplateProcessor {
	public void process(String template, String input);

	public String getTemplate();

	public String getInput();
}
