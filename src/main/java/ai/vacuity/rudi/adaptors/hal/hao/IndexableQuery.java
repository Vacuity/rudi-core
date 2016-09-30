package ai.vacuity.rudi.adaptors.hal.hao;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.Query;

public class IndexableQuery implements Query {

	Query q = null;
	int id = 0;
	String label = "";

	public IndexableQuery(Query q) {
		this.q = q;
	}

	public Query getQuery() {
		return q;
	}

	@Override
	public void setBinding(String name, Value value) {
		this.q.setBinding(name, value);
	}

	@Override
	public void removeBinding(String name) {
		this.q.removeBinding(name);
	}

	@Override
	public void clearBindings() {
		this.q.clearBindings();
	}

	@Override
	public BindingSet getBindings() {
		return this.q.getBindings();
	}

	@Override
	public void setDataset(Dataset dataset) {
		this.q.setDataset(dataset);
	}

	@Override
	public Dataset getDataset() {
		return this.q.getDataset();
	}

	@Override
	public void setIncludeInferred(boolean includeInferred) {
		this.setIncludeInferred(includeInferred);
	}

	@Override
	public boolean getIncludeInferred() {
		return this.q.getIncludeInferred();
	}

	@Override
	public void setMaxExecutionTime(int maxExecTime) {
		this.q.setMaxExecutionTime(maxExecTime);
	}

	@Override
	public int getMaxExecutionTime() {
		return this.q.getMaxExecutionTime();
	}

	@Override
	@Deprecated
	public void setMaxQueryTime(int maxQueryTime) {
		this.q.setMaxQueryTime(maxQueryTime);
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

}
