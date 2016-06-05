package org.activecheck.plugin;

@SuppressWarnings("unused")
public class ActivecheckPluginFactoryException extends Exception {
    private static final long serialVersionUID = 1L;

    public ActivecheckPluginFactoryException(String message) {
        super(message);
    }

    public ActivecheckPluginFactoryException(Throwable cause) {
        super(cause);
    }

    public ActivecheckPluginFactoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
