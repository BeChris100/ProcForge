package net.bc100dev.pfc.sh;

public class ShellException extends Exception {

    public ShellException(String message) {
        super(message);
    }

    public ShellException(Throwable cause) {
        super(cause);
    }
}
