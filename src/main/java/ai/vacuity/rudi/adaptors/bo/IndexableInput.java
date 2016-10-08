package ai.vacuity.rudi.adaptors.bo;

import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.rdf4j.model.IRI;

import ai.vacuity.rudi.adaptors.interfaces.IEvent;

public class IndexableInput implements IEvent {

	private IRI ownerIri = null;
	private IRI iri = null;
	private String input = null;
	private IRI[] called = new IRI[] {};
	private SortedSet<Match> matches = new TreeSet<Match>();

	public IndexableInput(IRI userIri, IRI iri, String input) {
		this.input = input;
		this.iri = iri;
		this.ownerIri = userIri;
	}

	@Override
	public String getLabel() {
		return this.input;
	}

	@Override
	public void setLabel(String input) {
		this.input = input;
	}

	@Override
	public IRI getIri() {
		return this.iri;
	}

	@Override
	public void setIri(IRI iri) {
		this.iri = iri;
	}

	public IRI getOwnerIri() {
		return ownerIri;
	}

	public void setOwnerIri(IRI userIri) {
		this.ownerIri = userIri;
	}

	public IRI[] getCalled() {
		return called;
	}

	public void setCalled(IRI[] called) {
		this.called = called;
	}

	public SortedSet<Match> getMatches() {
		return matches;
	}

	public void setMatches(SortedSet<Match> matches) {
		this.matches = matches;
	}

	public void addMatch(Match m) {
		getMatches().add(m);
	}

}
