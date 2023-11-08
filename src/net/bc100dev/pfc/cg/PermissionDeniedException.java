package net.bc100dev.pfc.cg;

import java.io.IOException;

public class PermissionDeniedException extends IOException {

    public PermissionDeniedException(String message) {
        super(message);
    }

}
