package org.activecheck.plugin.reporter;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.activecheck.common.plugin.reporter.ActivecheckReporter;
import org.activecheck.common.plugin.reporter.ActivecheckReporterStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActivecheckReporterScheduler {
	private static final Logger logger = LoggerFactory.getLogger(ActivecheckReporterScheduler.class);

	private ScheduledThreadPoolExecutor executorService = null;

	public void setupExecutorService(int worker) {
		// create thread pool
		if (executorService == null || executorService.getMaximumPoolSize() > worker) {
			executorService = new ScheduledThreadPoolExecutor(worker,
					new ActivecheckReporterThreadFactory());
			executorService.setKeepAliveTime(5, TimeUnit.SECONDS);
			executorService.allowCoreThreadTimeOut(true);
		} else {
			executorService.setCorePoolSize(worker);
		}

		// try to remove canceled futures
		executorService.purge();
	}

	public void addOrUpdateReporter(ActivecheckReporter nagiosReporter,
			int reloadInterval) {
		if (nagiosReporter.isEnabled()) {
			// schedule reporter
			nagiosReporter.setDestroyAfterSeconds(reloadInterval * 2);
			reschedule(nagiosReporter);
		} else {
			remove(nagiosReporter);
		}
	}

	public void reschedule(ActivecheckReporter nagiosReporter) {
		String reporterKey = nagiosReporter.getOverallServiceName();
		ActivecheckReporterStatus reporterStatus = nagiosReporter.getStatus();
		long delay = 0;
		switch (reporterStatus) {
		case DEAD: // remove dead NagiosReporter
			remove(nagiosReporter);
			break;

		case NEW: // schedule reporter for the first time but not all at
					// once
			delay = (nagiosReporter.getScheduleIntervalInSeconds() % (executorService.getQueue().size() + 1)) * 500;
			logger.debug("Scheduling service '" + reporterKey + "' in " + delay + "ms");
			nagiosReporter.schedule(executorService, delay);
			break;

		case ERROR: // reschedule reporter with larger delay
			delay = 120 * 1000;
			logger.debug("Rescheduling erronous service '" + reporterKey + "' in " + delay + "ms");
			nagiosReporter.schedule(executorService, delay);
			break;

		case REQUEUE: // reschedule reporter
			delay = nagiosReporter.getScheduleIntervalInSeconds() * 1000;
			logger.debug("Rescheduling service '" + reporterKey + "' in " + delay + "ms");
			nagiosReporter.schedule(executorService, delay);
			break;

		default: // log status and do nothing
			logger.debug("Not scheduling service '" + reporterKey + "' in state '" + reporterStatus + "'");
			break;
		}
	}

	private void remove(ActivecheckReporter nagiosReporter) {
		String reporterKey = nagiosReporter.getOverallServiceName();

		logger.info("Removing service '" + reporterKey + "'");
		executorService.remove(nagiosReporter);
	}
}
