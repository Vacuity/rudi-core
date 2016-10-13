package ai.vacuity.rudi.adaptors.interfaces;

import ai.vacuity.rudi.adaptors.bo.p2p.Input;

public interface IQueryProvider {

	public enum Type {
		SPARQL, SERQL, SQL, LDPATH, VIRTUOSO_FACETS
	}

	public String getQuery(Input input, IQueryProvider.Type queryType);

}