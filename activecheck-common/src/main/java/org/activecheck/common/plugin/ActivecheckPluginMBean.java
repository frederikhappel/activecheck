package org.activecheck.common.plugin;

public interface ActivecheckPluginMBean {
	// @Description("reload plugin configuration from properties")
	public void reloadConfiguration();

	// @Description("last configuration reload time")
	public String getConfigurationReloadTime();

	// @Description("Configuration file")
	public String getConfigFile();
}
