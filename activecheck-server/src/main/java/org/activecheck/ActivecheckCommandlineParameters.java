package org.activecheck;

import com.beust.jcommander.Parameter;

public class ActivecheckCommandlineParameters {
    @Parameter(names = {"-help", "--help", "-h", "-usage", "--usage"}, description = "print help and exit", help = true)
    public boolean help = false;

    @Parameter(names = {"-version", "--version"}, description = "print version and exit")
    public boolean version = false;

    @Parameter(names = {"-config", "--config", "-c"}, description = "path to configuration file")
    public String configfile = null;
}
