package org.activecheck.common.plugin;

@SuppressWarnings("unused")
public interface ActivecheckPluginMBean {
    // @Description("reload plugin configuration from properties")
    void reloadConfiguration();

    // @Description("last configuration reload time")
    String getConfigurationReloadTime();

    // @Description("Configuration file")
    String getConfigFile();
}
