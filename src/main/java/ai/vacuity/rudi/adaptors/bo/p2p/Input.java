package ai.vacuity.rudi.adaptors.bo.p2p;

import ai.vacuity.rudi.adaptors.interfaces.RoutableEvent;

public class Input implements RoutableEvent {
	private static final long serialVersionUID = 4715947902139136083L;

	private String label;

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

}
