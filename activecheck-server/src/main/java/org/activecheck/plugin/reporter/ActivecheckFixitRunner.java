package org.activecheck.plugin.reporter;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import org.activecheck.common.plugin.reporter.ActivecheckReporter;
import org.activecheck.common.plugin.reporter.ActivecheckReporterStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActivecheckFixitRunner implements Runnable {
	private static final Logger logger = LoggerFactory
			.getLogger(ActivecheckFixitRunner.class);

	private Queue<ActivecheckReporter> fixitQueue = new ConcurrentLinkedQueue<ActivecheckReporter>();
	private final int sleepMillis = 2000;

	public void add(ActivecheckReporter reporter) {
		fixitQueue.add(reporter);
	}

	@Override
	public void run() {
		while (true) {
			ActivecheckReporter reporter = fixitQueue.poll();
			while (reporter != null) {
				if (reporter.getStatus() == ActivecheckReporterStatus.REQUEUE
						|| reporter.getStatus() == ActivecheckReporterStatus.SCHEDULED) {
					logger.info("FIXIT Running script for"
							+ reporter.getOverallServiceName());
					reporter.fixit();
					logger.info("FIXIT Finished script for"
							+ reporter.getOverallServiceName());
				}
			}
			try {
				logger.debug("FIXIT no scripts to run. Sleeping " + sleepMillis
						+ " millis");
				TimeUnit.MILLISECONDS.sleep(sleepMillis);
			} catch (InterruptedException e) {
				logger.error(e.getMessage());
				logger.trace(e.getMessage(), e);
			}
		}
	}
}
