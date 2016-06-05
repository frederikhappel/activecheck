package org.activecheck.common.net;

import org.activecheck.common.nagios.NagiosServiceReport;


@SuppressWarnings("unused")
public abstract class ActivecheckClient {
    protected final String address;
    protected final int port;

    public ActivecheckClient(String address, int port) {
        this.address = address;
        this.port = port;
    }

    public abstract void send(NagiosServiceReport report);
}
