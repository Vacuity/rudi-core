package ai.vacuity.rudi.adaptors.bo;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;

import ai.vacuity.rudi.adaptors.interfaces.IndexableEvent;

public class Query implements org.eclipse.rdf4j.query.Query, IndexableEvent {

	org.eclipse.rdf4j.query.Query delegate = null;
	int id = 0;
	String label = "";
	IRI iri = null;
	IRI notifies = null;
	boolean hasIsEmpty = false;

	public Query(org.eclipse.rdf4j.query.Query delegate) {
		this.delegate = delegate;
	}

	public org.eclipse.rdf4j.query.Query getDelegate() {
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see ai.vacuity.rudi.adaptors.bo.Indexable#getLabel()
	 */
	@Override
	public String getLabel() {
		return label;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ai.vacuity.rudi.adaptors.bo.Indexable#setLabel(java.lang.String)
	 */
	@Override
	public void setLabel(String label) {
		this.label = label;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ai.vacuity.rudi.adaptors.bo.Indexable#getIri()
	 */
	@Override
	public IRI getIri() {
		return iri;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ai.vacuity.rudi.adaptors.bo.Indexable#setIri(org.eclipse.rdf4j.model.IRI)
	 */
	@Override
	public void setIri(IRI iri) {
		this.iri = iri;
	}

	@Override
	public IRI getOwnerIri() {
		return this.notifies;
	}

	@Override
	public void setOwnerIri(IRI notifies) {
		this.notifies = notifies;
	}

	public boolean hasIsEmpty() {
		return hasIsEmpty;
	}

	public void setHasIsEmpty(boolean negate) {
		this.hasIsEmpty = negate;
	}

}
