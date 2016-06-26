package org.activecheck.plugin.collector.nsca;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import org.activecheck.common.nagios.NagiosServiceReport;
import org.activecheck.common.nagios.NagiosServiceStatus;
import org.activecheck.common.plugin.ActivecheckPluginProperties;
import org.activecheck.common.plugin.collector.ActivecheckCollector;
import org.activecheck.common.plugin.collector.ActivecheckCollectorType;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.jsendnsca.Level;
import com.googlecode.jsendnsca.MessagePayload;
import com.googlecode.jsendnsca.NagiosPassiveCheckSender;
import com.googlecode.jsendnsca.NagiosSettings;
import com.googlecode.jsendnsca.builders.MessagePayloadBuilder;
import com.googlecode.jsendnsca.builders.NagiosSettingsBuilder;
import com.googlecode.jsendnsca.encryption.Encryption;

@ActivecheckPluginProperties(propertiesToMerge = {})
public class NscaCollector extends ActivecheckCollector implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(NscaCollector.class);

	private int retryInterval = RETRY_INTERVAL_DEFAULT;
	private long nextRetryTime = 0;

	private NagiosSettings settings = null;
	private final Queue<MessagePayload> messageQueue;
	private boolean run = true;
	private Thread t = null;

	private static final HashMap<NagiosServiceStatus, Level> status2level = new HashMap<NagiosServiceStatus, Level>();
	static {
		status2level.put(NagiosServiceStatus.OK, Level.OK);
		status2level.put(NagiosServiceStatus.UNKNOWN, Level.UNKNOWN);
		status2level.put(NagiosServiceStatus.WARNING, Level.WARNING);
		status2level.put(NagiosServiceStatus.CRITICAL, Level.CRITICAL);
		status2level.put(NagiosServiceStatus.FAILURE, Level.CRITICAL);
	}

	public NscaCollector(PropertiesConfiguration properties) {
		super(properties, ActivecheckCollectorType.REPORTING);
		messageQueue = new LinkedList<MessagePayload>();

		// initialize what has not been initialized
		collectorInit();
	}

	@Override
	protected void collectorInit() {
		this.settings = new NagiosSettingsBuilder().withLargeMessageSupportEnabled().withNagiosHost(host.getFqdn()).withPort(host.getPort()).withEncryption(Encryption.XOR).create();

		// start thread if not already running
		if (t == null || !t.isAlive()) {
			// call hasChanged to prevent direct disconnect
			host.hasChanged();

			// start thread
			run = true;
			t = new Thread(this);
			t.setName("NSCAHost " + getCollectorEndpointName());
			t.start();
		}
	}

	@Override
	public void run() {
		// connect to NSCA host
		NagiosPassiveCheckSender sender = new NagiosPassiveCheckSender(settings);
		logger.info("Connected NSCA host " + getCollectorEndpointName());

		while (run && !host.hasChanged()) {
			long time = System.currentTimeMillis();
			if (time >= nextRetryTime) {
				if (nextRetryTime > 0) {
					logger.debug("Last submission error " + retryInterval + "seconds ago");
					retryInterval = retryInterval * 2 > RETRY_INTERVAL_MAX ? RETRY_INTERVAL_MAX
							: retryInterval * 2;
				}
				try {
					while (messageQueue.size() > 0) {
						sender.send(messageQueue.poll());
					}
					retryInterval = RETRY_INTERVAL_DEFAULT;
					nextRetryTime = 0;
				} catch (Exception e) {
					messageQueue.clear();
					nextRetryTime = time + retryInterval * 1000;
					logger.warn("sending to '" + getCollectorEndpointName() + "' failed. Retrying in " + retryInterval + " seconds: " + e.getMessage());
					logger.trace(e.getMessage(), e);
				}
			} else {
				logger.debug(getCollectorEndpointName() + " in error state. Retrying in " + retryInterval + " seconds");
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		// TODO: disconnect sender?
		logger.info("Disconnected NSCA host " + getCollectorEndpointName());
	}

	@Override
	public synchronized void sendImpl(NagiosServiceReport report) {
		String message = report.getMessageWithPerformancedata().replace("\n",
				"\\n");
		MessagePayload payload = new MessagePayloadBuilder().withHostname(report.getServiceHost()).withLevel(status2level.get(report.getStatus())).withMessage(message).create();
		String serviceName = report.getServiceName();
		if (serviceName != null) {
			payload.setServiceName(report.getServiceName());
		}
		messageQueue.add(payload);
		logger.trace(message);
	}

	@Override
	public void disconnect() {
		run = false;
	}
}
