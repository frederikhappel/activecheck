package org.activecheck.common.plugin.collector;

import org.apache.commons.lang.Validate;

import java.net.InetSocketAddress;

public class GenericCollector {
    private String hostname = "localhost";
    private String domain = "localnet";
    private int port = 0;
    private boolean changed = false;
    private static final String proto = "tcp";

    public GenericCollector() {
    }

    public GenericCollector(String hostWithPort, int defaultPort) {
        Validate.notNull(hostWithPort);
        final String[] hostParts = hostWithPort.toLowerCase().split(":");
        setPort(hostParts.length > 1 ? Integer.parseInt(hostParts[1])
                : defaultPort);
        setFqdn(hostParts[0]);
    }

    public GenericCollector(String hostWithPort) {
        this(hostWithPort, 0);
    }

    public GenericCollector(GenericCollector host) {
        setPort(host.getPort());
        setFqdn(host.getFqdn());
    }

    public void setFqdn(String fqdn) {
        Validate.notNull(fqdn);
        if (!fqdn.equals(getFqdn())) {
            final String[] fqdnParts = fqdn.split("\\.", 2);
            hostname = fqdnParts[0];
            domain = fqdnParts.length > 1 ? fqdnParts[1] : null;
            changed = true;
        }
    }

    public void setPort(int port) {
        Validate.isTrue(port >= 0);
        Validate.isTrue(port <= 65535);
        if (this.port != port) {
            this.port = port;
            changed = true;
        }
    }

    @SuppressWarnings("unused")
    public boolean hasChanged() {
        if (changed) {
            changed = false;
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return getFqdn();
    }

    public String getFqdn() {
        if (domain != null) {
            return String.format("%s.%s", hostname, domain);
        } else {
            return hostname;
        }
    }

    public String getHostname() {
        return hostname;
    }

    public String getDomain() {
        return domain;
    }

    public int getPort() {
        return port;
    }

    public InetSocketAddress getSocketAddress() {
        return new InetSocketAddress(getFqdn(), port);
    }

    public String getUrl() {
        return String.format("%s://%s:%d", proto, getFqdn(), port);
    }
}
