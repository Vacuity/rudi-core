package ai.vacuity.rudi.adaptors.utils;

public class Merger {

	
	public void merge(){
		// connect to the quad store
		// look up all triples for subject S whose rdfs:sameAs SA is of type via:Merge
		// gather the properties of the SA into a map M from propVal -> propURI
		// let V be all property values of S where the property is in the value space of M
		// pass S, V, and M to fillSPARQL
		// fillSPARQL
		// for every instance of tag ${x} in the via:query property of SA, substitute the tag
		// for the value in V which is keyed to the tag in M
		// execute the query, update the graph by swapping all instances of S with the result of the SPARQL
	}
	
}
