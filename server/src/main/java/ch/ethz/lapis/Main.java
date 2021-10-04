package ch.ethz.lapis;

import ch.ethz.lapis.core.Config;
import ch.ethz.lapis.core.ConfigurationManager;
import ch.ethz.lapis.core.SubProgram;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class Main {

    private static ArrayList<SubProgram> subPrograms;
    private static Map<String, SubProgram> subProgramMap;

    public static void main(String[] args) throws Exception {
        System.out.println("Welcome at Vineyard!");

        // Load sub programs
        subPrograms = new ArrayList<>() {{
            add(new LapisMain());
        }};
        subProgramMap = new HashMap<>();
        for (SubProgram subProgram : subPrograms) {
            subProgramMap.put(subProgram.getName(), subProgram);
        }

        // Parse arguments
        if (args.length == 0) {
            printHelp();
            System.exit(1);
        }
        if (args[0].equals("--help") || args[0].equals("help")) {
            printHelp();
            System.exit(0);
        }
        int subProgramArgsOffset = 0;
        Path configFilePath = null;
        if (args[0].equals("--config")) {
            configFilePath = Path.of(args[1]);
            subProgramArgsOffset = 2;
        }
        String subProgramName = args[subProgramArgsOffset];
        SubProgram subProgram = subProgramMap.get(subProgramName);
        if (subProgram == null) {
            System.out.println("Unknown subprogram. Available programs:");
            printListOfSubPrograms();
            System.exit(1);
        }

        // Load configs
        ConfigurationManager configurationManager = new ConfigurationManager();
        Config configuration;
        if (configFilePath != null) {
            configuration = configurationManager.loadConfiguration(configFilePath,
                subProgram.getConfigClass(), subProgram.getName());
        } else {
            configuration = configurationManager.loadConfiguration(subProgram.getConfigClass(), subProgram.getName());
        }

        // Start sub program
        String[] argsForSubProgram = Arrays.copyOfRange(args, subProgramArgsOffset + 1, args.length);
        subProgram.run(argsForSubProgram, configuration);
    }


    private static void printHelp() {
        System.out.println("""
            Usage: program --help
                | program help
                | program [--config <path to config file>] <sub program name> <args for sub program...>
            Available subprograms are:""");
        printListOfSubPrograms();
    }


    private static void printListOfSubPrograms() {
        for (SubProgram subProgram : subPrograms) {
            System.out.println("  - " + subProgram.getName());
        }
    }

}
