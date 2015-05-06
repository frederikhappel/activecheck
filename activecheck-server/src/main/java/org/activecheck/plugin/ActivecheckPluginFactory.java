package org.activecheck.plugin;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;

import org.activecheck.ActivecheckConfiguration;
import org.activecheck.common.plugin.ActivecheckPlugin;
import org.activecheck.common.plugin.ActivecheckPluginProperties;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActivecheckPluginFactory {
	private static final Logger logger = LoggerFactory
			.getLogger(ActivecheckPluginFactory.class);

	private static final String JMX_OBJECT_DOMAIN = "org.activecheck:type=Plugins";

	private final Map<String, ActivecheckPlugin> activecheckPlugins;
	private URLClassLoader urlClassLoader = null;

	public ActivecheckPluginFactory() {
		activecheckPlugins = new ConcurrentHashMap<String, ActivecheckPlugin>();
	}

	public void setPluginDir(String rawPluginDir) {
		List<URL> pluginURLs = new ArrayList<URL>();

		logger.debug("Loading plugins from " + rawPluginDir);
		File pluginDir = new File(rawPluginDir);
		File[] pluginFiles = pluginDir.listFiles(); // TODO: add jar filter
		if (!pluginDir.isDirectory()) {
			logger.warn("Plugin directory '" + pluginDir + "' does not exist");
		} else if (pluginFiles.length <= 0) {
			logger.info("Plugin directory '" + pluginDir + "' is empty");
		} else {
			for (File pluginFile : pluginFiles) {
				try {
					pluginURLs.add(pluginFile.toURI().toURL());
				} catch (MalformedURLException e) {
					logger.error("Cannot add plugin'" + pluginFile + "'");
				}
			}
		}

		// create plugin class loader
		ClassLoader currentThreadClassLoader = Thread.currentThread()
				.getContextClassLoader();
		urlClassLoader = URLClassLoader.newInstance(
				pluginURLs.toArray(new URL[] {}), currentThreadClassLoader);
	}

	@SuppressWarnings("unchecked")
	public ActivecheckPlugin createPlugin(File configFile,
			ActivecheckConfiguration configuration, Observer observer)
			throws ActivecheckPluginFactoryException, ConfigurationException {
		PropertiesConfiguration pluginProperties = new PropertiesConfiguration(
				configFile);
		pluginProperties.setFile(configFile);

		// create plugin class loader
		if (urlClassLoader == null) {
			setPluginDir(configuration.getPluginDir());
		}

		String classname = pluginProperties.getString("class", null);
		ActivecheckPlugin activecheckPlugin = activecheckPlugins.get(configFile
				.getPath());

		// determine plugin class name
		Class<? extends ActivecheckPlugin> pluginClass;
		if (activecheckPlugin != null) {
			pluginClass = activecheckPlugin.getClass();
		} else {
			try {
				pluginClass = (Class<? extends ActivecheckPlugin>) urlClassLoader
						.loadClass(classname);
			} catch (ClassNotFoundException e) {
				throw new ActivecheckPluginFactoryException(
						"Unknown plugin class '" + classname + "'");
			}
		}

		// merge properties
		ActivecheckPluginProperties activecheckPluginProperties = pluginClass
				.getAnnotation(ActivecheckPluginProperties.class);
		if (activecheckPluginProperties != null) {
			pluginProperties = configuration.mergeWith(pluginProperties, Arrays
					.asList(activecheckPluginProperties.propertiesToMerge()));
		} else {
			logger.error("ActivecheckPluginProperties annotation missing for "
					+ pluginClass);
		}

		// set properties
		if (activecheckPlugin != null) {
			activecheckPlugin.setProperties(pluginProperties);
		} else {
			try {
				Constructor<? extends ActivecheckPlugin> constructor = pluginClass
						.getConstructor(new Class[] { PropertiesConfiguration.class });
				activecheckPlugin = constructor.newInstance(pluginProperties);
			} catch (NoSuchMethodException | SecurityException
					| InstantiationException | IllegalAccessException
					| IllegalArgumentException e) {
				logger.error("Unable to instantiate plugin for class '"
						+ classname + "'");
				throw new ActivecheckPluginFactoryException(e);
			} catch (InvocationTargetException e) {
				logger.error("Unable to instantiate plugin for class '"
						+ classname + "'");
				throw new ActivecheckPluginFactoryException(e.getCause());
			}
			activecheckPlugins.put(configFile.getPath(), activecheckPlugin);
			activecheckPlugin.addObserver(observer);

			// add to jmx
			String pluginName = activecheckPlugin.getPluginName();
			MBeanRegistrationFactory.getInstance().register(JMX_OBJECT_DOMAIN,
					pluginName, activecheckPlugin);
		}

		// remove disabled plugin
		if (!activecheckPlugin.isEnabled()) {
			String pluginName = activecheckPlugin.getPluginName();

			logger.info("Removing plugin '" + pluginName + "'");
			activecheckPlugin.deleteObservers();
			activecheckPlugins.remove(configFile.getPath());

			// remove from jmx
			MBeanRegistrationFactory.getInstance().unregister(
					JMX_OBJECT_DOMAIN, pluginName);
		}

		return activecheckPlugin;
	}

	public int getPluginCount() {
		return activecheckPlugins.size();
	}
}
