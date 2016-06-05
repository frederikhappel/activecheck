package org.activecheck.common.plugin.collector;

import org.activecheck.common.plugin.ActivecheckPluginMBean;

public interface ActivecheckCollectorMBean extends ActivecheckPluginMBean {
    // @Description("Connection URL")
    String getCollectorEndpointName();
}
