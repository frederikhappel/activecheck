package org.activecheck;

import com.beust.jcommander.JCommander;
import org.activecheck.common.Encoding;
import org.activecheck.common.nagios.NagiosServiceReport;
import org.activecheck.common.nagios.NagiosServiceStatus;
import org.activecheck.common.plugin.ActivecheckPlugin;
import org.activecheck.common.plugin.collector.ActivecheckCollector;
import org.activecheck.common.plugin.reporter.ActivecheckReporter;
import org.activecheck.net.ActivecheckServer;
import org.activecheck.net.TcpActivecheckServer;
import org.activecheck.plugin.ActivecheckPluginFactory;
import org.activecheck.plugin.collector.ActivecheckPacketProcessor;
import org.activecheck.plugin.collector.StdoutHost;
import org.activecheck.plugin.reporter.ActivecheckReporterScheduler;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

/**
 * Activecheck is used to submit passive checks to NSCA services. While it is
 * intended to run as a daemon it can be used command line program as well.
 *
 * @author Frederik Happel mail@frederikhappel.de
 */
public class Activecheck {
    private static final Logger logger = LoggerFactory.getLogger(Activecheck.class);
    private static final String VERSION = Activecheck.class.getPackage().getImplementationVersion();

    private String selfJarChecksum = null;
    private boolean killOnChecksumMismatch = true;
    private int reloadInterval = 3600;
    private int checkDumpInterval = 10;
    private int hostCheckInterval = 60;

    private ActivecheckConfiguration configuration;
    private final ActivecheckPluginFactory pluginFactory;
    private final ActivecheckReporterScheduler reporterScheduler;
    private final ActivecheckPacketProcessor activecheckPacketProcessor;
    private final CheckDumper checkDumper;

    private String localFqdn = "localhost";

    public Activecheck(String cfgfile) {
        // determine local host name
        try {
            localFqdn = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            logger.error("Unable to determine own hostname: {}", e.getMessage());
            logger.trace(e.getMessage(), e);
        }

        // get properties from configuration file
        try {
            configuration = new ActivecheckConfiguration(cfgfile);
        } catch (ConfigurationException e) {
            final String errorMessage = String.format("Error creating configuration object '%s': %s", cfgfile, e.getMessage());
            logger.error(errorMessage);
            logger.trace(e.getMessage(), e);
            System.exit(1);
        }

        // initialize members
        pluginFactory = new ActivecheckPluginFactory();
        reporterScheduler = new ActivecheckReporterScheduler();
        activecheckPacketProcessor = new ActivecheckPacketProcessor(localFqdn);
        checkDumper = new CheckDumper();

        // log to console
        if (configuration.logToConsole()) {
            activecheckPacketProcessor.addOrUpdateCollector(new StdoutHost());
        }

        // load configuration
        reloadConfiguration();
    }

    private void calculateSelfMD5Hash() {
        try (final InputStream is = new FileInputStream(new File(Activecheck.class
                .getProtectionDomain().getCodeSource().getLocation().toURI()))) {
            final String md5 = DigestUtils.md5Hex(is);
            if (selfJarChecksum == null) {
                selfJarChecksum = md5;
            } else if (!md5.equals(selfJarChecksum)) {
                final String exitMessage = "Checksum of running JAR does not match checksum of available JAR. Stopping ActiveCheck";
                logger.info(exitMessage);
                System.exit(0);
            }
        } catch (IOException | URISyntaxException e) {
            // to avoid that this exception occurs again, set killOnChecksumMismatch = false
            killOnChecksumMismatch = false;
            logger.warn("Cannot kill self on checksum mismatch: {}", e.getMessage());
            logger.trace(e.getMessage(), e);
        }
    }

