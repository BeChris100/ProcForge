package net.bc100dev.pfc.sh;

import net.bc100dev.commons.CLIParser;
import net.bc100dev.commons.ResourceManager;
import net.bc100dev.commons.Terminal;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * The PCL (Process Command Line) is an interactive shell, used for
 * interacting with the Instagram Private APIs, along with the interaction
 * of the official and publicly available Instagram Graph API.
 */
public class Shell {

    private final Scanner scIn;

    private final List<ShellConfig> shellConfigList = new ArrayList<>();
    private final List<ShellCaller> shellCallers = new ArrayList<>();

    private String PS1 = "==> ";

    /**
     * Initializes the PCL with the necessary Input and the necessary commands.
     *
     * @throws ShellException May throw an exception during initializing the commands.
     * @throws IOException    Throws an exception, when cannot read any command entries
     */
    public Shell() throws IOException, ShellException {
        this.scIn = new Scanner(System.in);

        appendCallers(Shell.class, "/net/bc100dev/pfc/res/commands.json");
    }

    public void appendConfig(List<ShellConfig> configList) {
        shellConfigList.addAll(configList);
    }

    /**
     * Reads a JSON File and parses into the necessary commands and other relevant information for the Shell
     *
     * @param jsonFile The JSON file to parse
     * @throws IOException    Will throw on Input Readers
     * @throws ShellException Will throw on classes/methods that are not found
     */
    public void appendCallers(File jsonFile) throws IOException, ShellException {
        ShellCommandEntry entry = ShellCommandEntry.initialize(jsonFile);
        shellCallers.addAll(entry.getCommands());
    }

    /**
     * Reads a JSON File and parses into the necessary commands and other relevant information for the Shell
     *
     * @param correspondingClass A corresponding class to its Class Path
     * @param resourceFile       The resource file within its Class Path
     * @throws IOException    Will throw on Input Readers
     * @throws ShellException Will throw on classes/methods that are not found
     */
    public void appendCallers(Class<?> correspondingClass, String resourceFile) throws IOException, ShellException {
        ResourceManager resMgr = new ResourceManager(correspondingClass, false);
        ShellCommandEntry entry = ShellCommandEntry.initialize(resMgr, resourceFile);
        shellCallers.addAll(entry.getCommands());
    }

    /**
     * Parses and assigns/reads a specific configuration. These configurations can be
     * passed to their perspective methods to read, which can be used to remove most of
     * the arguments for their specific commands, depending on their use case.
     *
     * @param line The line to get a configuration parsed
     */
    private void assignCfg(String line) {
        if (line.contains("=")) {
            String[] opt = line.split("=", 2);
            opt[0] = opt[0].trim().replaceFirst("&", "");
            opt[1] = opt[1].trim();

            if (opt[1].contains("\""))
                opt[1] = opt[1].replaceAll("\"", "");

            if (opt[0].equals("PS1")) {
                PS1 = opt[1];
                return;
            }

            if (shellConfigList.isEmpty()) {
                shellConfigList.add(new ShellConfig(opt[0], opt[1]));
                return;
            }

            boolean found = false;
            for (int i = 0; i < shellConfigList.size(); i++) {
                ShellConfig cfg = shellConfigList.get(i);

                if (opt[0].equals(cfg.name)) {
                    cfg.value = opt[1];

                    shellConfigList.set(i, cfg);

                    found = true;
                }
            }

            if (!found)
                shellConfigList.add(new ShellConfig(opt[0], opt[1]));
        } else {
            if (shellConfigList.isEmpty()) {
                System.out.println("NoItemsAssigned");
                return;
            }

            String nm = line.replaceFirst("&", "").trim();

            boolean found = false;
            for (ShellConfig cfg : shellConfigList) {
                if (nm.equals(cfg.name)) {
                    System.out.printf("%s ==> %s\n", cfg.name, cfg.value);
                    found = true;
                }
            }

            if (!found)
                System.out.printf("%s is not defined\n", nm);
        }
    }

