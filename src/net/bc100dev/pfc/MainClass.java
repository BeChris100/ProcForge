package net.bc100dev.pfc;

import net.bc100dev.pfc.sh.Shell;
import net.bc100dev.pfc.sh.ShellException;

import java.io.IOException;

public class MainClass {

    public static void main(String[] args) {
        try {
            Shell shell = new Shell();
            shell.launch();
        } catch (ShellException | IOException ex) {
            ex.printStackTrace();
        }
    }

}
