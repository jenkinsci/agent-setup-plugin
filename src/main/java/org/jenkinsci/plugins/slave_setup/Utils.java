package org.jenkinsci.plugins.slave_setup;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.tasks.BatchFile;
import hudson.tasks.Shell;

import java.io.*;
import java.util.ArrayList;

public class Utils {

    public static String fileName = "slave_setup.ini";

    // New OS switching Method 25.05.18
        /*
            Added Unix and Win32 Platform Handling to add support to Windows OSs by using
            the BathFile obj letting us to configure automated WIN machines on our
            Integration enviroments.
            Observed that both Shell and BatchFile are heritages from CommandInterpreter
            so we added the BatchFile for WIN OSs support.

            We decided to fork and add this small portion of code instead creating a new
            one.

            This new static method replaces on SetupDeployer and SetupSlaveLauncher the execute and executeScript
            Shell instancing and executing respectively.
         */
    public static int remoteRun(Launcher launcher, TaskListener listener, String script, FilePath root) throws IOException, InterruptedException {
        int r;
        //listener.getLogger().println("Checking operating system of slave, isUnix: " + String.valueOf(launcher.isUnix()));
        if (launcher.isUnix()) {
            /*
            Originally this plugin used only Shell(script) and shell.buildCommandLine(scriptfile)(this portion of code)
            to execute commands in SetupSlaveLauncher.java during the execution and SetupDeployer.java on executeScript
            method.
             */
            //listener.getLogger().println("Slave is detected as Unix.");
            Shell shell = new Shell(script);
            FilePath scriptFile = shell.createScriptFile(root);
            r = launcher.launch().cmds(shell.buildCommandLine(scriptFile)).stdout(listener).join();
        } else {
            /*
            We create a BatchFile obj instead a Shell classObject if the current OS is not Unix
            Also we comment those verbose printings about Slave's OS
             */
            //listener.getLogger().println("Slave is detected as Windows.");
            BatchFile batch = new BatchFile(script);
            FilePath scriptFile = batch.createScriptFile(root);
            r = launcher.launch().cmds(batch.buildCommandLine(scriptFile)).stdout(listener).join();
        }
        return r;
    }
    public static ArrayList<String> getInstalledComponents(TaskListener listener,String slaveRootPath)
    {

        listener.getLogger().println("On Utils.getInstalledComponents");
        String configPath = combinePaths(slaveRootPath,Utils.fileName);
        ArrayList<String> installeds = new ArrayList<String>();
        listener.getLogger().println(String.format("Config file to open: %s", configPath));
        try
        {
            BufferedReader br = new BufferedReader(new FileReader(configPath));
            String line;
            while ((line = br.readLine()) != null) {
                installeds.add(line);
            }
            br.close();
            return installeds;
        }catch(Exception ex) {
            listener.getLogger().println(ex.getMessage());
            return installeds;
        }
    }

    public static boolean setInstalledComponents(ArrayList<String> installedComponents)
    {
        String filePath = jenkins.model.Jenkins.getInstance().getRootDir().toString();
        try {
            File configFile = new File(filePath + Utils.fileName);
            configFile.createNewFile();
            FileWriter fileHdl = new FileWriter(configFile, false);
            for(String line:installedComponents)
            {
                fileHdl.write(line);
            }
            fileHdl.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean addInstalledComponent(String component)
    {
        String filePath = jenkins.model.Jenkins.getInstance().getRootDir().toString();
        try {
            FileWriter fileHdl = new FileWriter(new File(filePath + Utils.fileName), true);
            fileHdl.write(component);
            fileHdl.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String combinePaths(String path1, String path2)
    {
        File file1 = new File(path1);
        File file2 = new File(file1, path2);
        return file2.getPath();
    }
}
