package org.jenkinsci.plugins.slave_setup;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Computer;
import hudson.model.Label;
import hudson.model.TaskListener;
import hudson.tasks.BatchFile;
import hudson.tasks.Shell;

import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Utils {

    public static String configFileName = "slave_setup.ini";
    // public static TaskListener listener;

    // New OS switching Method 25.05.18
    /*
     * Added Unix and Win32 Platform Handling to add support to Windows OSs by using
     * the BathFile obj letting us to configure automated WIN machines on our
     * Integration enviroments. Observed that both Shell and BatchFile are heritages
     * from CommandInterpreter so we added the BatchFile for WIN OSs support.
     * 
     * We decided to fork and add this small portion of code instead creating a new
     * one.
     * 
     * This new static method replaces on SetupDeployer and SetupSlaveLauncher the
     * execute and executeScript Shell instancing and executing respectively.
     */
    public static int multiOsExecutor(TaskListener listener, String script, FilePath root, EnvVars enviroment)
            throws IOException, InterruptedException {
        // 31.5.18, Aaron: Remove int memory allocation. and remove code commented
        // 1.6.18 Aaron: Addition of enviroment variables support.
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
    /*
     * public static ArrayList<String> getInstalledComponents(TaskListener listener,
     * FilePath slaveConfigPath) { //String configPath =
     * combinePaths(slaveConfigPath,Utils.configFileName); ArrayList<String>
     * installeds = new ArrayList<String>();
     * listener.getLogger().println(String.format("Config file to open: %s",
     * slaveConfigPath)); try { BufferedReader br = new BufferedReader(new
     * InputStreamReader(slaveConfigPath.read())); String line; while ((line =
     * br.readLine()) != null) { installeds.add(line); } br.close(); return
     * installeds; }catch(Exception ex) {
     * listener.getLogger().println(ex.getMessage()); return installeds; } }
     */

    public static boolean setInstalledComponents(ArrayList<String> installedComponents, String slaveConfigPath) {
        try {
            File configFile = new File(slaveConfigPath + Utils.configFileName);
            configFile.createNewFile();
            FileWriter fileHdl = new FileWriter(configFile, false);
            for (String line : installedComponents) {
                fileHdl.write(line);
            }
            fileHdl.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean addInstalledComponent(String component) {
        String filePath = jenkins.model.Jenkins.getInstance().getRootDir().toString();
        try {
            FileWriter fileHdl = new FileWriter(new File(filePath + Utils.configFileName), true);
            fileHdl.write(component);
            fileHdl.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static void displayInstalledComponents(TaskListener listener, ArrayList<String> installedComponents) {
        listener.getLogger().println("Installed components:");
        if (installedComponents.size() > 0) {
            for (String component : installedComponents) {
                listener.getLogger().println(component);
            }
            listener.getLogger().println();
        }
    }

    public static boolean createConfigFile(FilePath root, TaskListener listener) {
        try {
            FilePath configFile = root.child(configFileName);
            if (configFile != null) {
                listener.getLogger().println("Creating new config file :" + configFile.getRemote());
                configFile.write();
                return true;
            }
            listener.getLogger().println("Error getting configFile FilePath during creation.");
            return false;

        } catch (Exception ex) {
            listener.getLogger().println("Error during creating new config file :" + ex.getMessage());
            return false;
        }
    }

    /**
     * Label matching procedure Based on
     * https://github.com/beerdn/slave-setup-plugin/commit/511b19f24d6a59902c6d6b6c838c7c8a85674d85
     */
    public static boolean labelMatches(String pattern, Computer slave) {
        Label configLabel = Label.get(pattern);
        return configLabel.matches(slave.getNode());

    }

    /**
     * This function will print all messages we wan as INFO
     */
    /*
     * public static void println(String message){
     * Utils.listener.getLogger().println(message); }
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
}
