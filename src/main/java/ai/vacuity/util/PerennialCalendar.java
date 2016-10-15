package ai.vacuity.util;

import org.joda.time.Chronology;
import org.joda.time.DateTimeZone;
import org.joda.time.chrono.AssembledChronology;
import org.joda.time.chrono.GregorianChronology;

public class PerennialCalendar extends AssembledChronology {

	private static final PerennialCalendar INSTANCE_UTC;

	static {
		INSTANCE_UTC = new PerennialCalendar(GregorianChronology.getInstanceUTC(), null);
	}

	public static PerennialCalendar getInstance() {
		return INSTANCE_UTC;
	}

	protected PerennialCalendar(Chronology base, Object param) {
		super(base, param);
	}

	String moonage_separator = " ";

	@Override
	protected void assemble(Fields fields) {
		// TODO Auto-generated method stub

	}

	@Override
	public Chronology withUTC() {
		return INSTANCE_UTC;
	}

	@Override
	public Chronology withZone(DateTimeZone zone) {
		throw new UnsupportedOperationException("Method was not implemented");
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}

}
