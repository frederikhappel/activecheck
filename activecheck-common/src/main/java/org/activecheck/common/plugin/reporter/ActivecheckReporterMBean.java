package org.activecheck.common.plugin.reporter;

import java.util.Collection;

import org.activecheck.common.nagios.NagiosServiceReport;

public interface ActivecheckReporterMBean {
	// @Description("rerun checks")
	public void runCommand() throws ActivecheckReporterException;

	// @Description("reload plugin configuration from properties")
	public void reloadConfiguration();

	// @Description("last configuration reload time")
	public String getConfigurationReloadTime();

	// @Description("Configuration file")
	public String getConfigFile();

	// @Description("destroy reporter after specified time in seconds")
	public int getDestroyAfterSeconds();

	// @Description("Nagios service name")
	public String getOverallServiceName();

	// @Description("Nagios service host")
	public String getOverallServiceHost();

	// @Description("Nagios service status")
	public String getOverallServiceStatus();

	// @Description("Nagios reporter status")
	public String getNagiosReporterStatus();

	// @Description("Nagios check interval")
	public int getCheckInterval();

	// @Description("Nagios retry interval")
	public int getRetryInterval();

	// @Description("last run time")
	public String getLastRunTime();

	// @Description("execution time in milliseconds")
	public long getExecutionTimeMillis();

	// @Description("last schedule time")
	public String getLastScheduleTime();

	// @Description("last schedule delay in milliseconds")
	public long getLastScheduleDelayMillis();

	// @Description("get all reports")
	public Collection<NagiosServiceReport> getReports();
}
