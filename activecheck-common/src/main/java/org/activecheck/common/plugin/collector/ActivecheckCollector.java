package org.activecheck.common.plugin.collector;

import org.activecheck.common.nagios.NagiosServiceReport;
import org.activecheck.common.plugin.ActivecheckPlugin;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class ActivecheckCollector extends ActivecheckPlugin {
	private static final Logger logger = LoggerFactory.getLogger(ActivecheckCollector.class);

	public static final int RETRY_INTERVAL_MAX = 60;
	public static final int RETRY_INTERVAL_DEFAULT = 1;

	protected final GenericCollector host = new GenericCollector();
	private final ActivecheckCollectorType type;

	public ActivecheckCollector(PropertiesConfiguration properties,
			ActivecheckCollectorType type) {
		super(properties);
		this.type = type;

		// initialize what has not been initialized
		pluginInit();
	}

	private final void pluginInit() {
		// change host
		host.setFqdn(properties.getString("host", null));
		host.setPort(properties.getInteger("port", 0));
	}

	@Override
	public String getPluginName() {
		return type + "_" + this.getClass().getSimpleName().toUpperCase() + "_" + this.hashCode();
	}

	public String getCollectorEndpointName() {
		return host.getUrl();
	};

	public final void send(NagiosServiceReport report) {
		String serviceName = report.getServiceName();

		switch (type) {
		case GRAPHING:
			boolean graphPerfData = report.getRouting().doGraphPerfdata();
			boolean graphNagiosStatus = report.getRouting().doGraphResults();
			if (!graphNagiosStatus && !graphPerfData) {
				logger.info("nothing should be graphed");
			} else if (!report.hasChanged()) {
				logger.debug("Not sending metrics for the unchanged service '" + serviceName + "'");
			} else if (serviceName == null || serviceName.isEmpty()) {
				logger.debug("Not sending metrics for an unnamed service");
			} else {
				sendImpl(report);
			}
			break;

		case REPORTING:
			boolean reportResults = report.getRouting().doReportResults();
			if (!reportResults) {
				logger.info("check results should not be submitted for service '" + serviceName + "'");
			} else {
				sendImpl(report);
			}
			break;

		default:
			logger.error("This is impossible");
			break;
		}
	}

	@Override
	protected final void pluginReload() {
		pluginInit();
		collectorInit();
	}

	abstract protected void collectorInit();

	abstract public void sendImpl(NagiosServiceReport report);

	abstract public void disconnect() throws Exception;
}
