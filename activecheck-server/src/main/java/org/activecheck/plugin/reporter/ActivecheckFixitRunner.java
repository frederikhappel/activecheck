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

	public void add(ActivecheckReporter reporter) {
		fixitQueue.add(reporter);
	}

	@Override
	public void run() {
		while (true) {
			for (ActivecheckReporter reporter : fixitQueue) {
				if (reporter.getStatus() == ActivecheckReporterStatus.REQUEUE
						|| reporter.getStatus() == ActivecheckReporterStatus.SCHEDULED) {
					reporter.fixit();
				}
			}
			try {
				TimeUnit.MILLISECONDS.sleep(2000);
			} catch (InterruptedException e) {
				logger.error(e.getMessage());
				logger.trace(e.getMessage(), e);
			}
		}
	}
}
