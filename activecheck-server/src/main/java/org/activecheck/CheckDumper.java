package org.activecheck;

import org.activecheck.common.Encoding;
import org.activecheck.common.nagios.NagiosServiceReport;
import org.activecheck.common.plugin.reporter.ActivecheckReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * This class is used to dump the results of all reporters to a file
 *
 * @since v1.4.0
 */
public class CheckDumper {
    private static final Logger logger = LoggerFactory.getLogger(CheckDumper.class);

    private String filename = null;
    private Set<String> filters = new HashSet();

    public void setCheckDumpFile(String filename) {
        if (this.filename != null && (filename == null || !filename.equals(this.filename))) {
            final File checkDumpFile = new File(this.filename);
            if (!checkDumpFile.delete()) {
                logger.error("Unable to remove check dump file '{}'", filename);
            }
        }

        if (filename != null && !filename.isEmpty()) {
            this.filename = filename;
        }
    }

    public void setFilter(String[] rawFilters) {
        this.filters.clear();
        for (String rawFilter : rawFilters) {
            rawFilter = rawFilter.toUpperCase();
            try {
                filters.add(rawFilter);
            } catch (IllegalArgumentException e) {
                logger.error("Invalid status filter '{}'", rawFilter);
                logger.trace(e.getMessage(), e);
            }
        }
    }

    public void dump(Collection<ActivecheckReporter> reporters) {
        final File checkDumpFile = new File(filename);
        try (final BufferedWriter checkDumpWriter = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(checkDumpFile.getAbsolutePath()), Encoding.UTF8))) {
            for (ActivecheckReporter reporter : reporters) {
                if (filters.contains(reporter.getOverallServiceStatus())) {
                    for (final NagiosServiceReport report : reporter.getReports()) {
                        checkDumpWriter.write(String.format(
                                "%s: %s (%s)%n",
                                reporter.getOverallServiceName(), reporter.getOverallServiceStatus(), report.getMessage()
                        ));
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Unable to open check dump file '{}': {}", filename, e.getMessage());
            logger.trace(e.getMessage(), e);
        }
    }
}
