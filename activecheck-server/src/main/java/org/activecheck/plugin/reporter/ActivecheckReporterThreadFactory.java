package org.activecheck.plugin.reporter;

import java.util.concurrent.ThreadFactory;

public class ActivecheckReporterThreadFactory implements ThreadFactory {
    @Override
    public Thread newThread(Runnable arg0) {
        // Validate.isTrue(arg0 instanceof NagiosReporter);
        final Thread t = new Thread(arg0);
        t.setName("NagiosReporter");
        return t;
    }
}
