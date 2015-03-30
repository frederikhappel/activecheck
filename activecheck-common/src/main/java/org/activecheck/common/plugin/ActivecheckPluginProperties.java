package org.activecheck.common.plugin;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ActivecheckPluginProperties {
	String[] propertiesToMerge();
}
