package org.activecheck.plugin.reporter;

import org.activecheck.common.plugin.reporter.ActivecheckReporter;
import org.activecheck.common.plugin.reporter.ActivecheckReporterStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ActivecheckReporterScheduler {
    private static final Logger logger = LoggerFactory.getLogger(ActivecheckReporterScheduler.class);

    private ScheduledThreadPoolExecutor reporterExecutorService = null;
    private final ActivecheckFixitRunner fixitRunner;

    public ActivecheckReporterScheduler() {
        fixitRunner = new ActivecheckFixitRunner();
        final Thread fixitExecutor = new Thread(fixitRunner);
        fixitExecutor.setName("FIXIT Executor");
        fixitExecutor.start();
    }

    public void setupExecutorService(int worker) {
        // create reporter scheduler thread pool
        if (reporterExecutorService == null
                || reporterExecutorService.getMaximumPoolSize() > worker) {
            reporterExecutorService = new ScheduledThreadPoolExecutor(worker,
                    new ActivecheckReporterThreadFactory());
            reporterExecutorService.setKeepAliveTime(5, TimeUnit.SECONDS);
            reporterExecutorService.allowCoreThreadTimeOut(true);
        } else {
            reporterExecutorService.setCorePoolSize(worker);
        }

        // try to remove canceled futures
        reporterExecutorService.purge();
    }

    public void addOrUpdateReporter(ActivecheckReporter reporter,
                                    int reloadInterval) {
        if (reporter.isEnabled()) {
            // schedule reporter
            reporter.setDestroyAfterSeconds(reloadInterval * 2);
            reschedule(reporter);
        } else {
            remove(reporter);
        }
    }

    public void reschedule(ActivecheckReporter reporter) {
        final String reporterKey = reporter.getOverallServiceName();
        final ActivecheckReporterStatus reporterStatus = reporter.getStatus();
        long delay;
        switch (reporterStatus) {
            case DEAD: // remove dead NagiosReporter
                remove(reporter);
                break;

            case NEW: // schedule reporter for the first time but not all at
                // once
                delay = (reporter.getScheduleIntervalInSeconds() % (reporterExecutorService
                        .getQueue().size() + 1)) * 500;
                logger.debug("Scheduling service '{}' in {}ms", reporterKey, delay);
                reporter.schedule(reporterExecutorService, delay);
                break;

            case ERROR: // reschedule reporter with larger delay
                delay = 120 * 1000;
                logger.debug("Rescheduling erronous service '{}' in {}ms", reporterKey, delay);
                reporter.schedule(reporterExecutorService, delay);
                break;

            case REQUEUE: // reschedule reporter
                delay = reporter.getScheduleIntervalInSeconds() * 1000;
                logger.debug("Rescheduling service '{}' in {}ms", reporterKey, delay);
                reporter.schedule(reporterExecutorService, delay);
                break;

            default: // log status and do nothing
                logger.debug("Not scheduling service '{}' in state '{}'", reporterKey, reporterStatus);
                break;
        }
        fixitRunner.add(reporter);
    }

    private void remove(ActivecheckReporter reporter) {
        final String reporterKey = reporter.getOverallServiceName();

        logger.info("Removing service '{}'", reporterKey);
        reporterExecutorService.remove(reporter);
    }
}
