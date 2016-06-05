package org.activecheck.common.plugin.reporter;

public class ActivecheckReporterException extends Exception {
    private static final long serialVersionUID = -4531839679260892507L;

    @SuppressWarnings("unused")
    public ActivecheckReporterException(String message) {
        super(message);
    }

    @SuppressWarnings("unused")
    public ActivecheckReporterException(Throwable cause) {
        super(cause);
    }

    @SuppressWarnings("unused")
    public ActivecheckReporterException(String message, Throwable cause) {
        super(message, cause);
    }
}
