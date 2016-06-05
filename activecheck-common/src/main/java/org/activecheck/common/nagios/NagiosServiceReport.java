package org.activecheck.common.nagios;

import org.apache.commons.codec.digest.DigestUtils;

public class NagiosServiceReport extends NagiosCheckResult implements
        Comparable<NagiosServiceReport> {
    private static final long serialVersionUID = -4631772558439693705L;
    private final String serviceName;
    private volatile String serviceHost;
    private long startTime = -1;
    private long finishTime = -1;
    private boolean changed = true;
    private NagiosServiceReportRouting reportRouting = new NagiosServiceReportRouting();

    public NagiosServiceReport(String serviceName, String serviceHost,
                               NagiosServiceStatus status, String message, long startTime,
                               long finishTime) {
        super();
        this.serviceName = serviceName;
        this.serviceHost = serviceHost;
        this.startTime = startTime;
        this.finishTime = finishTime;
        setStatus(status);
        parseMessage(message);
    }

    public NagiosServiceReport(String serviceName, String serviceHost,
                               NagiosServiceStatus status) {
        super();
        this.serviceName = serviceName;
        this.serviceHost = serviceHost;
        setStatus(status);
    }

    public NagiosServiceReport(String serviceName, String serviceHost, NagiosCheckResult checkResult) {
        super();
        this.serviceName = serviceName;
        this.serviceHost = serviceHost;
        merge(checkResult);
    }

    public final String getServiceHost() {
        return serviceHost;
    }

    public final boolean setServiceHost(String serviceHost) {
        if (this.serviceHost == null && serviceHost != null) {
            this.serviceHost = serviceHost;
            return true;
        }
        return false;
    }

    public final String getServiceName() {
        return serviceName;
    }

    public final long getStartTime() {
        return startTime;
    }

    public final boolean setStartTime(long startTime) {
        if (this.startTime < 0 && startTime > 0) {
            this.startTime = startTime;
            return true;
        }
        return false;
    }

    public final long getFinishTime() {
        return finishTime;
    }

    public final boolean setFinishTime(long finishTime) {
        if (this.finishTime < 0 && finishTime > 0) {
            this.finishTime = finishTime;
            return true;
        }
        return false;
    }

    public final NagiosServiceReportRouting getRouting() {
        return reportRouting;
    }

    public final void setRouting(NagiosServiceReportRouting reportRouting) {
        this.reportRouting = reportRouting;
    }

    public boolean hasChanged(NagiosServiceReport report) {
        if (report != null) {
            final String md5self = DigestUtils.md5Hex(toString());
            final String md5report = DigestUtils.md5Hex(report.toString());
            changed = getStatus() != report.getStatus()
                    || !md5self.equals(md5report);
        }
        return changed;
    }

    public boolean hasChanged() {
        return changed;
    }

    /*
     * Note: this class has a natural ordering that is inconsistent with equals.
     */
    @Override
    public int compareTo(NagiosServiceReport o) {
        return o.getStatus().getSeverityOrder() - getStatus().getSeverityOrder();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof NagiosServiceReport) {
            final NagiosServiceReport comparable = (NagiosServiceReport) o;
            return comparable.getServiceHost().equals(serviceHost)
                    && comparable.getServiceName().equals(serviceName);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return (serviceHost + serviceName).hashCode();
    }

    @Override
    public String toString() {
        return getStatus() + " - " + getMessageWithPerformancedata();
    }
}
