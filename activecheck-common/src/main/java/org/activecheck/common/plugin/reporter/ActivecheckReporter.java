package org.activecheck.common.plugin.reporter;

import org.activecheck.common.Encoding;
import org.activecheck.common.nagios.NagiosCheckResult;
import org.activecheck.common.nagios.NagiosPerformanceData;
import org.activecheck.common.nagios.NagiosServiceReport;
import org.activecheck.common.nagios.NagiosServiceReportRouting;
import org.activecheck.common.nagios.NagiosServiceStatus;
import org.activecheck.common.plugin.ActivecheckPlugin;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public abstract class ActivecheckReporter extends ActivecheckPlugin implements Runnable, ActivecheckReporterMBean {
    private static final Logger logger = LoggerFactory.getLogger(ActivecheckReporter.class);
    private static final int DEFAULT_CHECK_INTERVAL = 30;

    // class members
    private NagiosServiceStatus overallServiceStatus = NagiosServiceStatus.UNKNOWN;
    private String overallServiceName = null;
    private String overallServiceHost = null;
    private ActivecheckReporterStatus status = ActivecheckReporterStatus.NEW;
    private ScheduledFuture<ActivecheckReporter> sf = null;
    private final Map<String, NagiosServiceReport> serviceReports = new HashMap<>();

    private int checkInterval = DEFAULT_CHECK_INTERVAL;
    private int retryInterval = DEFAULT_CHECK_INTERVAL;
    private long lastRunTime = 0;
    private long executionTime = 0;
    private long lastScheduleTime = 0;
    private long lastScheduleDelay = 0;
    private final NagiosServiceReportRouting reportRouting;
    private int errorCountMax = 0;
    private int errorCount = 0;

    public ActivecheckReporter(PropertiesConfiguration properties) {
        super(properties);
        Validate.notNull(properties.getProperty("servicename"));

        overallServiceName = properties.getString("servicename");
        overallServiceHost = properties.getString("servicehost", null);
        reportRouting = new NagiosServiceReportRouting(properties);

        // initialize what has not been initialized
        setPluginName(overallServiceName);
        pluginInit();
    }

    private void pluginInit() {
        errorCountMax = properties.getInt("max_errors", errorCountMax);
        reportRouting.setFromProperties(properties);

        // calculate intervals
        checkInterval = properties.getInt("check_interval", DEFAULT_CHECK_INTERVAL);
        retryInterval = properties.getInt("retry_interval", DEFAULT_CHECK_INTERVAL);
        if (retryInterval > checkInterval) {
            retryInterval = checkInterval;
        }

        // set report and status if not already done
        if (serviceReports.get(overallServiceName) == null) {
            setOverallServiceReport("Configuration reloaded some seconds ago");
        }
    }

    public final int getScheduleIntervalInSeconds() {
        return (overallServiceStatus != NagiosServiceStatus.OK) ? retryInterval : checkInterval;
    }

    @Override
    public final void run() {
        if (!isEnabled()) {
            // kill this reporter
            kill();
        } else if (status == ActivecheckReporterStatus.SCHEDULED) {
            // run task
            lastRunTime = System.currentTimeMillis();
            status = ActivecheckReporterStatus.RUNNING;
            try {
                runCommand();

                // clear error count
                errorCount = 0;
                status = ActivecheckReporterStatus.REQUEUE;
            } catch (ActivecheckReporterException e) {
                setOverallServiceReport(NagiosServiceStatus.CRITICAL, e.getMessage());

                // increase error count
                errorCount++;
                logger.debug("error count for service {} = {}", overallServiceName, errorCount);
                if (errorCountMax > 0 && errorCount >= errorCountMax) {
                    status = ActivecheckReporterStatus.REQUESTSHUTDOWN;
                    logger.error("reached max errors of {} for service {}. Requesting shutdown",
                            errorCountMax, overallServiceName);
                } else {
                    status = ActivecheckReporterStatus.ERROR;
                    logger.error(e.getMessage());
                    logger.debug(e.getMessage(), e);
                }
            }
            executionTime = System.currentTimeMillis() - lastRunTime;
        }
        // notify observers
        setChanged();
        notifyObservers(status);
    }

    public final String getOverallServiceName() {
        return overallServiceName;
    }

    public final String getOverallServiceHost() {
        return overallServiceHost;
    }

    public final String getOverallServiceStatus() {
        return overallServiceStatus.toString();
    }

    public final ActivecheckReporterStatus getStatus() {
        return status;
    }

    public final String getNagiosReporterStatus() {
        return status.toString();
    }

    public final int getCheckInterval() {
        return checkInterval;
    }

    public final int getRetryInterval() {
        return retryInterval;
    }

    public final String getLastRunTime() {
        if (lastRunTime > 0) {
            return new Date(lastRunTime).toString();
        } else {
            return "NEVER";
        }
    }

    public final long getLastRunTimeMillis() {
        return lastRunTime;
    }

    public final String getNextRunTime() {
        if (lastRunTime > 0) {
            return new Date(lastRunTime + lastScheduleDelay).toString();
        } else {
            return new Date(System.currentTimeMillis() + lastScheduleDelay).toString();
        }
    }

    public final long getExecutionTimeMillis() {
        return executionTime;
    }

    public final String getLastScheduleTime() {
        if (lastScheduleTime > 0) {
            return new Date(lastScheduleTime).toString();
        } else {
            return "NEVER";
        }
    }

    public final long getLastScheduleDelayMillis() {
        return lastScheduleDelay;
    }

    @SuppressWarnings("unchecked")
    public final ScheduledFuture<ActivecheckReporter> schedule(
            ScheduledThreadPoolExecutor executorService, long delay) {
        // cancel schedule if already existing
        lastScheduleTime = System.currentTimeMillis();
        lastScheduleDelay = delay;
        if (sf != null) {
            sf.cancel(true);
        }
        sf = (ScheduledFuture<ActivecheckReporter>) executorService.schedule(this, delay, TimeUnit.MILLISECONDS);
        status = ActivecheckReporterStatus.SCHEDULED;
        return sf;
    }

    public final ScheduledFuture<ActivecheckReporter> getScheduledFuture() {
        return sf;
    }

    public final Collection<NagiosServiceReport> getReports() {
        return serviceReports.values();
    }

    public final List<String> getPerformanceData() {
        // TODO: replace with stream API
//        return (List<String>) serviceReports.values().stream()
//                .filter(report -> report != null)
//                .map(report -> report.getPerfData().stream()
//                        .filter(perfdata -> perfdata != null)
//                        .map(perfdata -> perfdata.getLine())
//                        .collect(Collectors.toList())
//                )
//                .collect(Collectors.toList());
        final List<String> perfDataLines = new ArrayList<>();
        for (NagiosServiceReport report : serviceReports.values()) {
            for (NagiosPerformanceData perfData : report.getPerfData()) {
                perfDataLines.add(perfData.getLine());
            }
        }
        return perfDataLines;
    }

    protected final void addServiceReport(NagiosServiceReport report) {
        final String reportServiceName = report.getServiceName();
        if (overallServiceName.equals(reportServiceName)) {
            logger.error("Please use setOverallServiceReport instead");
        } else {
            report.setRouting(reportRouting);
            report.hasChanged(serviceReports.get(reportServiceName));
            serviceReports.put(reportServiceName, report);
            logger.info("Service '{}': '{} - {}'", reportServiceName, report.getStatus(), report.getMessage());
        }
    }

    protected final void setOverallServiceReport(NagiosServiceReport report) {
        final String reportServiceName = report.getServiceName();
        if (!overallServiceName.equals(reportServiceName)) {
            logger.error("Cannot use report of '{}' for '{}'", reportServiceName, overallServiceName);
        } else {
            report.setRouting(reportRouting);
            report.hasChanged(serviceReports.get(reportServiceName));
            overallServiceStatus = report.getStatus();
            serviceReports.put(reportServiceName, report);
            logger.info("Service '{}': '{} - {}'", reportServiceName, overallServiceStatus, report.getMessage());
        }
    }

    protected final void setOverallServiceReport(NagiosCheckResult checkResult) {
        final NagiosServiceReport report = new NagiosServiceReport(
                overallServiceName, overallServiceHost, checkResult);
        setOverallServiceReport(report);
    }

    protected final void setOverallServiceReport(String message) {
        final NagiosServiceReport report = new NagiosServiceReport(
                overallServiceName, overallServiceHost, overallServiceStatus);
        report.setMessage(message);
        setOverallServiceReport(report);
    }

    protected final void setOverallServiceReport(NagiosServiceStatus status, String message) {
        final NagiosServiceReport report = new NagiosServiceReport(
                overallServiceName, overallServiceHost, status);
        report.setMessage(message);
        setOverallServiceReport(report);
    }

    @Override
    protected final void pluginReload() {
        pluginInit();
        reporterInit();
    }

    public final String fixit() {
        final String reportServiceName = getOverallServiceName();
        final String fixitKey = String.format("fixit_%s", getOverallServiceStatus().toLowerCase());
        final String command = properties.getString(fixitKey);
        final StringBuilder exitMessage = new StringBuilder();
        if (command != null && !command.isEmpty()) {
            logger.info("Service '{}': trying to run '{}'", reportServiceName, command);
            Process p = null;
            try {
                p = Runtime.getRuntime().exec(command);
                p.waitFor();
                try (final BufferedReader reader = new BufferedReader(
                        new InputStreamReader(p.getInputStream(), Encoding.UTF8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        exitMessage.append(line);
                        logger.info("Service '{}' FIXIT: {}", reportServiceName, line);
                    }
                } catch (IOException e) {
                    final String message = String.format(
                            "Failed to run FIXIT command '%s': %s",
                            command, e.getMessage()
                    );
                    exitMessage.append(message);
                    logger.error(message);
                }
            } catch (IOException | InterruptedException e) {
                final String message = String.format(
                        "Failed to run FIXIT command '%s': %s",
                        command, e.getMessage()
                );
                exitMessage.append(message);
                logger.error(message);
            } finally {
                if (p != null) {
                    p.destroyForcibly();
                }
            }
        } else {
            final String message = String.format(
                    "Service '%s' FIXIT for status '%s' is not defined",
                    reportServiceName, getOverallServiceStatus()
            );
            exitMessage.append(message);
            logger.debug(message);
        }
        return exitMessage.toString();
    }

    @Override
    public String runCommandOperation() {
        try {
            runCommand();
            return String.format("Status: %s", getOverallServiceStatus());
        } catch (ActivecheckReporterException e) {
            return e.getMessage();
        }
    }

    private void kill() {
        // set status to dead
        status = ActivecheckReporterStatus.DEAD;

        // cancel schedule
        if (sf != null) {
            sf.cancel(true);
        }

        // run cleanup jobs
        cleanUp();
    }

    @Override
    public final String disable() {
        kill();
        return super.disable();
    }

    abstract protected void reporterInit();

    abstract protected void cleanUp();

    abstract public void runCommand() throws ActivecheckReporterException;
}
