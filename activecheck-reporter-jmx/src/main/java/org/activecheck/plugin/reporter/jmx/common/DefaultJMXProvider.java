package org.activecheck.plugin.reporter.jmx.common;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import java.io.IOException;
import java.util.Map;

/**
 * The default implementation of a JMXProvider based on the static factory
 * method provided by the Java platform.
 */
public class DefaultJMXProvider implements JMXProvider {
	public JMXConnector getConnector(JMXServiceURL url, Map<String, ?> env)
			throws IOException {
		return JMXConnectorFactory.connect(url, env);
	}
}
