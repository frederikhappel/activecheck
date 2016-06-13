package org.activecheck;

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

public class MBeanRegistry {
    private static final Logger logger = LoggerFactory.getLogger(MBeanRegistry.class);
    private static final String JMX_OBJECT_PREFIX = "org.activecheck";

    private static final MBeanRegistry instance = new MBeanRegistry();
    private final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

    public static MBeanRegistry getInstance() {
        return instance;
    }

    private String generateJmxPath(String type, String name) {
        final StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s:type=%s", JMX_OBJECT_PREFIX, type));
        if (name != null) {
            sb.append(",name=").append(name.replaceAll(":", "_"));
        }
        return sb.toString();
    }

    public void register(String type, String name, Object mbean) {
        final String jmxName = generateJmxPath(type, name);
        try {
            final ObjectName jmxObjectName = new ObjectName(jmxName);
            try {
                mbs.getObjectInstance(jmxObjectName);
                logger.debug("MBean already registered '{}'", jmxName);
            } catch (InstanceNotFoundException e1) {
                mbs.registerMBean(mbean, jmxObjectName);
            }
        } catch (InstanceAlreadyExistsException | MBeanRegistrationException
                | NotCompliantMBeanException | MalformedObjectNameException e) {
            logger.error("Registering MBean '{}' failed: {}", jmxName, e.getMessage());
            logger.trace(e.getMessage(), e);
        }
    }

    public void unregister(String type, String name) {
        final String jmxName = generateJmxPath(type, name);
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
