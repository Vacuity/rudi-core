package ai.vacuity.rudi.adaptors.interfaces;

import java.io.Serializable;

public interface RoutableEvent extends Serializable {
	public String getLabel();
}
