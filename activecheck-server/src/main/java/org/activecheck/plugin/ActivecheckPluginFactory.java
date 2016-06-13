package org.activecheck.plugin;

import org.activecheck.ActivecheckConfiguration;
import org.activecheck.common.plugin.ActivecheckPlugin;
import org.activecheck.common.plugin.ActivecheckPluginProperties;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ActivecheckPluginFactory {
    private static final Logger logger = LoggerFactory.getLogger(ActivecheckPluginFactory.class);

    private final Map<String, ActivecheckPlugin> activecheckPlugins = new ConcurrentHashMap<>();
    private URLClassLoader urlClassLoader = null;

    public void setPluginDir(String rawPluginDir) {
        final List<URL> pluginURLs = new ArrayList<>();

        logger.debug("Loading plugins from {}", rawPluginDir);
        final File pluginDir = new File(rawPluginDir);
        final File[] pluginFiles = pluginDir.listFiles(); // TODO: add jar filter
        if (!pluginDir.isDirectory()) {
            logger.warn("Plugin directory '{}' does not exist", pluginDir);
        } else if (pluginFiles == null || pluginFiles.length <= 0) {
            logger.info("Plugin directory '{}' is empty", pluginDir);
        } else {
            for (final File pluginFile : pluginFiles) {
                try {
                    pluginURLs.add(pluginFile.toURI().toURL());
                } catch (MalformedURLException e) {
                    logger.error("Cannot add plugin '{}'", pluginFile);
                }
            }
        }

        // create plugin class loader
        final ClassLoader currentThreadClassLoader = Thread.currentThread().getContextClassLoader();
        urlClassLoader = URLClassLoader.newInstance(
                pluginURLs.toArray(new URL[pluginURLs.size()]),
                currentThreadClassLoader
        );
    }

    @SuppressWarnings("unchecked")
    public ActivecheckPlugin createPlugin(File configFile, ActivecheckConfiguration configuration, int reloadInterval)
            throws ActivecheckPluginFactoryException, ConfigurationException {
        PropertiesConfiguration pluginProperties = new PropertiesConfiguration(configFile);
        pluginProperties.setFile(configFile);
        final String pluginKey = configFile.getAbsolutePath();

        // create plugin class loader
        if (urlClassLoader == null) {
            setPluginDir(configuration.getPluginDir());
        }

        final String classname = pluginProperties.getString("class", null);
        ActivecheckPlugin plugin = activecheckPlugins.get(pluginKey);

        // determine plugin class name
        final Class<? extends ActivecheckPlugin> pluginClass;
        if (plugin != null) {
            pluginClass = plugin.getClass();
        } else {
            try {
                pluginClass = (Class<? extends ActivecheckPlugin>) urlClassLoader.loadClass(classname);
            } catch (ClassNotFoundException e) {
                throw new ActivecheckPluginFactoryException("Unknown plugin class '" + classname + "'");
            }
        }

        // merge properties
        final ActivecheckPluginProperties activecheckPluginProperties = pluginClass
                .getAnnotation(ActivecheckPluginProperties.class);
        if (activecheckPluginProperties != null) {
            pluginProperties = configuration.mergeWith(
                    pluginProperties, Arrays.asList(activecheckPluginProperties.propertiesToMerge())
            );
        } else {
            logger.error("ActivecheckPluginProperties annotation missing for {}", pluginClass);
        }

        // set properties
        if (plugin != null) {
            plugin.setProperties(pluginProperties);
        } else {
            try {
                final Constructor<? extends ActivecheckPlugin> constructor = pluginClass
                        .getConstructor(PropertiesConfiguration.class);
                plugin = constructor.newInstance(pluginProperties);
            } catch (NoSuchMethodException | SecurityException
                    | InstantiationException | IllegalAccessException
                    | IllegalArgumentException e) {
                logger.error("Unable to instantiate plugin for class '{}'", classname);
                throw new ActivecheckPluginFactoryException(e);
            } catch (InvocationTargetException e) {
                logger.error("Unable to instantiate plugin for class '{}'", classname);
                throw new ActivecheckPluginFactoryException(e.getCause());
            }
            activecheckPlugins.put(plugin.getConfigFile(), plugin);
        }

        // set time when to consider a reporter as dead
        plugin.setDestroyAfterSeconds(reloadInterval);

        // remove disabled plugin
        if (!plugin.isEnabled()) {
            removePlugin(plugin);
        }


        return plugin;
    }

    @SuppressWarnings("unchecked")
    public <T extends ActivecheckPlugin> Collection<T> getPlugins(final Class<T> cls) {
        return (Collection<T>) activecheckPlugins.values().stream()
                .filter(plugin -> plugin != null && cls.isInstance(plugin))
                .collect(Collectors.toSet());
    }

    private void removePlugin(ActivecheckPlugin plugin) {
        final String pluginName = plugin.getPluginName();
        logger.debug("Removing plugin {}", pluginName);

        // delete all references
        plugin.deleteObservers();
        activecheckPlugins.remove(plugin.getConfigFile());
    }

    public void removeDeadPlugins() {
        for (final ActivecheckPlugin plugin : activecheckPlugins.values()) {
            if (!plugin.isEnabled()) {
                removePlugin(plugin);
            }
        }
    }
}
