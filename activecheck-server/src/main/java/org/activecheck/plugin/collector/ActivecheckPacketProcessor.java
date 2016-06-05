package org.activecheck.plugin.collector;

import org.activecheck.common.nagios.NagiosServiceReport;
import org.activecheck.common.plugin.collector.ActivecheckCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ActivecheckPacketProcessor {
    private static final Logger logger = LoggerFactory.getLogger(ActivecheckPacketProcessor.class);
    private final Map<String, ActivecheckCollector> activecheckCollectorHosts;
    private final Set<String> configuredCollectorHosts;
    private final Set<String> cleanupCollectorHosts;

    public ActivecheckPacketProcessor() {
        activecheckCollectorHosts = new ConcurrentHashMap<>();
        configuredCollectorHosts = new HashSet<>();
        cleanupCollectorHosts = new HashSet<>();
    }

    public void addOrUpdateCollector(ActivecheckCollector activecheckCollector) {
        final String collectorName = activecheckCollector.getPluginName();
        configuredCollectorHosts.add(collectorName);
        cleanupCollectorHosts.remove(collectorName);
        if (!activecheckCollectorHosts.containsKey(collectorName)) {
            logger.debug("Adding Generic host {}", collectorName);
            activecheckCollectorHosts.put(collectorName, activecheckCollector);
        }
    }

    public void process(NagiosServiceReport report) {
        for (final ActivecheckCollector activecheckCollectorHost : activecheckCollectorHosts.values()) {
            logger.debug("Processing packet for host {}", activecheckCollectorHost.getCollectorEndpointName());
            activecheckCollectorHost.send(report);
        }
    }

    public void removeNonexistentHosts() {
        if (cleanupCollectorHosts.size() > 0) {
            for (final String collectorHost : cleanupCollectorHosts) {
                logger.debug("Trying to remove collector host {}", collectorHost);
                if (activecheckCollectorHosts.containsKey(collectorHost)) {
                    try {
                        activecheckCollectorHosts.get(collectorHost).disconnect();
                        logger.info("Successfully removed collector host {}", collectorHost);
                    } catch (Exception e) {
                        logger.error("Failed removing collector host {}", collectorHost, e);
                    }
                    activecheckCollectorHosts.remove(collectorHost);
                } else {
                    logger.debug("Not removing non-existent collector host {}", collectorHost);
                }
            }
            cleanupCollectorHosts.clear();
        }
        cleanupCollectorHosts.addAll(configuredCollectorHosts);
        configuredCollectorHosts.clear();
    }
}
