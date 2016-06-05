package org.activecheck.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

public class MBeanRegistrationFactory {
    private static final Logger logger = LoggerFactory.getLogger(MBeanRegistrationFactory.class);
    private static final MBeanRegistrationFactory instance = new MBeanRegistrationFactory();
    private final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

    public static MBeanRegistrationFactory getInstance() {
        return instance;
    }

    public void register(String domain, String name, Object mbean) {
        final String jmxName = String.format("%s,name=%s", domain, name.replaceAll(":", "_"));
        try {
            final ObjectName jmxObjectName = new ObjectName(jmxName);
            try {
                mbs.getObjectInstance(jmxObjectName);
                logger.warn("MBean already registered '{}'", jmxName);
            } catch (InstanceNotFoundException e1) {
                mbs.registerMBean(mbean, jmxObjectName);
            }
        } catch (InstanceAlreadyExistsException | MBeanRegistrationException
                | NotCompliantMBeanException | MalformedObjectNameException e) {
            logger.error("Registering MBean '{}' failed: {}", jmxName, e.getMessage());
            logger.trace(e.getMessage(), e);
        }
    }

    public void unregister(String domain, String name) {
        final String jmxName = String.format("%s,name=%s", domain, name.replaceAll(":", "_"));
        try {
            final ObjectName jmxObjectName = new ObjectName(jmxName);
            try {
                mbs.unregisterMBean(jmxObjectName);
            } catch (InstanceNotFoundException e1) {
                logger.warn("Trying to unregister unknown MBean '{}'", jmxName);
            }
        } catch (MBeanRegistrationException | MalformedObjectNameException e) {
            logger.error("Unregistering MBean '{}' failed: {}", jmxName, e.getMessage());
            logger.trace(e.getMessage(), e);
        }
    }
}
