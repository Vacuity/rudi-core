package ai.vacuity.rudi.adaptors.interfaces;

/**
 * Swaps placeholders in <via:Output/> elements, e.g. call (json, rdf, etc.), and translator values.
 * 
 * @author In Lak'ech.
 *
 */
public interface TemplateProcessor {
	public void process(String template, String target);

	public String getTemplate();

	public String getTarget();
}
