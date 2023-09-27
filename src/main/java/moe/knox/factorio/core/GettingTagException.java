package moe.knox.factorio.core;

public class GettingTagException extends Exception {
    public GettingTagException() {
        super();
    }

    public GettingTagException(Throwable cause) {
        super(cause.getMessage(), cause);
    }
}
