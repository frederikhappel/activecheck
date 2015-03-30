package org.activecheck.common.nagios;

import java.io.Serializable;

import org.apache.commons.configuration.PropertiesConfiguration;

public final class NagiosServiceReportRouting implements Serializable {
	private static final long serialVersionUID = 1746530249945187664L;
	private boolean reportResults = true; // send check results to nagios
	private boolean graphResults = false; // graph nagios check status
	private boolean graphPerfdata = true; // graph performance data

	public NagiosServiceReportRouting() {
		super();
	}

	public NagiosServiceReportRouting(PropertiesConfiguration properties) {
		super();
		setFromProperties(properties);
	}

	public void setFromProperties(PropertiesConfiguration properties) {
		reportResults = properties.getBoolean("report_results", reportResults);
		graphResults = properties.getBoolean("graph_results", graphResults);
		graphPerfdata = properties.getBoolean("graph_perfdata", graphPerfdata);
	}

	public boolean doReportResults() {
		return reportResults;
	}

	public void setReportResults(boolean reportResults) {
		this.reportResults = reportResults;
	}

	public boolean doGraphResults() {
		return graphResults;
	}

	public void setGraphResults(boolean graphResults) {
		this.graphResults = graphResults;
	}

	public boolean doGraphPerfdata() {
		return graphPerfdata;
	}

	public void setGraphPerfdata(boolean graphPerfdata) {
		this.graphPerfdata = graphPerfdata;
	}
}
