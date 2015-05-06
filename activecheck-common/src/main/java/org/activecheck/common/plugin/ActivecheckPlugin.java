package org.activecheck.common.plugin;

import java.util.Date;
import java.util.Iterator;
import java.util.Observable;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ActivecheckPlugin extends Observable implements
		ConfigurationListener, ActivecheckPluginMBean {
	private static final Logger logger = LoggerFactory
			.getLogger(ActivecheckPlugin.class);

	// class members
	protected final PropertiesConfiguration properties;
	private String pluginName = null;
	protected long lastReloadTime = System.currentTimeMillis();

	public ActivecheckPlugin(PropertiesConfiguration properties) {
		Validate.notNull(properties);
		this.properties = (PropertiesConfiguration) properties.clone();
		this.properties
				.setReloadingStrategy(new FileChangedReloadingStrategy());
		this.properties.addConfigurationListener(this);
	}

	public final void setProperties(PropertiesConfiguration newProperties) {
		// merge own properties with given properties
		Validate.notNull(newProperties);
		Iterator<String> keys = newProperties.getKeys();
		while (keys.hasNext()) {
			String key = keys.next();
			Object value = newProperties.getProperty(key);
			if (!properties.containsKey(key)) {
				properties.addProperty(key, value);
			} else if (!properties.getString(key).contentEquals(
					newProperties.getString(key))) {
				properties.setProperty(key, value);
			}
		}
		lastReloadTime = System.currentTimeMillis();

		// run trigger
		reloadConfiguration();
	}

	@Override
	public final void configurationChanged(ConfigurationEvent arg0) {
		logger.debug("Received configuration change '" + arg0.getType()
				+ "' for plugin '" + getPluginName() + "' attribute '"
				+ arg0.getPropertyName() + "' => '" + arg0.getPropertyValue()
				+ "'");
		reloadConfiguration();
	}

	public final boolean isEnabled() {
		return properties.getBoolean("enabled", true);
	}

	public final void reloadConfiguration() {
		long currentReloadTime = System.currentTimeMillis();
		logger.debug("Reloading configuration for plugin '" + getPluginName()
				+ "'");
		pluginReload();
		lastReloadTime = currentReloadTime;
	}

	public final String getConfigurationReloadTime() {
		if (lastReloadTime > 0) {
			return new Date(lastReloadTime).toString();
		} else {
			return "NEVER";
		}
	}

	public final String getConfigFile() {
		return properties.getFileName();
	}

	public final String getPluginName() {
		// initialize with a unique name if null
		if (pluginName == null) {
			pluginName = this.getClass().getSimpleName() + "_"
					+ this.hashCode();
		}
		return pluginName;
	}

	protected final void setPluginName(String pluginName) {
		// make sure pluginName cannot change
		if (this.pluginName == null) {
			this.pluginName = pluginName;
		}
	}

	abstract protected void pluginReload();
}
