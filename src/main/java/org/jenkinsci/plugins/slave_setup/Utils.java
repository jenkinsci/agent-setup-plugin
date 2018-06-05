package org.jenkinsci.plugins.slave_setup;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Computer;
import hudson.model.Label;
import hudson.model.TaskListener;
import hudson.model.labels.LabelAtom;
import hudson.tasks.BatchFile;
import hudson.tasks.Shell;
import jenkins.model.Jenkins;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Static methods class container used across the code.
 * 
 * @author ByteHeed:
 * @author Mikel Royo Gutierrez
 * @author Aaron Giovannini
 */
public class Utils {

    /**
     * With given boolean for isUnix operating System, returns OS end of line
     * separator
     * 
     * @param isUnix boolean, true for Unix OS
     * @return String, conaining Operating System End of Line character
     */
    public static String osLineSeparator(boolean isUnix) {
        return isUnix ? "\n" : "\r\n";
    }

    /**
     * Given full path from OS, returns os End of line separator
     * 
     * @param someFullPath String with non relative path to any file/folder
     * @return String, conaining Operating System End of Line character
     */
    public static String osLineSeparator(String someFullPath) {
        Components.debug("incoming path to get OS " + someFullPath);
        return Utils.osLineSeparator(someFullPath.startsWith("/"));
    }

    /** 
     * Added Unix and Win32 Platform Handling to add support to Windows OS by using
     * the BatchFile obj letting us to configure automated WIN machines on our
     * Integration enviroments. Observed that both Shell and BatchFile are heritages
     * from CommandInterpreter so we added the BatchFile for WIN OSs support.
     * 
     * This new static method replaces on SetupDeployer and SetupSlaveLauncher the
     * execute and executeScript Shell instancing and executing respectively.
     *  
     * @param listener TaskListener, connected to slave computer, will give us channel
     * @param script String pure script content to be executed in Abstracted OS
     * @param root FilePath (Jenkins) from Computer to be executed
     * @param enviroment EnvVars, not necesary, but will be included in Computer before be executed
     * 
     * @return int containing returnCode
     * 
     * @throws IOException if failed to read/write some file
     * @throws InterruptedException User request Disconnect/Cancel
     */
    public static int multiOsExecutor(TaskListener listener, String script, FilePath root, EnvVars enviroment)
            throws IOException, InterruptedException {

        Launcher launcher = root.createLauncher(listener);
        if (launcher.isUnix()) {
            /*
             * Originally this plugin used only Shell(script) and
             * shell.buildCommandLine(scriptfile)(this portion of code) to execute commands
             * in SetupSlaveLauncher.java during the execution and SetupDeployer.java on
             * executeScript method.
             */
            Shell shell = new Shell(script);
            FilePath scriptFile = shell.createScriptFile(root);
            return launcher.launch().cmds(shell.buildCommandLine(scriptFile)).pwd(root).envs(enviroment)
                    .stdout(listener).join();
        } else {
            /*
             * We create a BatchFile obj instead a Shell classObject if the current OS is
             * not Unix Also we comment those verbose printings about Slave's OS
             */
            BatchFile batch = new BatchFile(script);
            FilePath scriptFile = batch.createScriptFile(root);
            return launcher.launch().cmds(batch.buildCommandLine(scriptFile)).pwd(root).envs(enviroment)
                    .stdout(listener).join();
        }

    }

    /**
     * Improved function, this will check if label matches with one of slave labels or
     * Will match if some reggex matches with all slave labels
     * 
     * 
     * https://github.com/beerdn/slave-setup-plugin/commit/511b19f24d6a59902c6d6b6c838c7c8a85674d85
     * 
     * @param pattern String pattern containing one label or reggex 
     * @param slave Computer where will check if pattern matchs
     * 
     * @return boolean true if pattern matches with slave labels
     */
    public static boolean labelMatches(String pattern, Computer slave) {
        Label configLabel = Label.get(pattern.toLowerCase());
        ListIterator<LabelAtom> iterator = new ArrayList<LabelAtom>(slave.getNode().getAssignedLabels()).listIterator();
        Set<LabelAtom> labels = new HashSet<LabelAtom>();
        while (iterator.hasNext())
        {
            labels.add(new LabelAtom(iterator.next().getName().toLowerCase()));
        }
        return configLabel.matches(labels);
            // slave.getNode());

    }

    /**
     * Idea from https://stackoverflow.com/questions/1526826/printing-all-variables-value-from-a-class
     * 
     * This class prints entire properies, methods, from any object, Useful to debug unkown objects
     * 
     * @param obj Object to get verbose content
     * @return String with entire obj readed
     */
    public static String StringFy(Object obj) {

        StringBuilder result = new StringBuilder();
        try {
            String newLine = System.getProperty("line.separator");

            result.append(obj.getClass().getName());
            result.append(" Object {");
            result.append(newLine);

            // determine fields declared in obj class only (no fields of superclass)
            Field[] fields = obj.getClass().getDeclaredFields();

            // print field names paired with their values
            for (Field field : fields) {
                result.append("  ");
                try {
                    result.append(field.getName());
                    result.append(": ");
                    field.setAccessible(true);
                    // requires access to private field:
                    result.append(field.get(obj));
                } catch (IllegalAccessException ex) {
                    System.out.println(ex);
                }
                result.append(newLine);
            }
            result.append("}");
        } catch (Exception ex) {
            result.append("Unable to get attributes: " + ex.getMessage());
        }
        return result.toString();
    }

    /**
     * Gets all active (ONLINE) slaves running on the all jenkins instance.
     * @return List of Computer containing the current active slaves.
     */
    public static List<Computer> getAllActiveSlaves() {
        final List<Computer> computers = Arrays.asList(Jenkins.getInstance().getComputers());

        List<Computer> activeComputers = new ArrayList<Computer>();

        for (Computer computer : computers) {
            if (!(computer instanceof Jenkins.MasterComputer) && computer.isOnline()) {
                activeComputers.add(computer);
            }
        }

        return activeComputers;
    }

}
