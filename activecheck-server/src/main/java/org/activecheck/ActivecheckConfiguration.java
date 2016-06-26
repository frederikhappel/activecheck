package org.activecheck;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ActivecheckConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(ActivecheckConfiguration.class);

    // define defaults
    private static final int DEFAULT_ACTIVECHECK_BIND_PORT = 5623;
    private static final int DEFAULT_WORKER = 1;
    private static final boolean DEFAULT_NSCA_PROXY = false;
    private static final int DEFAULT_CONFIGURATION_RELOAD_INTERVAL = 60;
    private static final int DEFAULT_HOSTCHECK_INTERVAL = 10;
    private static final int DEFAULT_CHECKDUMP_INTERVAL = 10;
    private static final boolean DEFAULT_CONSOLE_LOG = false;

    // define class members
    private final PropertiesConfiguration properties;
    private final String propertiesDir;
    private final List<String> propertiesToMerge = new ArrayList<>(Arrays.asList(
            "check_interval",
            "reload_interval",
            "retry_interval",
            "graph_perfdata",
            "graph_results",
            "report_results"
    ));

    public ActivecheckConfiguration(String configFile) throws ConfigurationException {
        propertiesDir = new File(configFile).getAbsoluteFile().getParent();
        properties = new PropertiesConfiguration(configFile);
        properties.setReloadingStrategy(new FileChangedReloadingStrategy());
    }

    public PropertiesConfiguration mergeWith(
            PropertiesConfiguration childProperties,
            List<String> additionalPropertiesToMerge) {
        final Iterator<String> keys = properties.getKeys();
        while (keys.hasNext()) {
            final String key = keys.next();
            if ((propertiesToMerge.contains(key) || additionalPropertiesToMerge.contains(key))
                    && !childProperties.containsKey(key)) {
                childProperties.addProperty(key, properties.getProperty(key));
            }
        }

        return childProperties;
    }

    public int getBindPort() {
        return properties.getInt("bindPort", DEFAULT_ACTIVECHECK_BIND_PORT);
    }

    public boolean isNscaProxy() {
        return properties.getBoolean("nsca_proxy", DEFAULT_NSCA_PROXY);
    }

    public boolean logToConsole() {
        return properties.getBoolean("console_log", DEFAULT_CONSOLE_LOG);
    }

    public int getConfigurationReloadInterval() {
        return properties.getInt("reload_interval", DEFAULT_CONFIGURATION_RELOAD_INTERVAL);
    }

    public String getPidFile() {
        return properties.getString("pidfile", null);
    }

    public boolean monitorJarFile() {
        return properties.getBoolean("monitorjar", true);
    }

    public List<File> getPluginConfigurationFiles() {
        final List<Object> list = properties.getList("includedir", null);
        final List<File> fileList = new ArrayList<>();
        if (list != null) {
            for (Object item : list) {
                final String rawPluginDir = (String) item;
                logger.debug("Including path {}", rawPluginDir);
                File pluginDir = new File(rawPluginDir);
                if (!pluginDir.isAbsolute()) {
                    pluginDir = new File(propertiesDir + File.separator + rawPluginDir);
                }
                if (!pluginDir.isDirectory()) {
                    logger.warn("Plugin directory '{}' does not exist", pluginDir);
                } else {
                    final File[] configFiles = pluginDir.listFiles();
                    if (configFiles == null || configFiles.length <= 0) {
                        logger.warn("Plugin directory '{}' is empty", pluginDir);
                    } else {
                        Collections.addAll(fileList, configFiles);
                    }
                }
            }
        }
        return fileList;
    }

    public String getPluginDir() {
        final File pluginDir = new File(properties.getString("plugindir", "plugins"));
        return pluginDir.getAbsolutePath();
    }

    public int getWorker() {
        return properties.getInt("worker", DEFAULT_WORKER);
    }

    public int getHostCheckInterval() {
        return properties.getInt("hostcheck_interval", DEFAULT_HOSTCHECK_INTERVAL);
    }

    public int getCheckDumpInterval() {
        return properties.getInt("checkdump_interval", DEFAULT_CHECKDUMP_INTERVAL);
    }

    public String getCheckDumpFile() {
        return properties.getString("checkdump_file", null);
    }

    public String[] getCheckDumpFilters() {
        return properties.getStringArray("checkdump_filters");
    }
}
