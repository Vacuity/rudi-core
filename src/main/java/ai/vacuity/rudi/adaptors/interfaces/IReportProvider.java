package ai.vacuity.rudi.adaptors.interfaces;

import ai.vacuity.rudi.adaptors.types.Report;

public interface IReportProvider {

	public Report getReport(IndexableEvent event);

}