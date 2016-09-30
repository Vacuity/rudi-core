package ai.vacuity.rudi.adaptors.hal.hao;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.Query;

public class IndexableQuery implements Query {

	Query delegate = null;
	int id = 0;
	String label = "";
	IRI iri = null;

	public IndexableQuery(Query delegate) {
		this.delegate = delegate;
	}

	public Query getDelegate() {
		return delegate;
	}

	@Override
	public void setBinding(String name, Value value) {
		this.delegate.setBinding(name, value);
	}

	@Override
	public void removeBinding(String name) {
		this.delegate.removeBinding(name);
	}

	@Override
	public void clearBindings() {
		this.delegate.clearBindings();
	}

	@Override
	public BindingSet getBindings() {
		return this.delegate.getBindings();
	}

	@Override
	public void setDataset(Dataset dataset) {
		this.delegate.setDataset(dataset);
	}

	@Override
	public Dataset getDataset() {
		return this.delegate.getDataset();
	}

	@Override
	public void setIncludeInferred(boolean includeInferred) {
		this.setIncludeInferred(includeInferred);
	}

	@Override
	public boolean getIncludeInferred() {
		return this.delegate.getIncludeInferred();
	}

	@Override
	public void setMaxExecutionTime(int maxExecTime) {
		this.delegate.setMaxExecutionTime(maxExecTime);
	}

	@Override
	public int getMaxExecutionTime() {
		return this.delegate.getMaxExecutionTime();
	}

	@Override
	@Deprecated
	public void setMaxQueryTime(int maxQueryTime) {
		this.delegate.setMaxQueryTime(maxQueryTime);
	}

	@Override
	public int getMaxQueryTime() {
		return getMaxQueryTime();
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public IRI getIri() {
		return iri;
	}

	public void setIri(IRI iri) {
		this.iri = iri;
	}

}
