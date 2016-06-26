package org.activecheck.plugin.reporter.jmx.query;

public class JMXQueryExecutorException extends Exception {
	private static final long serialVersionUID = 4751505629736300341L;

	public JMXQueryExecutorException(String message) {
		super(message);
	}

	public JMXQueryExecutorException(Throwable cause) {
		super(cause.getMessage(), cause);
	}

	public JMXQueryExecutorException(String message, Throwable cause) {
		super(message, cause);
	}
}
