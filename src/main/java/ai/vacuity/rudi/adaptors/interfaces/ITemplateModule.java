package ai.vacuity.rudi.adaptors.interfaces;

/**
 * Swaps placeholders in <via:Output/> elements, e.g. call (json, rdf, etc.) and/or translator values.
 * 
 * @author In Lak'ech.
 *
 */
public interface ITemplateModule {
	public void process(String template, IndexableEvent input);

	public String getTemplate();

	public IndexableEvent getEvent();
}
