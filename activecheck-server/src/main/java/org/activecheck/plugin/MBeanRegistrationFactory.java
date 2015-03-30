package org.activecheck.plugin;

import java.util.HashMap;
import java.util.Map;

import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.softee.management.helper.MBeanRegistration;

public class MBeanRegistrationFactory {
	private static final Logger logger = LoggerFactory
			.getLogger(MBeanRegistrationFactory.class);
	private static final MBeanRegistrationFactory instance = new MBeanRegistrationFactory();
	private final Map<String, MBeanRegistration> registeredMBeans;

	public MBeanRegistrationFactory() {
		registeredMBeans = new HashMap<String, MBeanRegistration>();
	}

	public static MBeanRegistrationFactory getInstance() {
		return instance;
	}

	public void register(String domain, String name, Object mbean) {
		ObjectName jmxObjectName;
		String jmxName = domain + "," + "name=" + name.replaceAll(":", "_");
		MBeanRegistration registration = registeredMBeans.get(jmxName);
		if (registration == null) {
			try {
				jmxObjectName = new ObjectName(jmxName);
				registration = new MBeanRegistration(mbean, jmxObjectName);
				registeredMBeans.put(jmxName, registration);
				registration.register();
			} catch (Exception e) {
				logger.error("Registering MBean '" + jmxName + "' failed: "
						+ e.getMessage());
				logger.trace(e.getMessage(), e);
			}
		} else {
			logger.warn("MBean already registered '" + jmxName + "'");
		}
	}

	public void unregister(String domain, String name) {
		String jmxName = domain + "," + "name=" + name.replaceAll(":", "_");
		MBeanRegistration registration = registeredMBeans.get(jmxName);
		if (registration != null) {
			try {
				registration.unregister();
				registeredMBeans.remove(jmxName);
			} catch (Exception e) {
				logger.error("Unregistering MBean '" + jmxName + "' failed: "
						+ e.getMessage());
				logger.trace(e.getMessage(), e);
			}
		} else {
			logger.warn("Trying to unregister unknown MBean '" + jmxName + "'");
		}
	}
}
