package org.activecheck.plugin.collector.graphite;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;

public class NagiosMetricRegistry implements Runnable {
	private static final Logger logger = LoggerFactory
			.getLogger(NagiosMetricRegistry.class);

	private final MetricRegistry registry = new MetricRegistry();
	private final Map<String, NagiosGauge<?>> gauges = new ConcurrentHashMap<String, NagiosGauge<?>>();
	private boolean run = true;

	public MetricRegistry getRegistry() {
		return registry;
	}

	@SuppressWarnings("unchecked")
	public <T> void addGauge(T value, String... names) {
		String graphitePath = GraphiteUtils.makeGraphitePath(names);
		graphitePath = GraphiteUtils.finalizeGraphitePath(graphitePath);
		logger.debug("Sending status metrics for " + graphitePath);
		NagiosGauge<T> perfDataGauge = (NagiosGauge<T>) registry.getGauges()
				.get(graphitePath);
		if (perfDataGauge == null) {
			perfDataGauge = new NagiosGauge<T>(value);
			gauges.put(graphitePath, perfDataGauge);
			registry.register(graphitePath, perfDataGauge);
		} else {
			perfDataGauge.setValue(value);
		}
	}

	public void stop() {
		run = false;
	}

	@Override
	public void run() {
		while (run) {
			for (Entry<String, NagiosGauge<?>> entry : gauges.entrySet()) {
				if (entry.getValue().isDead()) {
					if (registry.remove(entry.getKey())) {
						gauges.remove(entry.getKey());
					}
				}
			}
			try {
				TimeUnit.SECONDS.sleep(60);
			} catch (InterruptedException e) {
				logger.error(e.getMessage());
				logger.trace(e.getMessage(), e);
			}
		}
		registry.removeMatching(MetricFilter.ALL);
		gauges.clear();
	}
}
