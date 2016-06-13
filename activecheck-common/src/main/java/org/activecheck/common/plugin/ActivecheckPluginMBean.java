package org.activecheck.common.plugin;

@SuppressWarnings("unused")
public interface ActivecheckPluginMBean {
    // @Description("reload plugin configuration from properties")
    String reloadConfiguration();

    // @Description("last configuration reload time")
    String getConfigurationReloadTime();

    // @Description("Configuration file")
    String getConfigFile();

    // @Description("disable this plugin instance (until next reload)")
    String disable();

    // @Description("indicates if this plugin is enabled")
    boolean isEnabled();

    // @Description("destroy plugin after it has not been used for the defined time in seconds")
    int getDestroyAfterSeconds();
}
