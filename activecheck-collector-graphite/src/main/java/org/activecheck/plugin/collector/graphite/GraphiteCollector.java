package org.activecheck.plugin.collector.graphite;

import java.util.concurrent.TimeUnit;

import org.activecheck.common.nagios.NagiosPerformanceData;
import org.activecheck.common.nagios.NagiosServiceReport;
import org.activecheck.common.plugin.ActivecheckPluginProperties;
import org.activecheck.common.plugin.collector.ActivecheckCollector;
import org.activecheck.common.plugin.collector.ActivecheckCollectorType;
import org.activecheck.common.plugin.collector.GenericCollector;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.Validate;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;

@ActivecheckPluginProperties(propertiesToMerge = {})
public class GraphiteCollector extends ActivecheckCollector {
	private static final String DEFAULT_GRAPHITE_PREFIX = "activecheck";
	private static final int DEFAULT_GRAPHITE_INTERVAL = 10;
	private static final String STATUS_METRIC_NAME = "nagios_status";

	private Graphite graphite = null;
	private final NagiosMetricRegistry registry = new NagiosMetricRegistry();
	private final Thread housekeeperThread;
	private GraphiteReporter reporter = null;

	public GraphiteCollector(PropertiesConfiguration properties) {
		super(properties, ActivecheckCollectorType.GRAPHING);
		// initialize what has not been initialized
		collectorInit();

		// start housekeeper
		housekeeperThread = new Thread(registry);
		housekeeperThread.setName("MetricsRegistryHousekeeper");
		housekeeperThread.start();
	}

	@Override
	protected void collectorInit() {
		String prefix = properties.getString("graphite_prefix",
				DEFAULT_GRAPHITE_PREFIX);
		int interval = properties.getInteger("graphite_interval",
				DEFAULT_GRAPHITE_INTERVAL);
		Validate.notNull(prefix);

		// TODO: renew all if changed
		if (graphite == null || reporter == null) {
			graphite = new Graphite(host.getSocketAddress());
			reporter = GraphiteReporter.forRegistry(registry.getRegistry())
					.prefixedWith(prefix).convertRatesTo(TimeUnit.SECONDS)
					.convertDurationsTo(TimeUnit.MILLISECONDS)
					.filter(MetricFilter.ALL).build(graphite);
			reporter.start(interval, TimeUnit.SECONDS);
		}
	}

	@Override
	public void sendImpl(NagiosServiceReport report) {
		// create and clean up metric path
		GenericCollector host = new GenericCollector(report.getServiceHost());

		final String graphitePathBase = GraphiteUtils.makeGraphitePath(
				host.getDomain(), host.getHostname(),
				GraphiteUtils.sanitizeServiceName(report.getServiceName()));

		// actually send nagios status (as a gauge)
		if (report.getRouting().doGraphResults()) {
			registry.addGauge(report.getStatus().getStatusCode(),
					graphitePathBase, STATUS_METRIC_NAME);
		}

		// send performance data
		if (report.getRouting().doGraphPerfdata()) {
			for (NagiosPerformanceData perfData : report.getPerfData()) {
				// submit current value if available
				registry.addGauge(perfData.getCurrent(), graphitePathBase,
						perfData.getName(), "current");

				// submit warning if available
				if (perfData.hasWarning()) {
					registry.addGauge(perfData.getWarning(), graphitePathBase,
							perfData.getName(), "warning");
				}

				// submit critical if available
				if (perfData.hasCritical()) {
					registry.addGauge(perfData.getCritical(), graphitePathBase,
							perfData.getName(), "critical");
				}

				// submit minimum if available
				if (perfData.hasMinimum()) {
					registry.addGauge(perfData.getMinimum(), graphitePathBase,
							perfData.getName(), "minimum");
				}

				// submit maximum if available
				if (perfData.hasMaximum()) {
					registry.addGauge(perfData.getMaximum(), graphitePathBase,
							perfData.getName(), "maximum");
				}
			}
		}
	}

	@Override
	public void disconnect() throws Exception {
		registry.stop();
		graphite.close();
	}
}
