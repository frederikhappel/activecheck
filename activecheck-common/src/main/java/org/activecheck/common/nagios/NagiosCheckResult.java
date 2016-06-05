package org.activecheck.common.nagios;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NagiosCheckResult implements Serializable {
    private static final long serialVersionUID = 7642381902579833708L;
    private static final Logger logger = LoggerFactory.getLogger(NagiosCheckResult.class);

    private NagiosServiceStatus status = NagiosServiceStatus.OK;
    private final List<String> messages = new ArrayList<>();
    private final List<NagiosPerformanceData> perfDataList = new ArrayList<>();
    private String concatenator = "\n";
    private Map<String, String> perfDataReplacements = new HashMap<>();

    public static NagiosCheckResult fromMessage(String message) {
        NagiosCheckResult checkResult = new NagiosCheckResult();
        checkResult.parseMessage(message);
        return checkResult;
    }

    public final void clear(NagiosServiceStatus status) {
        this.status = status;
        messages.clear();
        perfDataList.clear();
    }

    public final void merge(NagiosCheckResult checkResult) {
        setStatusMoreSevere(checkResult.getStatus());
        messages.addAll(checkResult.getMessages());
        perfDataList.addAll(checkResult.getPerfData());
    }

    public final void parseMessage(String message) {
        parseMessage(message, status);
    }

    public final void addPerfDataReplacement(String search, String replace) {
        perfDataReplacements.put(search, replace);
    }

    public final void parseMessage(String message, NagiosServiceStatus status) {
        if (message == null || message.isEmpty()) {
            logger.debug("Message is empty");
        } else {
            clear(status);
            // parse message see
            // http://nagios.sourceforge.net/docs/3_0/pluginapi.html
            String rawParts[] = message.split("\\|");
            List<String> perfDataLines = new ArrayList<>();
            addMessage(rawParts[0]);
            if (rawParts.length < 2) {
                logger.debug("No performance data found: '{}'", message);
            } else if (rawParts.length == 2) {
                Collections.addAll(perfDataLines, rawParts[1].split("\\r?\\n"));
            } else if (rawParts.length > 2) {
                String[] rawPartsInner = rawParts[1].split("\\r?\\n");
                perfDataLines.add(rawPartsInner[0]);
                for (int i = 1; i < rawPartsInner.length; i++) {
                    addMessage(rawPartsInner[i]);
                }
                Collections.addAll(perfDataLines, rawParts[2].split("\\r?\\n"));
            }
            for (String line : perfDataLines) {
                for (String perfDataRaw : line.trim().split("\\s")) {
                    try {
                        NagiosPerformanceData perfData = new NagiosPerformanceData(
                                perfDataRaw);
                        perfData.replace(perfDataReplacements);
                        perfDataList.add(perfData);
                    } catch (NagiosPerformanceDataException e) {
                        String errorMessage = e.getMessage()
                                + " original message: '" + message + "'";
                        logger.info(errorMessage);
                        logger.trace(errorMessage, e);
                    }
                }
            }
        }
    }

    public final void setConcatenator(String concatenator) {
        this.concatenator = concatenator;
    }

    public final List<String> getMessages() {
        return messages;
    }

    public final String getMessage(String concatenator) {
        return StringUtils.join(messages, concatenator).replace("|", " or ");
    }

    public final String getMessage() {
        return getMessage(concatenator);
    }

    public final String getMessageWithPerformancedata() {
        if (perfDataList.size() > 0) {
            return this.getMessage() + " | "
                    + StringUtils.join(perfDataList, " ");
        } else {
            return this.getMessage();
        }
    }

    public final void addMessage(String message) {
        message = message.trim();
        messages.add(message);
        logger.debug(message);
    }

    public final void setMessage(String message) {
        clear(status);
        messages.add(message);
    }

    public final List<NagiosPerformanceData> getPerfData() {
        return perfDataList;
    }

    public final void addPerformanceData(NagiosPerformanceData perfData) {
        try {
            perfData.replace(perfDataReplacements);
            perfDataList.add(perfData);
        } catch (NagiosPerformanceDataException e) {
            logger.error(e.getMessage());
            logger.trace(e.getMessage(), e);
        }
    }

    public final void addPerformanceData(
            Collection<NagiosPerformanceData> perfDataList) {
        for (NagiosPerformanceData perfData : perfDataList) {
            addPerformanceData(perfData);
        }
    }

    public final NagiosServiceStatus getStatus() {
        return status;
    }

    public final void setStatus(NagiosServiceStatus status) {
        this.status = status;
    }

    public final void setStatusMoreSevere(NagiosServiceStatus status) {
        this.status = status.moreSevere(this.status);
    }
}
