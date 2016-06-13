package org.activecheck.plugin.reporter;

import org.activecheck.MBeanRegistry;
import org.activecheck.common.plugin.reporter.ActivecheckReporter;
import org.activecheck.common.plugin.reporter.ActivecheckReporterStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.TimeUnit;

public class ActivecheckReporterScheduler implements Observer {
    private static final Logger logger = LoggerFactory.getLogger(ActivecheckReporterScheduler.class);
    private static final String JMX_OBJECT_TYPE = "Reporters";

    private final ActivecheckReporterExecutor reporterExecutorService;
    private final ActivecheckFixitRunner fixitRunner;

    public ActivecheckReporterScheduler() {
        /**
         * Initialise an executor for the reporters
         */
        reporterExecutorService = new ActivecheckReporterExecutor(1);
        reporterExecutorService.setKeepAliveTime(5, TimeUnit.SECONDS);
        reporterExecutorService.allowCoreThreadTimeOut(true);
        MBeanRegistry.getInstance().register("ReporterExecutor", null, reporterExecutorService);

        /**
         * Fixit runner should always be only one thread to avoid that malicious
         * fixit operations could occupy all available threads
         */
        fixitRunner = new ActivecheckFixitRunner();
        final Thread fixitExecutor = new Thread(fixitRunner);
        fixitExecutor.setName("FIXIT Executor");
        fixitExecutor.start();
    }

    public void setupExecutorService(int worker) {
        // set reporter scheduler thread pool
        reporterExecutorService.setCorePoolSize(worker);

        // try to remove canceled futures
        reporterExecutorService.purge();
    }

    public void addOrUpdateReporter(ActivecheckReporter reporter) {
        if (isActive(reporter)) {
            final String reporterName = reporter.getOverallServiceName();
            logger.info("Adding service '{}'", reporter.getPluginName());

            // add to jmx
            MBeanRegistry.getInstance().register(JMX_OBJECT_TYPE, reporterName, reporter);

            // set scheduler as observer as rescheduling is handled here
            reporter.addObserver(this);

            // schedule reporter
            reschedule(reporter);
        }
    }

    private void reschedule(ActivecheckReporter reporter) {
        final ActivecheckReporterStatus reporterStatus = reporter.getStatus();
        final String reporterKey = reporter.getOverallServiceName();
        long delay;
        switch (reporterStatus) {
            case NEW: // schedule reporter for the first time but not all reporters at once
                delay = (reporter.getScheduleIntervalInSeconds() % (reporterExecutorService.getQueue().size() + 1)) * 500;
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
        // add reporter to fixit runner
        fixitRunner.add(reporter);
    }

    /**
     * This method is called whenever the observed object is changed. An
     * application calls an <tt>Observable</tt> object's
     * <code>notifyObservers</code> method to have all the object's
     * observers notified of the change.
     *
     * @param o   the observable object.
     * @param arg an argument passed to the <code>notifyObservers</code>
     */
    @Override
    public void update(Observable o, Object arg) {
        logger.debug("Received update from {}", o.getClass());
        if (o instanceof ActivecheckReporter) {
            final ActivecheckReporter reporter = (ActivecheckReporter) o;
            if (reporter.getStatus() == ActivecheckReporterStatus.REQUESTSHUTDOWN) {
                final String errorMessage = String.format("Shutdown due to request from %s", reporter.getOverallServiceName());
                logger.error(errorMessage);
                System.exit(1);
            }
            if (isActive(reporter)) {
                reschedule(reporter);
            }
        }
    }

    private boolean isActive(ActivecheckReporter reporter) {
        final ActivecheckReporterStatus reporterStatus = reporter.getStatus();
        final boolean active = reporter.isEnabled() && !reporterStatus.equals(ActivecheckReporterStatus.DEAD);
        if (!active) {
            final String reporterName = reporter.getPluginName();
            logger.info("Removing service '{}'", reporterName);

            // remove all references
            reporterExecutorService.remove(reporter);
            reporter.deleteObserver(this);
            MBeanRegistry.getInstance().unregister(JMX_OBJECT_TYPE, reporterName);
        }
        return active;
    }
}
