package org.activecheck.common.pidfile;

import org.activecheck.common.Encoding;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Observable;
import java.util.concurrent.TimeUnit;

public class PidFile extends Observable implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(PidFile.class);
    private static final int checkIntervalInSeconds = 10;

    private String filename = null;
    private int pid = -1;
    private boolean pidChanged = false;

    private void check() {
        if (filename != null) {
            logger.debug("checking pidfile {}", filename);
            try (final BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(filename), Encoding.UTF8))) {
                final int pidFromFile = Integer.parseInt(reader.readLine());

                setPid(pidFromFile);
            } catch (Exception e) {
                logger.warn("Cannot read/parse pidfile '{}': " + filename, e.getMessage());
                setPid(-1);
            }
        }
    }

    @Override
    public void run() {
        while (countObservers() > 0) {
            check();
            try {
                TimeUnit.SECONDS.sleep(checkIntervalInSeconds);
            } catch (InterruptedException e) {
                logger.error("Failed to sleep thread for {}s", checkIntervalInSeconds);
                logger.trace(e.getMessage(), e);
            }
        }
    }

    private void setPid(int newPid) {
        pidChanged = newPid != pid;
        if (pidChanged) {
            logger.info("PID changed from {} to {}", pid, newPid);
            pid = newPid;
            setChanged();
            notifyObservers(pidChanged);
        } else {
            logger.debug("PID ({}) did not change", pid);
        }
    }

    public boolean hasPidChanged() {
        return pidChanged;
    }

    public boolean isConfigured() {
        return filename != null;
    }

    public void setFilename(String filename) {
        Validate.notNull(filename, "filename cannot be NULL");
        this.filename = filename;
        check();
    }

    public int getPid() {
        return pid;
    }

    public String getFilename() {
        return filename;
    }
}
