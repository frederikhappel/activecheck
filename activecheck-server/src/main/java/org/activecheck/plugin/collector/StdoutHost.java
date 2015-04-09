package org.activecheck.plugin.collector;

import org.activecheck.common.nagios.NagiosServiceReport;
import org.activecheck.common.nagios.NagiosServiceStatus;
import org.activecheck.common.plugin.collector.ActivecheckCollector;
import org.activecheck.common.plugin.collector.ActivecheckCollectorType;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StdoutHost extends ActivecheckCollector {
	private static final Logger logger = LoggerFactory
			.getLogger(StdoutHost.class);

	private static PropertiesConfiguration defaultProperties;
	static {
		defaultProperties = new PropertiesConfiguration();
		defaultProperties.addProperty("host", "localhost");
	}

	// dummy constructor
	public StdoutHost() {
		super(defaultProperties, ActivecheckCollectorType.REPORTING, "STDOUT");
	}

	@Override
	public synchronized void sendImpl(NagiosServiceReport report) {
		NagiosServiceStatus status = report.getStatus();
		String serviceName = report.getServiceName();
		String message = report.getMessageWithPerformancedata();
		if (serviceName == null) {
			System.out.println(message);
		} else {
			System.out.println(status + " - " + serviceName + "\n" + message);
		}
	}

	@Override
	public String getCollectorEndpointName() {
		return "STDOUT";
	}

	@Override
	public void disconnect() throws Exception {
		logger.info("Nothing to disconnect");
	}

	@Override
	protected void collectorInit() {
		// nothing to be done here
	}
}
