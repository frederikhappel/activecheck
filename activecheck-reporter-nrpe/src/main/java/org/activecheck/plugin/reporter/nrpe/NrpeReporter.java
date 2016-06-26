package org.activecheck.plugin.reporter.nrpe;

import java.io.IOException;

import org.activecheck.common.nagios.NagiosServiceStatus;
import org.activecheck.common.plugin.ActivecheckPluginProperties;
import org.activecheck.common.plugin.reporter.ActivecheckReporter;
import org.activecheck.common.plugin.reporter.ActivecheckReporterException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// reporter properties

@ActivecheckPluginProperties(propertiesToMerge = { "nrpe_port", "nrpe_host",
		"nrpe_ssl", "nrpe_timeout" })
public class NrpeReporter extends ActivecheckReporter {
	private static final Logger logger = LoggerFactory
			.getLogger(NrpeReporter.class);

	private static final String DEFAULT_NRPE_HOST = "localhost";
	private static final int DEFAULT_NRPE_PORT = 5666;
	private static final boolean DEFAULT_NRPE_SSL = true;
	private static final int DEFAULT_NRPE_TIMEOUT = 10;
	private static final int RETRY_DELAY = 1000;

	private String host = DEFAULT_NRPE_HOST;
	private int port = DEFAULT_NRPE_PORT;
	private boolean useSsl = DEFAULT_NRPE_SSL;
	private int timeout = DEFAULT_NRPE_TIMEOUT;
	private NrpeCommand nrpeCommand = null;

	public NrpeReporter(PropertiesConfiguration newProperties) {
		super(newProperties);

		// initialize what has not been initialized
		reporterInit();
	}

	@Override
	protected void reporterInit() {
		// reload configuration and recreate commandInvoker
		host = properties.getString("nrpe_host", DEFAULT_NRPE_HOST);
		port = properties.getInt("nrpe_port", DEFAULT_NRPE_PORT);
		useSsl = properties.getBoolean("nrpe_ssl", DEFAULT_NRPE_SSL);
		timeout = properties.getInt("nrpe_timeout", DEFAULT_NRPE_TIMEOUT);

		// redefine values
		String command = properties.getString("command", null);
		String arguments = properties.getString("arguments", null);
		nrpeCommand = new NrpeCommand(command, arguments);
	}

	@Override
	public void runCommand() throws ActivecheckReporterException {
		if (nrpeCommand == null) {
			String errorMessage = "no command has been defined for service '"
					+ getOverallServiceName() + "'";
			logger.error(errorMessage);
			setOverallServiceReport(NagiosServiceStatus.WARNING, errorMessage);
		} else {
			// execute query
			logger.debug("Service '" + getOverallServiceName()
					+ "': Running NRPE command '" + nrpeCommand.getQuery()
					+ "'");

			try {
				try {
					// run nrpe command
					NrpeCommandInvoker.execute(host, port, useSsl, timeout,
							nrpeCommand);
					Validate.notEmpty(nrpeCommand.getCheckResult().getMessage());
				} catch (IOException | NullPointerException
						| IllegalArgumentException e) {
					logger.info("Rerunning NRPE command in " + RETRY_DELAY
							+ "ms. Error message: '" + e.getMessage() + "'");
					Thread.sleep(RETRY_DELAY);
					NrpeCommandInvoker.execute(host, port, useSsl, timeout,
							nrpeCommand);
				}
				setOverallServiceReport(nrpeCommand.getCheckResult());
				logger.debug("Service '"
						+ getOverallServiceName()
						+ "': '"
						+ nrpeCommand.getCheckResult()
								.getMessageWithPerformancedata() + "'");
			} catch (Exception e) {
				logger.error("Error running NRPE command: '"
						+ nrpeCommand.getQuery() + "': " + e.getMessage());
				logger.trace(e.getMessage(), e);
				setOverallServiceReport(NagiosServiceStatus.CRITICAL,
						e.getMessage());
			}
		}
	}

	@Override
	protected void cleanUp() {
		return;
	}
}
