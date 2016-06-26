/**
 * 
 */
package org.activecheck.plugin.reporter.graylog;

import java.io.IOException;
import java.util.Collection;

import org.activecheck.common.nagios.NagiosServiceReport;
import org.activecheck.common.nagios.NagiosServiceStatus;
import org.activecheck.common.plugin.ActivecheckPluginProperties;
import org.activecheck.common.plugin.reporter.ActivecheckReporter;
import org.activecheck.common.plugin.reporter.ActivecheckReporterException;
import org.activecheck.plugin.reporter.graylog.api.GraylogApiStreamObject;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.Validate;

/**
 * @author fh
 * 
 */
@ActivecheckPluginProperties(propertiesToMerge = {})
public class GraylogReporter extends ActivecheckReporter {
	private static final int MAX_CONCURRENT_REQUESTS = 10;

	private GraylogAlertCollector alertCollector = null;
	private String url = null;
	private int since = 15;
	private int warning = 1;
	private int critical = since;

	public GraylogReporter(PropertiesConfiguration properties) {
		super(properties);
		alertCollector = new GraylogAlertCollector(MAX_CONCURRENT_REQUESTS);

		// initialize what has not been initialized
		reporterInit();
	}

	@Override
	protected void reporterInit() {
		// get parameters
		url = properties.getString("url", null);
		since = properties.getInt("since", since);
		warning = properties.getInt("warning", warning);
		critical = properties.getInt("critical", since > warning ? since
				: warning);
		Validate.notNull(url);

		// get authentication parameters
		String username = properties.getString("graylog2.username", null);
		String password = properties.getString("graylog2.password", "");
		alertCollector.setCredentials(username, password);
	}

	@Override
	public void runCommand() throws ActivecheckReporterException {
		long timestamp = (System.currentTimeMillis() - since * 60000l) / 1000;
		String overallMessage = "";
		long streamsWarning = 0;
		long streamsCritical = 0;
		long streamsDisabled = 0;
		try {
			Collection<GraylogApiStreamObject> streams = alertCollector
					.getStreams(url, timestamp);
			NagiosServiceStatus overallStatus = NagiosServiceStatus.OK;
			for (GraylogApiStreamObject stream : streams) {
				long numAlerts = stream.getNumAlerts();
				String message = "";

				// determine status for current stream
				NagiosServiceStatus status = NagiosServiceStatus.OK;
				if (!stream.isDisabled()) {
					if (numAlerts >= critical) {
						status = NagiosServiceStatus.CRITICAL;
						streamsCritical++;
					} else if (numAlerts >= warning) {
						status = NagiosServiceStatus.WARNING;
						streamsWarning++;
					}
					message = String.format("%d alerts in the last %d minutes",
							numAlerts, since);
				} else {
					status = NagiosServiceStatus.WARNING;
					streamsDisabled++;
					message = "DISABLED or PAUSED";
				}
				if (status != NagiosServiceStatus.OK) {
					overallStatus = overallStatus.moreSevere(status);
					overallMessage += String.format("%n%s - %s: %s", status,
							stream.getTitle(), message);
				}

				// create report
				NagiosServiceReport report = new NagiosServiceReport(
						getOverallServiceName() + "_" + stream.getTitle(),
						getOverallServiceHost(), status);
				report.setMessage(message);
				addServiceReport(report);
			}

			// ran through without errors
			NagiosServiceReport report = new NagiosServiceReport(
					getOverallServiceName(), getOverallServiceHost(),
					overallStatus);
			report.setMessage(String
					.format("queried %d streams with states CRITICAL: %d, WARNING: %d, DISABLED/PAUSED: %d%s",
							streams.size(), streamsCritical, streamsWarning,
							streamsDisabled, overallMessage));
			setOverallServiceReport(report);
		} catch (IOException | InterruptedException | RuntimeException e) {
			// something went wrong. set overall status to CRITICAL
			setOverallServiceReport(NagiosServiceStatus.CRITICAL,
					e.getMessage());
			throw new ActivecheckReporterException(e);
		}
	}

	@Override
	protected void cleanUp() {
		alertCollector.cleanUp();
		return;
	}
}