    /**
     * The beauty happens here: The interactive shell.
     */
    private void cmd() {
        System.out.print(PS1);
        String ln = scIn.nextLine().trim();

        if (ln.isEmpty()) {
            cmd();
            return;
        }

        if (ln.startsWith("&")) {
            assignCfg(ln);
            cmd();
            return;
        }

        String[] lnSplits = CLIParser.translateCmdLine(ln);

        if (lnSplits.length == 0) {
            Terminal.println(Terminal.Color.RED, String.format("Syntax error with parsing line \"%s\"", ln), true);
            cmd();
            return;
        }

        String exec = lnSplits[0];
        String[] givenArgs = new String[lnSplits.length - 1];

        if (lnSplits.length > 1)
            System.arraycopy(lnSplits, 1, givenArgs, 0, lnSplits.length - 1);

        switch (exec) {
            case "help", "app-help", "?" -> {
                StringTokenizer tok = new StringTokenizer(ln, " ");
                Map<String, String> helps = new HashMap<>();

                while (tok.hasMoreTokens()) {
                    String tokVal = tok.nextToken();

                    if (tokVal.equals("help"))
                        // We do not need any help from the "help" command
                        continue;

                    for (ShellCaller caller : shellCallers) {
                        if (caller.getCommand().equals(tokVal)) {
                            try {
                                if (helps.containsKey(tokVal))
                                    continue;

                                helps.put(tokVal, caller.retrieveLongHelp());
                            } catch (ShellException ignore) {
                                Terminal.println(Terminal.Color.RED,
                                        String.format("Unknown command \"%s\"", tokVal), true);
                            }
                        } else {
                            for (String altCommand : caller.getAlternateCommands()) {
                                if (altCommand.equals(tokVal)) {
                                    try {
                                        if (helps.containsKey(caller.getCommand()))
                                            continue;

                                        helps.put(caller.getCommand(), caller.retrieveLongHelp());
                                    } catch (ShellException ignore) {
                                        Terminal.println(Terminal.Color.RED,
                                                String.format("Unknown command \"%s\"", tokVal), true);
                                    }
                                }
                            }
                        }
                    }
                }

                if (!helps.keySet().isEmpty()) {
                    List<String> cmd = new ArrayList<>(helps.keySet());

                    if (cmd.size() == 1)
                        Terminal.println(Terminal.Color.BLUE, helps.get(cmd.get(0)), true);
                    else {
                        for (int i = 0; i < cmd.size(); i++) {
                            Terminal.println(Terminal.Color.CYAN, cmd.get(i), true);
                            Terminal.println(Terminal.Color.BLUE, helps.get(cmd.get(i)), true);

                            if (i != cmd.size() - 1)
                                System.out.println();
                        }
                    }
                } else {
                    int maxCmdLength = 0;

                    for (ShellCaller caller : shellCallers) {
                        String cmd = caller.getCommand();
                        if (cmd.length() > maxCmdLength)
                            maxCmdLength = cmd.length();
                    }

                    maxCmdLength += 5;

                    for (ShellCaller caller : shellCallers) {
                        String cmd = caller.getCommand();
                        int spaces = maxCmdLength - cmd.length();

                        Terminal.print(Terminal.Color.CYAN, cmd + " ".repeat(spaces), true);
                        Terminal.println(Terminal.Color.YELLOW, caller.retrieveShortHelp(), true);
                    }
                }

                cmd();
                return;
            }
            case "exit", "quit", "close" -> {
                scIn.close();
                System.exit(0);
                return;
            }
            default -> execCommand(exec, givenArgs);
        }

        cmd();
    }

    /**
     * Executes a command from the parsed line.
     *
     * @param exec The command parameter
     * @param args Given PCL command parameters
     */
    private void execCommand(String exec, String[] args) {
        boolean cmdFound = false;
        for (ShellCaller caller : shellCallers) {
            if (caller.getCommand().equals(exec)) {
                cmdFound = true;

                try {
                    int code = caller.execute(args, shellConfigList);
                    if (code != 0) {
                        Terminal.println(Terminal.Color.RED,
                                caller.getCommand() + ": exit code " + code, true);
                    }

                    break;
                } catch (ShellException ex) {
                    ex.printStackTrace();
                }
            } else {
                for (String alternate : caller.getAlternateCommands()) {
                    if (alternate.equals(exec)) {
                        cmdFound = true;

                        try {
                            int code = caller.execute(args, shellConfigList);
                            if (code != 0) {
                                Terminal.println(Terminal.Color.RED,
                                        alternate + ": exit code " + code, true);
                            }
                        } catch (ShellException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        }

        if (!cmdFound)
            Terminal.println(Terminal.Color.RED, String.format("%s: Command not found", exec), true);
    }

    /**
     * Launches the interactive Shell
     */
    public void launch() {
        cmd();
    }
}
