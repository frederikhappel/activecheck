package org.activecheck.common.pidfile;

import java.util.HashMap;
import java.util.Map;
import java.util.Observer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PidFileFactory {
	private static final Logger logger = LoggerFactory
			.getLogger(PidFileFactory.class);
	private static final PidFileFactory instance = new PidFileFactory();

	// define class members
	private final Map<String, PidFile> pidFileMap;

	public PidFileFactory() {
		pidFileMap = new HashMap<String, PidFile>();
	}

	public static PidFile create(String pidfilePath, Observer observer) {
		PidFile pidfile = instance.pidFileMap.get(pidfilePath);
		if (pidfile == null || pidfile.countObservers() == 0) {
			logger.info("Instantiating PidFile for '" + pidfilePath + "'");

			// create pidfile watcher
			pidfile = new PidFile();
			pidfile.setFilename(pidfilePath);
			instance.pidFileMap.put(pidfilePath, pidfile);

			// run pidfile watcher
			Thread t = new Thread(pidfile);
			t.setName("PidFile(" + pidfilePath + ")");
			t.start();
		}
		pidfile.addObserver(observer);

		return pidfile;
	}

	public static void delete(String pidfilePath, Observer observer) {
		PidFile pidfile = instance.pidFileMap.get(pidfilePath);
		if (pidfile != null) {
			logger.info("Removing PidFile for '" + pidfilePath + "'");
			pidfile.deleteObserver(observer);
		}
	}
}