    private void reloadConfiguration() {
        logger.info("Reloading configuration");

        final String pidfile = configuration.getPidFile();
        if (pidfile != null) {
            killOnChecksumMismatch = configuration.monitorJarFile();
            if (killOnChecksumMismatch) {
                calculateSelfMD5Hash();
            } else {
                logger.debug("Monitoring of JAR file is disabled");
            }

            final String[] pid_parts = ManagementFactory.getRuntimeMXBean().getName().split("@");
            try (final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(pidfile), Encoding.UTF8))) {
                writer.write(pid_parts[0]);
            } catch (IOException e) {
                logger.error("Unable to write pidfile '{}': {}", pidfile, e.getMessage());
                logger.trace(e.getMessage(), e);
                System.exit(1);
            }
        }
        reloadInterval = configuration.getConfigurationReloadInterval();
        hostCheckInterval = configuration.getHostCheckInterval();

        // should we dump failed checks into a file?
        checkDumpInterval = configuration.getCheckDumpInterval();
        checkDumper.setCheckDumpFile(configuration.getCheckDumpFile());

        // update worker pool size and cleanup dead reporters
        reporterScheduler.setupExecutorService(configuration.getWorker());

        // update plugins and reporters
        pluginFactory.setPluginDir(configuration.getPluginDir());
        for (File configFile : configuration.getPluginConfigurationFiles()) {
            logger.debug("Including configuration in file {}", configFile);
            try {
                // load properties from file
                final ActivecheckPlugin activecheckPlugin = pluginFactory.createPlugin(configFile, configuration, reloadInterval * 2);

                // determine if collector or reporter
                if (activecheckPlugin instanceof ActivecheckReporter) {
                    // update or create NagiosReporter and thread
                    reporterScheduler.addOrUpdateReporter((ActivecheckReporter) activecheckPlugin);
                    activecheckPlugin.addObserver(activecheckPacketProcessor);
                } else if (activecheckPlugin instanceof ActivecheckCollector) {
                    activecheckPacketProcessor.addOrUpdateCollector((ActivecheckCollector) activecheckPlugin);
                } else {
                    logger.error("unknown plugin type '{}'", activecheckPlugin.getClass());
                }
            } catch (Exception e) {
                logger.error("Cannot create plugin defined in '{}': {}", configFile, e.getMessage());
                logger.debug(e.getMessage(), e);
            }
        }
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public void start() {
        if (configuration.isNscaProxy()) {
            // create listening socket
            final InetAddress bindAddress = InetAddress.getLoopbackAddress();
            final int bindPort = configuration.getBindPort();
            try {
                final ActivecheckServer activecheckServer = new TcpActivecheckServer(bindAddress, bindPort);
                activecheckServer.addObserver(activecheckPacketProcessor);
                final Thread t = new Thread(activecheckServer);
                t.setName("ActivecheckServer");
                t.start();
            } catch (IOException e) {
                final String errorMessage = String.format("Unable to create a server socket at 127.0.0.1:%d", bindPort);
                logger.error(errorMessage);
                logger.trace(e.getMessage(), e);
                System.exit(1);
            }
        }

        // main loop
        long lastReloadMillis = System.currentTimeMillis();
        long lastHostCheckMillis = 0;
        long lastCheckDumpMillis = 0;
        while (true) {
            final long time = System.currentTimeMillis();

            // reload configuration?
            long diffMillis = time - lastReloadMillis;
            if (reloadInterval * 1000 - diffMillis <= 1000) {
                lastReloadMillis = time;
                logger.debug("Last reload {} milliseconds ago", diffMillis);
                reloadConfiguration();
                logger.debug("Next reload in {}s", reloadInterval);
            }

            // submit a host check result?
            diffMillis = time - lastHostCheckMillis;
            if (hostCheckInterval * 1000 - diffMillis <= 1000) {
                lastHostCheckMillis = time;
                // send host up notification
                logger.debug("Last host check {} milliseconds ago", diffMillis);
                final long currentTime = System.currentTimeMillis();
                final long uptime = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
                final String message = String.format("Uptime %d seconds%nActivecheck version %s", uptime, VERSION);
                final NagiosServiceReport report = new NagiosServiceReport(
                        null, localFqdn, NagiosServiceStatus.OK, message, currentTime, currentTime
                );
                activecheckPacketProcessor.process(report);
                logger.debug("Next host check in {}s", hostCheckInterval);
            }

            // output failed checks to file
            diffMillis = time - lastCheckDumpMillis;
            if (checkDumpInterval * 1000 - diffMillis <= 1000) {
                lastCheckDumpMillis = time;
                // dump to file
                logger.debug("Last check dump {} milliseconds ago", diffMillis);
                checkDumper.dump(pluginFactory.getPlugins(ActivecheckReporter.class));
                logger.debug("Next check dump in {}s", checkDumpInterval);
            }

            // remove nonexistent collectors
            pluginFactory.removeDeadPlugins();

            // sleep for 500ms
            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException e) {
                logger.error("Failed to sleep thread for 500ms");
                logger.trace(e.getMessage(), e);
            }
        }
    }

    /**
     * The main method, invoked when running from command line.
     *
     * @param args The supplied parameters.
     */
    public static void main(String[] args) {
        final ActivecheckCommandlineParameters params = new ActivecheckCommandlineParameters();
        final JCommander jcmd = new JCommander(params, args);
        jcmd.setProgramName("activecheck");

        if (params.help) {
            jcmd.usage();
            System.exit(NagiosServiceStatus.OK.getStatusCode());
        } else if (params.version) {
            System.out.println(VERSION);
            System.exit(NagiosServiceStatus.OK.getStatusCode());
        } else if (params.configfile == null) {
            System.out.println("Required options not specified!");
            System.exit(NagiosServiceStatus.CRITICAL.getStatusCode());
        }

        new Activecheck(params.configfile).start();
        System.exit(NagiosServiceStatus.OK.getStatusCode());
    }
}
