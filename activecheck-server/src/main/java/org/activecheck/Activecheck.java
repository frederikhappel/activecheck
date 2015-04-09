package org.activecheck;

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
import java.util.Observable;
import java.util.Observer;

import org.activecheck.common.Encoding;
import org.activecheck.common.nagios.NagiosServiceReport;
import org.activecheck.common.nagios.NagiosServiceStatus;
import org.activecheck.common.net.ActivecheckServer;
import org.activecheck.common.net.TcpActivecheckServer;
import org.activecheck.common.plugin.ActivecheckPlugin;
import org.activecheck.common.plugin.collector.ActivecheckCollector;
import org.activecheck.common.plugin.reporter.ActivecheckReporter;
import org.activecheck.common.plugin.reporter.ActivecheckReporterStatus;
import org.activecheck.plugin.ActivecheckPluginFactory;
import org.activecheck.plugin.collector.ActivecheckPacketProcessor;
import org.activecheck.plugin.collector.StdoutHost;
import org.activecheck.plugin.reporter.ActivecheckReporterScheduler;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;

/**
 * 
 * Activecheck is used to submit passive checks to NSCA services. While it is
 * intended to run as a daemon it can be used command line program as well.
 * 
 * @author Frederik Happel mail@frederikhappel.de
 */
public class Activecheck implements Observer {
	private static final Logger logger = LoggerFactory.getLogger(Activecheck.class);
	private static final String VERSION = Activecheck.class.getPackage().getImplementationVersion();

	private String selfJarChecksum = null;
	private boolean killOnChecksumMismatch = true;
	private int reloadInterval = 3600;
	private int hostCheckInterval = 60;

	private ActivecheckConfiguration configuration;
	private final ActivecheckPluginFactory pluginFactory;
	private final ActivecheckReporterScheduler reporterScheduler;
	private final ActivecheckPacketProcessor activecheckPacketProcessor;

	private String localFqdn = "localhost";

	public Activecheck(String cfgfile) {
		// determine local host name
		try {
			localFqdn = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			logger.error("Unable to determine own hostname: " + e.getMessage());
			logger.trace(e.getMessage(), e);
		}

		// get properties from configuration file
		try {
			configuration = new ActivecheckConfiguration(cfgfile);
		} catch (ConfigurationException e) {
			String errorMessage = "Error creating configuration object '" + cfgfile + "': " + e.getMessage();
			logger.error(errorMessage);
			logger.trace(e.getMessage(), e);
			System.exit(1);
		}

		// initialize members
		pluginFactory = new ActivecheckPluginFactory();
		reporterScheduler = new ActivecheckReporterScheduler();
		reporterScheduler.setupExecutorService(configuration.getWorker());
		activecheckPacketProcessor = new ActivecheckPacketProcessor();

		// load configuration
		reloadConfiguration();
	}

	private void calculateSelfMD5Hash() {
		try {
			InputStream is = new FileInputStream(
					new File(
							Activecheck.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
			String md5 = DigestUtils.md5Hex(is);
			if (selfJarChecksum == null) {
				selfJarChecksum = md5;
			} else if (!md5.equals(selfJarChecksum)) {
				String exitMessage = "Checksum of running JAR does not match checksum of available JAR. Stopping ActiveCheck";
				logger.info(exitMessage);
				System.exit(0);
			}
		} catch (IOException | URISyntaxException e) {
			// to avoid that this exception occurs again, set
			// killOnChecksumMismatch = false
			killOnChecksumMismatch = false;
			logger.warn("Cannot kill self on checksum mismatch: " + e.getMessage());
			logger.trace(e.getMessage(), e);
		}
	}

	private void reloadConfiguration() {
		logger.info("Reloading configuration");

		// update plugin path
		pluginFactory.setPluginDir(configuration.getPluginDir());

		// update worker pool size
		reporterScheduler.setupExecutorService(configuration.getWorker());

		String pidfile = configuration.getPidFile();
		if (pidfile != null) {
			killOnChecksumMismatch = configuration.monitorJarFile();
			if (killOnChecksumMismatch) {
				calculateSelfMD5Hash();
			} else {
				logger.debug("Monitoring of JAR file is disabled");
			}

			String[] pid_parts = ManagementFactory.getRuntimeMXBean().getName().split("@");
			BufferedWriter writer = null;
			try {
				writer = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(pidfile), Encoding.UTF8));
				writer.write(pid_parts[0]);
				writer.close();
			} catch (IOException e) {
				String errorMessage = "Unable to write pidfile '" + pidfile + "': " + e.getMessage();
				logger.error(errorMessage);
				logger.trace(e.getMessage(), e);
				System.exit(1);
			}
		}
		reloadInterval = configuration.getConfigurationReloadInterval();
		hostCheckInterval = configuration.getHostCheckInterval();

		// log to console
		if (configuration.logToConsole()) {
			activecheckPacketProcessor.addOrUpdateCollector(new StdoutHost());
		}
		activecheckPacketProcessor.removeNonexistentHosts();

		// loop over include files
		for (File configFile : configuration.getPluginConfigurationFiles()) {
			logger.debug("Including configuration in file " + configFile);
			try {
				// load properties from file
				ActivecheckPlugin activecheckPlugin = pluginFactory.createPlugin(configFile,
						configuration,
						this);

				// determine if collector or reporter
				if (activecheckPlugin instanceof ActivecheckReporter) {
					// update or create NagiosReporter and thread
					reporterScheduler.addOrUpdateReporter((ActivecheckReporter) activecheckPlugin,
							reloadInterval);
				} else if (activecheckPlugin instanceof ActivecheckCollector) {
					activecheckPacketProcessor.addOrUpdateCollector((ActivecheckCollector) activecheckPlugin);
				} else {
					logger.error("unknown plugin type '" + activecheckPlugin.getClass() + "'");
				}
			} catch (Exception e) {
				logger.error("Cannot create plugin defined in '" + configFile + "': " + e.getMessage());
				logger.debug(e.getMessage(), e);
			}
		}
	}

