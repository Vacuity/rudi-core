package ai.vacuity.rudi.adaptors.bo.p2p;

import java.io.Serializable;

public class Topic implements Serializable {

	private static final long serialVersionUID = 5371802327125799514L;

	private rice.p2p.scribe.Topic delegate;

	public rice.p2p.scribe.Topic getDelegate() {
		return delegate;
	}

	public void setDelegate(rice.p2p.scribe.Topic delegate) {
		this.delegate = delegate;
	}

}
