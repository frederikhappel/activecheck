package org.activecheck.plugin.reporter;

public interface ActivecheckReporterExecutorMBean {
    // @Description("Max threads in this pool")
    int getCorePoolSize();

    // @Description("Max threads in this pool")
    void setCorePoolSize(final int size);

    // @Description("Number of active threads")
    int getActiveCount();

    // @Description("Is the thread pool paused?")
    boolean isPaused();

    // @Description("pause running threads")
    String pause();

    // @Description("resume running threads")
    String resume();
}
