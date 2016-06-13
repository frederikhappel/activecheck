package org.activecheck.common.plugin;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Iterator;
import java.util.Observable;

public abstract class ActivecheckPlugin extends Observable implements ConfigurationListener, ActivecheckPluginMBean {
    private static final Logger logger = LoggerFactory.getLogger(ActivecheckPlugin.class);
    protected static final int DO_NOT_DESTROY = -1;

    // class members
    protected final PropertiesConfiguration properties;
    private String pluginName = null;
    private final String configFile;
    protected long lastReloadTime = System.currentTimeMillis();
    protected boolean enabled = true;
    protected int destroyAfterSeconds = 300;

    public ActivecheckPlugin(PropertiesConfiguration properties) {
        Validate.notNull(properties);
        this.properties = (PropertiesConfiguration) properties.clone();

        // set members
        enabled = properties.getBoolean("enabled", enabled);
        if (properties.getFile() != null) {
            configFile = properties.getFile().getAbsolutePath();
            // configure reloading strategy for the configuration file
            // TODO: does that work?
            this.properties.setReloadingStrategy(new FileChangedReloadingStrategy());
        } else {
            configFile = null;
        }

        // configure listeners for the configuration
        this.properties.addConfigurationListener(this);
    }

    public final void setProperties(PropertiesConfiguration newProperties) {
        // merge own properties with given properties
        Validate.notNull(newProperties);
        final Iterator<String> keys = newProperties.getKeys();
        while (keys.hasNext()) {
            final String key = keys.next();
            final Object value = newProperties.getProperty(key);
            if (!properties.containsKey(key)) {
                properties.addProperty(key, value);
            } else if (!properties.getString(key).contentEquals(newProperties.getString(key))) {
                properties.setProperty(key, value);
            }
        }
        enabled = properties.getBoolean("enabled", enabled);
        lastReloadTime = System.currentTimeMillis();

        // run trigger
        reloadConfiguration();
    }

    @Override
    public final void configurationChanged(ConfigurationEvent arg0) {
        logger.debug("Received configuration change '{}' for plugin '{}' attribute '{}' => '{}'",
                arg0.getType(), getPluginName(), arg0.getPropertyName(), arg0.getPropertyValue());
        reloadConfiguration();
    }

    @Override
    public final boolean isEnabled() {
        if (destroyAfterSeconds != DO_NOT_DESTROY) {
            return enabled && System.currentTimeMillis() - lastReloadTime <= destroyAfterSeconds * 1000;
        }
        return true;
    }

    @Override
    public String disable() {
        if (this.destroyAfterSeconds != DO_NOT_DESTROY) {
            enabled = false;

            // notify observers
            setChanged();
            notifyObservers();

            return "successfully disabled the plugin";
        } else {
            return "plugin cannot be disabled as it is set to DO_NOT_DESTROY";
        }
    }

    @Override
    public final String reloadConfiguration() {
        final long currentReloadTime = System.currentTimeMillis();
        logger.debug("Reloading configuration for plugin '{}'", getPluginName());
        pluginReload();
        lastReloadTime = currentReloadTime;
        return String.format("Reloaded Configuration on %s", new Date(lastReloadTime));
    }

    @Override
    public final String getConfigurationReloadTime() {
        if (lastReloadTime > 0) {
            return new Date(lastReloadTime).toString();
        } else {
            return "NEVER";
        }
    }

    @Override
    public final String getConfigFile() {
        return configFile;
    }

    public final String getPluginName() {
        // initialize with a unique name if null
        if (pluginName == null) {
            pluginName = String.format("%s_%s", this.getClass().getSimpleName(), this.hashCode());
        }
        return pluginName;
    }

    protected final void setPluginName(String pluginName) {
        // make sure pluginName cannot change once it is set
        if (this.pluginName == null) {
            this.pluginName = pluginName;
        }
    }

    @Override
    public final int getDestroyAfterSeconds() {
        return destroyAfterSeconds;
    }

    public final void setDestroyAfterSeconds(int destroyAfterSeconds) {
        // protect plugins from being removed
        if (this.destroyAfterSeconds != DO_NOT_DESTROY) {
            this.destroyAfterSeconds = destroyAfterSeconds;
        }
    }

    abstract protected void pluginReload();
}
