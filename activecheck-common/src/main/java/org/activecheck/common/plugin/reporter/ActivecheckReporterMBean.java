package org.activecheck.common.plugin.reporter;

import org.activecheck.common.plugin.ActivecheckPluginMBean;

import java.util.List;

@SuppressWarnings("unused")
public interface ActivecheckReporterMBean extends ActivecheckPluginMBean {
    // @Description("rerun checks")
    String runCommandOperation();

    // @Description("Nagios service name")
    String getOverallServiceName();

    // @Description("Nagios service host")
    String getOverallServiceHost();

    // @Description("Nagios service status")
    String getOverallServiceStatus();

    // @Description("Nagios reporter status")
    String getNagiosReporterStatus();

    // @Description("Nagios check interval")
    int getCheckInterval();

    // @Description("Nagios retry interval")
    int getRetryInterval();

    // @Description("last run time")
    String getLastRunTime();

    // @Description("next run time")
    String getNextRunTime();

    // @Description("execution time in milliseconds")
    long getExecutionTimeMillis();

    // @Description("last schedule time")
    String getLastScheduleTime();

    // @Description("last schedule delay in milliseconds")
    long getLastScheduleDelayMillis();

    // @Description("get all performance data lines")
    List<String> getPerformanceData();

    // @Description("run a fixit command for the current status")
    String fixit();
}
