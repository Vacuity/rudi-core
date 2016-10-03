package ai.vacuity.rudi.adaptors.bo;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.RepositoryConnection;

public class Tuple {
	RepositoryConnection connection = null;
	Resource subject = null;
	IRI predicate = null;
	Value object = null;
	Resource[] contexts = null;

	public RepositoryConnection getConnection() {
		return connection;
	}

	public void setConnection(RepositoryConnection connection) {
		this.connection = connection;
	}

	public Resource getSubject() {
		return subject;
	}

	public void setSubject(Resource subject) {
		this.subject = subject;
	}

	public IRI getPredicate() {
		return predicate;
	}

	public void setPredicate(IRI predicate) {
		this.predicate = predicate;
	}

	public Value getObject() {
		return object;
	}

	public void setObject(Value object) {
		this.object = object;
	}

	public Resource[] getContexts() {
		return contexts;
	}

	public void setContexts(Resource[] contexts) {
		this.contexts = contexts;
	}
}
