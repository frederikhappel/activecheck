package org.activecheck.common.nagios;

public class NagiosPerformanceDataException extends Exception {
	private static final long serialVersionUID = 8431559230325732223L;

	public NagiosPerformanceDataException(String message) {
		super(message);
	}

	public NagiosPerformanceDataException(Throwable cause) {
		super(cause);
	}

	public NagiosPerformanceDataException(String message, Throwable cause) {
		super(message, cause);
	}
}
