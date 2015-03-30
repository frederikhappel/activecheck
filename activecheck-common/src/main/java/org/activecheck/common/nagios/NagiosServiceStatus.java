package org.activecheck.common.nagios;


public enum NagiosServiceStatus {
	OK(0, 0), WARNING(1, 3), CRITICAL(2, 4), UNKNOWN(3, 1), DEPENDENT(4, 2), FAILURE(
		5, 5);

	private int statusCode;
	private int severityOrder;

	private NagiosServiceStatus(int statusCode, int severityOrder) {
		this.statusCode = statusCode;
		this.severityOrder = severityOrder;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public int getSeverityOrder() {
		return severityOrder;
	}

	public NagiosServiceStatus moreSevere(NagiosServiceStatus status) {
		return severityOrder > status.getSeverityOrder() ? this : status;
	}

	public NagiosServiceStatus lessSevere(NagiosServiceStatus status) {
		return severityOrder < status.getSeverityOrder() ? this : status;
	}

	public static NagiosServiceStatus severityOrderToStatus(int severityOrder) {
		for (NagiosServiceStatus status : NagiosServiceStatus.values()) {
			if (status.getSeverityOrder() == severityOrder) {
				return status;
			}
		}
		return null;
	}

	public static NagiosServiceStatus statusCodeToStatus(int statusCode) {
		for (NagiosServiceStatus status : NagiosServiceStatus.values()) {
			if (status.getStatusCode() == statusCode) {
				return status;
			}
		}
		return null;
	}
}
