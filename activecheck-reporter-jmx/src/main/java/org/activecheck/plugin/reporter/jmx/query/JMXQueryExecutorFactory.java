package org.activecheck.plugin.reporter.jmx.query;

import org.activecheck.common.Encoding;
import org.activecheck.common.pidfile.PidFile;
import org.activecheck.common.pidfile.PidFileFactory;
import org.activecheck.plugin.reporter.jmx.common.DefaultJMXProvider;
import org.activecheck.plugin.reporter.jmx.common.JMXProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class JMXQueryExecutorFactory implements Observer {
    private static final Logger logger = LoggerFactory.getLogger(JMXQueryExecutorFactory.class);
    private static final JMXQueryExecutorFactory instance = new JMXQueryExecutorFactory();

    // define class members
    private final ConcurrentHashMap<String, JMXQueryExecutor> queryExecutorMap;
    private final ConcurrentHashMap<String, String> pidfile2url;
    private final JMXProvider jmxProvider;

    public JMXQueryExecutorFactory() {
        queryExecutorMap = new ConcurrentHashMap<>();
        pidfile2url = new ConcurrentHashMap<>();
        jmxProvider = new DefaultJMXProvider();
    }

    private String urlFromHostAndPort(String host, String port) {
        return "service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jmxrmi";
    }

    private String processLineByPid(String pid) {
        String processLine = null;
        try {
            // TODO: Linux only! Port to other OS
            final Process p = Runtime.getRuntime().exec("ps -p " + pid + " --no-headers -f");
            try (final BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream(), Encoding.UTF8))) {
                // get rmi port from -Dcom.sun.management.jmxremote.port=<port>
                processLine = input.readLine();
            } catch (IOException e) {
                logger.debug("Unable to read process line from PID: " + e.getMessage());
                logger.trace(e.getMessage(), e);
            }
            p.destroyForcibly();
        } catch (IOException e) {
            logger.debug("Unable to execute command to retrieve PID: " + e.getMessage());
            logger.trace(e.getMessage(), e);
        }
        return processLine;
    }

    @SuppressWarnings("restriction")
    public JMXQueryExecutor connectPidfile(String pidfilePath, String username, String password) throws JMXQueryExecutorException {
        // retrieve existing JMXQueryExecutor
        final List<String> urls = new ArrayList<>();
        JMXQueryExecutor queryExecutor = null;

        // try to find existing url
        urls.add(pidfile2url.get(pidfilePath));

        // translate pid to url
        final PidFile pidfile = PidFileFactory.create(pidfilePath, this);
        if (pidfile.getPid().isPresent()) {
            final Integer pid = pidfile.getPid().get();
            try {
                logger.debug("Trying to connect via pid " + pid);
                urls.add(sun.management.ConnectorAddressLink.importFrom(pid));
            } catch (IOException e) {
                logger.debug("Unable to transform PID to URL: " + e.getMessage());
                logger.trace(e.getMessage(), e);

                // get rmi port from
                // -Dcom.sun.management.jmxremote.port=<port>
                logger.debug("Trying to determine JMX port for PID " + pid);
                final String line = processLineByPid(pid.toString());
                if (line != null) {
                    final String jmxport = line.replaceAll(".*-Dcom\\.sun\\.management\\.jmxremote\\.port=([0-9]+).*", "$1");
                    if (jmxport != null) {
                        final String tempurl = urlFromHostAndPort("127.0.0.1", jmxport);
                        logger.debug("tcp connect URL for PID " + pid + " is " + tempurl);
                        urls.add(tempurl);
                    }
                }
            }
        }

        // try to connect to an url
        for (final String url : urls.stream().filter(o -> o != null).collect(Collectors.toSet())) {
            queryExecutor = queryExecutorMap.get(url);
            try {
                queryExecutor = connectUrl(url, username, password);
                pidfile2url.put(pidfilePath, url);
                break;
            } catch (JMXQueryExecutorException | IOException e) {
                logger.debug("Unable to connect to URL: " + e.getMessage());
                logger.trace(e.getMessage(), e);
            }
        }
        if (queryExecutor == null) {
            throw new JMXQueryExecutorException("Error transforming PID to URL");
        }
        return queryExecutor;
    }

    @Override
    public void update(Observable arg0, Object arg1) {
        if (arg0 instanceof PidFile) {
            PidFile pidfile = (PidFile) arg0;
            try {
                connectPidfile(pidfile.getFilename(), null, null);
            } catch (JMXQueryExecutorException e) {
                logger.error("Reconnecting RMI for pidfile '" + pidfile.getFilename() + "' failed: " + e.getMessage());
                logger.trace(e.getMessage(), e);
            }
        }
    }

    public JMXQueryExecutor connectUrl(final String url, final String username, final String password) throws JMXQueryExecutorException, IOException {
        // retrieve existing JMXQueryExecutor
        JMXQueryExecutor queryExecutor = queryExecutorMap.get(url);
        if (queryExecutor == null) {
            logger.debug("Creating new JMXQueryExecutor for '" + url + "'");
            queryExecutor = new JMXQueryExecutor(jmxProvider, url);
            queryExecutorMap.put(url, queryExecutor);
        }
        if (username == null) {
            queryExecutor.connect(url);
        } else {
            queryExecutor.connect(url, username, password);
        }
        return queryExecutor;
    }

    public void disconnect(final JMXQueryExecutor queryExecutor) {
        if (queryExecutor != null) {
            final Optional<String> url = queryExecutorMap.entrySet().stream()
                    .filter(entry -> entry.getValue().equals(queryExecutor))
                    .map(entry -> entry.getKey())
                    .findFirst();
            if (url.isPresent()) {
                logger.debug("Removing JMXQueryExecutor for '" + url + "'");
                queryExecutorMap.remove(url);

                // find entry in pidfile2url
                for (String pidfilePath : pidfile2url.keySet()) {
                    String value = pidfile2url.get(pidfilePath);
                    if (value.equals(url)) {
                        PidFileFactory.delete(pidfilePath, this);
                        pidfile2url.remove(pidfilePath);
                    }
                }

            }
            // disconnect query executor
            queryExecutor.disconnect();
        }
    }

    public static JMXQueryExecutorFactory getInstance() {
        return instance;
    }
}
