package org.activecheck.common.plugin.reporter;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.activecheck.common.nagios.NagiosCheckResult;
import org.activecheck.common.nagios.NagiosServiceReport;
import org.activecheck.common.nagios.NagiosServiceReportRouting;
import org.activecheck.common.nagios.NagiosServiceStatus;
import org.activecheck.common.plugin.ActivecheckPlugin;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.softee.management.annotation.Description;
import org.softee.management.annotation.ManagedAttribute;

public abstract class ActivecheckReporter extends ActivecheckPlugin implements
		Runnable {
	// constants
	private static final Logger logger = LoggerFactory.getLogger(ActivecheckReporter.class);
	private static final int DEFAULT_CHECK_INTERVAL = 30;

	// class members
	private NagiosServiceStatus overallServiceStatus = NagiosServiceStatus.UNKNOWN;
	private String overallServiceName = null;
	private String overallServiceHost = null;
	private ActivecheckReporterStatus status = ActivecheckReporterStatus.NEW;
	ScheduledFuture<ActivecheckReporter> sf = null;
	private final Map<String, NagiosServiceReport> serviceReports = new HashMap<String, NagiosServiceReport>();

	private int checkInterval = DEFAULT_CHECK_INTERVAL;
	private int retryInterval = DEFAULT_CHECK_INTERVAL;
	private long lastRunTime = 0;
	private long executionTime = 0;
	private long lastScheduleTime = 0;
	private long lastScheduleDelay = 0;
	private int destroyAfterSeconds = 300;
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
		pluginInit();
	}

	private final void pluginInit() {
		errorCountMax = properties.getInt("max_errors", errorCountMax);
		reportRouting.setFromProperties(properties);

		// calculate intervals
		checkInterval = properties.getInt("check_interval",
				DEFAULT_CHECK_INTERVAL);
		retryInterval = properties.getInt("retry_interval",
				DEFAULT_CHECK_INTERVAL);
		if (retryInterval > checkInterval) {
			retryInterval = checkInterval;
		}

		// set report and status if not already done
		if (serviceReports.get(overallServiceName) == null) {
			setOverallServiceReport("Configuration reloaded some seconds ago");
		}
	}

	@Override
	public String getPluginName() {
		return overallServiceName;
	}

	public final int getScheduleIntervalInSeconds() {
		return (overallServiceStatus != NagiosServiceStatus.OK) ? retryInterval
				: checkInterval;
	}

	@Override
	public final void run() {
		if (status == ActivecheckReporterStatus.SCHEDULED) {
			long currentTime = System.currentTimeMillis();
			try {
				if (currentTime - lastReloadTime > destroyAfterSeconds * 1000) {
					// run cleanup jobs
					logger.info("Thread died for service '" + overallServiceName + "'");
					status = ActivecheckReporterStatus.DEAD;
					cleanUp();
				} else {
					// run task
					lastRunTime = currentTime;
					status = ActivecheckReporterStatus.RUNNING;
					try {
						runCommand();

						// clear error count
						errorCount = 0;
						status = ActivecheckReporterStatus.REQUEUE;
					} catch (ActivecheckReporterException e) {
						setOverallServiceReport(NagiosServiceStatus.CRITICAL,
								e.getMessage());

						// increase error count
						errorCount++;
						logger.debug("error count for service " + overallServiceName + " = " + errorCount);
						if (errorCountMax > 0 && errorCount >= errorCountMax) {
							status = ActivecheckReporterStatus.REQUESTSHUTDOWN;
							logger.error("reached max errors of " + errorCountMax + " for service " + overallServiceName + ". Requesting shutdown");
						} else {
							status = ActivecheckReporterStatus.ERROR;
							logger.error(e.getMessage());
							logger.debug(e.getMessage(), e);
						}
					}
				}
			} finally {
				// cancel schedule
				if (status == ActivecheckReporterStatus.DEAD && sf != null) {
					sf.cancel(true);
				}

				// notify observers
				setChanged();
				notifyObservers(status);
			}
			executionTime = System.currentTimeMillis() - lastRunTime;
		}
	}

	@ManagedAttribute
	@Description("destroy reporter after specified time in seconds")
	public final int getDestroyAfterSeconds() {
		return destroyAfterSeconds;
	}

	public final void setDestroyAfterSeconds(int destroyAfterSeconds) {
		this.destroyAfterSeconds = destroyAfterSeconds;
	}

	@ManagedAttribute
	@Description("Nagios service name")
	public final String getOverallServiceName() {
		return overallServiceName;
	}

	@ManagedAttribute
	@Description("Nagios service host")
	public final String getOverallServiceHost() {
		return overallServiceHost;
	}

	@ManagedAttribute
	@Description("Nagios service status")
	public final String getOverallServiceStatus() {
		return overallServiceStatus.toString();
	}

	public final ActivecheckReporterStatus getStatus() {
		return status;
	}

	@ManagedAttribute
	@Description("Nagios reporter status")
	public final String getNagiosReporterStatus() {
		return status.toString();
	}

	@ManagedAttribute
	@Description("Nagios check interval")
	public final int getCheckInterval() {
		return checkInterval;
	}

	@ManagedAttribute
	@Description("Nagios retry interval")
	public final int getRetryInterval() {
		return retryInterval;
	}

	@ManagedAttribute
	@Description("last configuration reload time")
	public final String getConfigurationReloadTime() {
		if (lastReloadTime > 0) {
			return new Date(lastReloadTime).toString();
		} else {
			return "NEVER";
		}
	}

	@ManagedAttribute
	@Description("last run time")
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

	@ManagedAttribute
	@Description("execution time in milliseconds")
	public final long getExecutionTimeMillis() {
		return executionTime;
	}

	@ManagedAttribute
	@Description("last schedule time")
	public final String getLastScheduleTime() {
		if (lastScheduleTime > 0) {
			return new Date(lastScheduleTime).toString();
		} else {
			return "NEVER";
		}
	}

	@ManagedAttribute
	@Description("last schedule delay in milliseconds")
	public final long getLastScheduleDelayMillis() {
		return lastScheduleDelay;
	}

	@ManagedAttribute
	@Description("Nagios service configuration file")
	public final String getConfigFile() {
		return properties.getFileName();
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
		sf = (ScheduledFuture<ActivecheckReporter>) executorService.schedule(this,
				delay,
				TimeUnit.MILLISECONDS);
		status = ActivecheckReporterStatus.SCHEDULED;
		return sf;
	}

	public final ScheduledFuture<ActivecheckReporter> getScheduledFuture() {
		return sf;
	}

	public final Collection<NagiosServiceReport> getReports() {
		return serviceReports.values();
	}

	protected final void addServiceReport(NagiosServiceReport report) {
		String reportServiceName = report.getServiceName();
		if (overallServiceName.equals(reportServiceName)) {
			logger.error("Please use setOverallServiceReport instead");
		} else {
			report.setRouting(reportRouting);
			report.hasChanged(serviceReports.get(reportServiceName));
			serviceReports.put(reportServiceName, report);
			logger.info("Service '" + reportServiceName + "': '" + status + " - " + report.getMessage() + "'");
		}
	}

	protected final void setOverallServiceReport(NagiosServiceReport report) {
		String reportServiceName = report.getServiceName();
		if (!overallServiceName.equals(reportServiceName)) {
			logger.error("Cannot use report of '" + reportServiceName + "' for '" + overallServiceName + "'");
		} else {
			report.setRouting(reportRouting);
			report.hasChanged(serviceReports.get(reportServiceName));
			overallServiceStatus = report.getStatus();
			serviceReports.put(reportServiceName, report);
			logger.info("Service '" + reportServiceName + "': '" + status + " - " + reportServiceName + "'");
		}
	}

	protected final void setOverallServiceReport(NagiosCheckResult checkResult) {
		NagiosServiceReport report = new NagiosServiceReport(
				overallServiceName, overallServiceHost, checkResult);
		setOverallServiceReport(report);
	}

	protected final void setOverallServiceReport(String message) {
		NagiosServiceReport report = new NagiosServiceReport(
				overallServiceName, overallServiceHost, overallServiceStatus);
		report.setMessage(message);
		setOverallServiceReport(report);
	}

	protected final void setOverallServiceReport(NagiosServiceStatus status,
			String message) {
		NagiosServiceReport report = new NagiosServiceReport(
				overallServiceName, overallServiceHost, status);
		report.setMessage(message);
		setOverallServiceReport(report);
	}

	@Override
	protected final void pluginReload() {
		pluginInit();
		reporterInit();
	}

	abstract protected void reporterInit();

	protected abstract void runCommand() throws ActivecheckReporterException;

	protected abstract void cleanUp();
}
