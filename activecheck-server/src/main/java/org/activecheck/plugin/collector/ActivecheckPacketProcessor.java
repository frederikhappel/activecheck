package org.activecheck.plugin.collector;

import org.activecheck.MBeanRegistry;
import org.activecheck.common.nagios.NagiosServiceReport;
import org.activecheck.common.plugin.collector.ActivecheckCollector;
import org.activecheck.common.plugin.reporter.ActivecheckReporter;
import org.activecheck.net.ActivecheckServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO: This maybe should use a thread pool instead of being run from the main thread
 */
public class ActivecheckPacketProcessor implements Observer {
    private static final Logger logger = LoggerFactory.getLogger(ActivecheckPacketProcessor.class);
    private static final String JMX_OBJECT_TYPE = "Collectors";

    private final Map<String, ActivecheckCollector> activecheckCollectorHosts = new ConcurrentHashMap<>();
    private final String localFqdn;

    public ActivecheckPacketProcessor(@Nonnull String localFqdn) {
        this.localFqdn = localFqdn;
    }

    public void addOrUpdateCollector(ActivecheckCollector collector) {
        final String collectorName = collector.getPluginName();

        if (isActive(collector)) {
            if (!activecheckCollectorHosts.containsKey(collectorName)) {
                logger.debug("Adding collector {}", collectorName);

                // add collector to hash map
                activecheckCollectorHosts.put(collectorName, collector);

                // add to jmx
                MBeanRegistry.getInstance().register(JMX_OBJECT_TYPE, collectorName, collector);
            }
        }
    }

    public void process(NagiosServiceReport report) {
        for (final ActivecheckCollector collector : activecheckCollectorHosts.values()) {
            if (isActive(collector)) {
                logger.debug("Processing packet for collector {}", collector.getCollectorEndpointName());
                collector.send(report);
            }
        }
    }

    private boolean isActive(ActivecheckCollector collector) {
        final boolean active = collector.isEnabled();
        if (!active) {
            final String collectorName = collector.getPluginName();

            logger.debug("Removing collector {}", collectorName);
            try {
                // run collector cleanup
                collector.disconnect();
            } catch (Exception e) {
                logger.error("Failed disconnecting collector {}", collectorName, e);
            }

            // remove all references
            activecheckCollectorHosts.remove(collectorName);
            MBeanRegistry.getInstance().unregister(JMX_OBJECT_TYPE, collector.getPluginName());
        }
        return active;
    }

    @Override
    public void update(Observable arg0, Object arg1) {
        logger.debug("Received update from {}", arg0.getClass());
        if (arg0 instanceof ActivecheckReporter) {
            final ActivecheckReporter nagiosReporter = (ActivecheckReporter) arg0;
            final long startTime = nagiosReporter.getLastRunTimeMillis();
            final long finishTime = startTime + nagiosReporter.getExecutionTimeMillis();

            for (NagiosServiceReport report : nagiosReporter.getReports()) {
                // send report to configured hosts
                report.setServiceHost(localFqdn);
                report.setStartTime(startTime);
                report.setFinishTime(finishTime);
                process(report);
            }
        } else if (arg0 instanceof ActivecheckServer) {
            // send received packet to configured hosts
            process((NagiosServiceReport) arg1);
        }
    }
}
