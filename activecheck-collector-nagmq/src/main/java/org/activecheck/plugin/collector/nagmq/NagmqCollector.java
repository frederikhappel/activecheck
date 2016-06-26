package org.activecheck.plugin.collector.nagmq;

import java.util.LinkedList;
import java.util.Queue;

import org.activecheck.common.nagios.NagiosServiceReport;
import org.activecheck.common.plugin.ActivecheckPluginProperties;
import org.activecheck.common.plugin.collector.ActivecheckCollector;
import org.activecheck.common.plugin.collector.ActivecheckCollectorType;
import org.activecheck.plugin.collector.nagmq.common.NagmqStatusCheck;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@ActivecheckPluginProperties(propertiesToMerge = {})
public class NagmqCollector extends ActivecheckCollector implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(NagmqCollector.class);

	private static final int DEFAULT_ZMQ_HWM = 1000;

	private static final ZMQ.Context ctx = ZMQ.context(1);
	private int retryInterval = RETRY_INTERVAL_DEFAULT;
	private long nextRetryTime = 0;

	private int hwm;
	private final Gson gson;
	private final Queue<String> messageQueue;
	private boolean run = true;
	private Thread t = null;

	public NagmqCollector(PropertiesConfiguration properties) {
		super(properties, ActivecheckCollectorType.REPORTING);

		gson = new GsonBuilder().create();
		messageQueue = new LinkedList<String>();

		// initialize what has not been initialized
		collectorInit();
	}

	@Override
	protected void collectorInit() {
		this.hwm = properties.getInt("hwm", DEFAULT_ZMQ_HWM);

		// start thread if not already running
		if (t == null || !t.isAlive()) {
			// call hasChanged to prevent direct disconnect
			host.hasChanged();

			// start thread
			run = true;
			t = new Thread(this);
			t.setName("NagmqHost " + getCollectorEndpointName());
			t.start();
		}
	}

	@Override
	public void run() {
		// zeromq connection
		ZMQ.Socket client = ctx.socket(ZMQ.PUSH);
		client.setLinger(0);
		client.connect(host.getUrl());
		logger.info("Connected Nagmq host " + getCollectorEndpointName());

		while (run && !host.hasChanged()) {
			client.setSndHWM(hwm);
			long time = System.currentTimeMillis();
			if (time >= nextRetryTime) {
				if (nextRetryTime > 0) {
					logger.debug("Last submission error " + retryInterval + "seconds ago");
					retryInterval = retryInterval * 2 > RETRY_INTERVAL_MAX ? RETRY_INTERVAL_MAX
							: retryInterval * 2;
				}
				try {
					while (messageQueue.size() > 0) {
						client.send(messageQueue.poll(), 0);
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
		client.close();
		logger.info("Disconnected Nagmq host " + getCollectorEndpointName());
	}

	@Override
	public synchronized void sendImpl(NagiosServiceReport report) {
		NagmqStatusCheck nagmqStatusCheck = new NagmqStatusCheck(
				report.getServiceHost(), report.getServiceName(),
				report.getMessageWithPerformancedata(),
				report.getStatus().getStatusCode(), report.getStartTime(),
				report.getFinishTime());
		String payload = gson.toJson(nagmqStatusCheck);
		messageQueue.add(payload);
		logger.trace(payload);
	}

	@Override
	public void disconnect() {
		run = false;
	}
}
