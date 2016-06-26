package org.activecheck.plugin.reporter.jmx;

import org.activecheck.common.nagios.NagiosServiceStatus;
import org.activecheck.common.plugin.ActivecheckPluginProperties;
import org.activecheck.common.plugin.reporter.ActivecheckReporter;
import org.activecheck.common.plugin.reporter.ActivecheckReporterException;
import org.activecheck.plugin.reporter.jmx.query.JMXQuery;
import org.activecheck.plugin.reporter.jmx.query.JMXQueryExecutor;
import org.activecheck.plugin.reporter.jmx.query.JMXQueryExecutorException;
import org.activecheck.plugin.reporter.jmx.query.JMXQueryExecutorFactory;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ActivecheckPluginProperties(propertiesToMerge = {"jmx.username", "jmx.password"})
public class JMXReporter extends ActivecheckReporter {
    private static final Logger logger = LoggerFactory.getLogger(JMXReporter.class);

    private JMXQueryExecutor jmxQueryExecutor = null;
    private JMXQuery jmxQuery = null;

    public JMXReporter(PropertiesConfiguration properties) {
        super(properties);

        // initialize what has not been initialized
        reporterInit();
    }

    @Override
    protected void reporterInit() {
        // parse configuration file
        // recreate JMXNagiosService instances
        final String query = properties.getString("query");
        final boolean isOperation = properties.getBoolean("operation", false);
        final List<Object> rawArguments = properties.getList("arguments", null);
        Object[] arguments = new Object[0];
        if (rawArguments != null) {
            final Pattern pattern = Pattern.compile("\\[(.*?)\\]");
            arguments = rawArguments.stream()
                    .filter(rawArgument -> rawArgument != null)
                    .map(rawArgument -> {
                        if (rawArgument instanceof String) {
                            final Matcher matcher = pattern.matcher((String) rawArgument);
                            if (matcher.find()) {
                                final String argument = matcher.group(1);
                                if (argument.isEmpty()) {
                                    rawArgument = new String[0];
                                } else {
                                    rawArgument = argument.split("\\,");
                                }
                            }
                        }
                        return rawArgument;
                    })
                    .collect(Collectors.toList())
                    .toArray();
        }
        jmxQuery = new JMXQuery(query, isOperation, arguments);
    }

    @Override
    public void runCommand() throws ActivecheckReporterException {
        if (jmxQueryExecutor == null || !jmxQueryExecutor.isConnected()) {
            logger.info("Cannot run query. JMX is not connected. Trying to (re)connect.");
            try {
                // (re)create jmxQueryExecutor
                final String username = properties.getString("jmx.username", "");
                final String password = properties.getString("jmx.password", "");
                final String pidfilePath = properties.getString("pidfile", null);
                final String url = properties.getString("url", null);
                if (pidfilePath != null) {
                    jmxQueryExecutor = JMXQueryExecutorFactory.getInstance()
                            .connectPidfile(pidfilePath, username, password);
                } else {
                    jmxQueryExecutor = JMXQueryExecutorFactory.getInstance()
                            .connectUrl(url, username, password);
                }
            } catch (JMXQueryExecutorException | IOException e) {
                jmxQueryExecutor = null;
                final String errorMessage = "JMXReporter Configuration Error for service '"
                        + getOverallServiceName() + "': " + e.getMessage();

                logger.error(errorMessage);
                logger.trace(e.getMessage(), e);

                // set report and status
                setOverallServiceReport(NagiosServiceStatus.CRITICAL, errorMessage);
                throw new ActivecheckReporterException(e);
            }
        }
        if (jmxQuery == null) {
            final String errorMessage = "no query or operation has been defined for service '" + getOverallServiceName() + "'";
            logger.error(errorMessage);
            setOverallServiceReport(NagiosServiceStatus.WARNING, errorMessage);
        } else {
            // execute query
            logger.debug("Service '" + getOverallServiceName()
                    + "': Running JMX query '" + jmxQuery.getQuery()
                    + "' on url '" + jmxQueryExecutor.getUrl() + "'");
            try {
                jmxQuery.execute(jmxQueryExecutor);

                // generate report
                setOverallServiceReport(jmxQuery.getCheckResult());
                logger.debug("Service '"
                        + getOverallServiceName()
                        + "': '"
                        + jmxQuery.getCheckResult()
                        .getMessageWithPerformancedata() + "'");

            } catch (Exception e) {
                logger.error("Error running JMX query: '" + jmxQuery.getQuery()
                        + "': " + e.getMessage());
                logger.trace(e.getMessage(), e);
                setOverallServiceReport(NagiosServiceStatus.CRITICAL,
                        e.getMessage());
            }
        }
    }

    @Override
    protected void cleanUp() {
        JMXQueryExecutorFactory.getInstance().disconnect(jmxQueryExecutor);
    }
}
