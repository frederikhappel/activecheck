package org.activecheck.common.plugin;

import java.util.Iterator;
import java.util.Observable;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.softee.management.annotation.Description;
import org.softee.management.annotation.MBean;
import org.softee.management.annotation.ManagedOperation;

@MBean
@Description("ActivecheckPlugin MBean")
public abstract class ActivecheckPlugin extends Observable implements
		ConfigurationListener {
	private static final Logger logger = LoggerFactory.getLogger(ActivecheckPlugin.class);

	// class members
	protected final PropertiesConfiguration properties;
	protected long lastReloadTime = System.currentTimeMillis();

	public ActivecheckPlugin(PropertiesConfiguration properties) {
		Validate.notNull(properties);
		this.properties = (PropertiesConfiguration) properties.clone();
		this.properties.setReloadingStrategy(new FileChangedReloadingStrategy());
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
			} else if (!properties.getString(key).contentEquals(newProperties.getString(key))) {
				properties.setProperty(key, value);
			}
		}
		lastReloadTime = System.currentTimeMillis();

		// run trigger
		reloadConfiguration();
	}

	@Override
	public final void configurationChanged(ConfigurationEvent arg0) {
		logger.debug("Received configuration change '" + arg0.getType() + "' for plugin '" + getPluginName() + "' attribute '" + arg0.getPropertyName() + "' => '" + arg0.getPropertyValue() + "'");
		reloadConfiguration();
	}

	public final boolean isEnabled() {
		return properties.getBoolean("enabled", true);
	}

	@ManagedOperation
	@Description("reload plugin configuration from properties")
	public final void reloadConfiguration() {
		long currentReloadTime = System.currentTimeMillis();
		logger.debug("Reloading configuration for plugin '" + getPluginName() + "'");
		pluginReload();
		lastReloadTime = currentReloadTime;
	}

	abstract protected void pluginReload();

	abstract public String getPluginName();
}