	@Override
	public void update(Observable arg0, Object arg1) {
		logger.debug("Received update from " + arg0.getClass());
		if (arg0 instanceof ActivecheckReporter) {
			ActivecheckReporter nagiosReporter = (ActivecheckReporter) arg0;
			long startTime = nagiosReporter.getLastRunTimeMillis();
			long finishTime = startTime + nagiosReporter.getExecutionTimeMillis();

			for (NagiosServiceReport report : nagiosReporter.getReports()) {
				// send report to configured hosts
				report.setServiceHost(localFqdn);
				report.setStartTime(startTime);
				report.setFinishTime(finishTime);
				activecheckPacketProcessor.process(report);
			}
			if (nagiosReporter.getStatus() == ActivecheckReporterStatus.REQUESTSHUTDOWN) {
				String errorMessage = "Shutdown due to request from " + nagiosReporter.getOverallServiceName();
				logger.error(errorMessage);
				System.exit(1);
			}
			reporterScheduler.reschedule(nagiosReporter);
		} else if (arg0 instanceof ActivecheckServer) {
			// send received packet to configured hosts
			NagiosServiceReport report = (NagiosServiceReport) arg1;
			activecheckPacketProcessor.process(report);
		}
	}

	public void start() {
		if (configuration.isNscaProxy()) {
			// create listening socket
			InetAddress bindAddress = InetAddress.getLoopbackAddress();
			int bindPort = configuration.getBindPort();
			ActivecheckServer activecheckServer = null;
			try {
				activecheckServer = new TcpActivecheckServer(bindAddress,
						bindPort);
				activecheckServer.addObserver(this);
			} catch (IOException e) {
				String errorMessage = "Unable to create a server socket at 127.0.0.1:" + bindPort;
				logger.error(errorMessage);
				logger.trace(e.getMessage(), e);
				System.exit(1);
			}
			Thread t = new Thread(activecheckServer);
			t.setName("ActivecheckServer");
			t.start();
		}
		long lastReloadMillis = System.currentTimeMillis();
		long lastHostCheckMillis = 0;
		while (true) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				logger.error("Failed to sleep thread for 500ms");
				logger.trace(e.getMessage(), e);
			}
			long time = System.currentTimeMillis();

			// reload configuration?
			long diffMillis = time - lastReloadMillis;
			if (reloadInterval * 1000 - diffMillis <= 1000) {
				lastReloadMillis = time;
				logger.debug("Last reload " + diffMillis + " milliseconds ago");
				reloadConfiguration();
				logger.debug("Next reload in " + reloadInterval + "s");
			}

			// submit a host check result?
			diffMillis = time - lastHostCheckMillis;
			if (hostCheckInterval * 1000 - diffMillis <= 1000) {
				lastHostCheckMillis = time;
				// send host up notification
				logger.debug("Last host check " + diffMillis + " milliseconds ago");
				long currentTime = System.currentTimeMillis();
				long uptime = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
				String message = "Uptime " + uptime + " seconds\nActivecheck version " + VERSION;
				NagiosServiceReport report = new NagiosServiceReport(null,
						localFqdn, NagiosServiceStatus.OK, message,
						currentTime, currentTime);
				activecheckPacketProcessor.process(report);
				logger.debug("Next host check in " + hostCheckInterval + "s");
			}
		}
	}

	/**
	 * The main method, invoked when running from command line.
	 *
	 * @param args
	 *            The supplied parameters.
	 */
	public static void main(String[] args) {
		ActivecheckCommandlineParameters params = new ActivecheckCommandlineParameters();
		JCommander jcmd = new JCommander(params, args);
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
