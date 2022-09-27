package moe.knox.factorio.core;

public class CoreException extends RuntimeException {
    public CoreException(String message, Throwable e) {
        super(message, e);
    }

    public CoreException(String message) {
        super(message);
    }
}
