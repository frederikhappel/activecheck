package org.activecheck.plugin.reporter.jmx.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXServiceURL;

import org.activecheck.plugin.reporter.jmx.common.JMXProvider;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JMXQueryExecutor is used for local or remote request of JMX attributes It
 * requires JRE 1.5 to be used for compilation and execution. Look method main
 * for description how it can be invoked.
 * <p/>
 * This plugin was found on nagiosexchange. It lacked a username/password/role
 * system.
 *
 * @author unknown
 * @author Ryan Gravener
 *         (<a href="http://ryangravener.com/app/contact">rgravener</a>)
 * @author Per Huss mr.per.huss (a) gmail.com
 * @author Frederik Happel
 */
public class JMXQueryExecutor {
    private static final Logger logger = LoggerFactory.getLogger(JMXQueryExecutor.class);
    private String url = null;
    private String username = null;
    private String password = null;
    private JMXConnector connector;
    private final JMXProvider jmxProvider;
    private MBeanServerConnection connection = null;
    private boolean connected = false;

    public JMXQueryExecutor(final JMXProvider jmxProvider, final String url) {
        this.url = url;
        this.jmxProvider = jmxProvider;
    }

    public synchronized void reconnect() throws JMXQueryExecutorException, IOException {
        try {
            Validate.notNull(url, "JMX URL cannot be null");
            if (!connected) {
                logger.info("Connecting to '" + url + "'");
                final JMXServiceURL jmxUrl = new JMXServiceURL(url);

                if (username != null) {
                    final Map<String, String[]> m = new HashMap<>();
                    m.put(JMXConnector.CREDENTIALS, new String[]{username, password});
                    connector = jmxProvider.getConnector(jmxUrl, m);
                } else {
                    connector = jmxProvider.getConnector(jmxUrl, null);
                }
                connection = connector.getMBeanServerConnection();
                connected = true;
            }
        } catch (IllegalArgumentException e) {
            throw new JMXQueryExecutorException(e);
        }
    }

    public void authenticate(final String newUsername, final String newPassword) throws JMXQueryExecutorException, IOException {
        connect(url, newUsername, newPassword);
    }

    public void connect(final String newUrl) throws JMXQueryExecutorException, IOException {
        connect(newUrl, username, password);
    }

    public void connect(final String newUrl, final String newUsername, final String newPassword) throws JMXQueryExecutorException, IOException {
        // disconnect jmxQueryExecutor if something changed
        if (newUrl == null || (url != null && !url.equals(newUrl))
                || (username != null && !username.equals(newUsername))
                || (password != null && !password.equals(newPassword))) {
            disconnect();
        }

        // (re)connect
        url = newUrl;
        username = newUsername;
        password = newPassword;
        reconnect();
    }

    public synchronized void disconnect() {
        connected = false;
        if (connector != null) {
            logger.info("Disconnecting from '" + url + "'");
            try {
                connector.close();
                connector = null;
            } catch (IOException e) {
                logger.warn("Failed to disconnect from '" + url + "': " + e.getMessage());
                logger.trace(e.getMessage(), e);
            }
        }
    }

    public String getUrl() {
        return url;
    }

    public boolean isConnected() {
        return connected;
    }

    public Object invoke(final String object, final String attributeName, Object[] params) throws JMXQueryExecutorException, IOException {
        if (params == null) {
            params = new Object[0];
        }

        final List<String> signatureList = new ArrayList<>();
        for (Object param : params) {
            signatureList.add(param.getClass().getName());
        }
        String[] signature = signatureList.toArray(new String[signatureList.size()]);

        Object result;
        try {
            reconnect();
            result = connection.invoke(new ObjectName(object), attributeName, params, signature);
        } catch (IOException e) {
            connected = false;
            final String message = "JMX not connected";
            logger.error(message, e.getMessage());
            logger.trace(e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            final String message = "Could not invoke operation '" + attributeName + "(" + StringUtils.join(params, ",")
                    + ")' on '" + object + "': " + e.getMessage();
            logger.error(message);
            logger.trace(e.getMessage(), e);
            throw new JMXQueryExecutorException(message, e);
        }
        return result;
    }

    public Object getAttribute(final String object, final String attributeName) throws JMXQueryExecutorException, IOException {
        Object result;
        try {
            reconnect();
            result = connection.getAttribute(new ObjectName(object), attributeName);
        } catch (IOException e) {
            connected = false;
            final String message = "JMX not connected";
            logger.error(message, e.getMessage());
            logger.trace(e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            final String message =
                    "Could not get value for attribute '" + attributeName + "' on '" + object + "': " + e.getMessage();
            logger.error(message);
            logger.trace(e.getMessage(), e);
            throw new JMXQueryExecutorException(message, e);
        }
        return result;
    }
}
