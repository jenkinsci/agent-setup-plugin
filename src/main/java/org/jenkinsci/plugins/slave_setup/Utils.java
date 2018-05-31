package org.jenkinsci.plugins.slave_setup;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.tasks.BatchFile;
import hudson.tasks.Shell;

import java.io.*;
import java.util.ArrayList;

public class Utils {

    public static String configFileName = "slave_setup.ini";

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
        //TODO: Rename it multiOsExecutor, can run in master if launcher is master linked.
    public static int remoteRun(Launcher launcher, TaskListener listener, String script, FilePath root) throws IOException, InterruptedException {
        //31.5.18, Aaron: Remove int memory allocation. and remove code commented

        if (launcher.isUnix()) {
            /*
            Originally this plugin used only Shell(script) and shell.buildCommandLine(scriptfile)(this portion of code)
            to execute commands in SetupSlaveLauncher.java during the execution and SetupDeployer.java on executeScript
            method.
             */
            Shell shell = new Shell(script);
            FilePath scriptFile = shell.createScriptFile(root);
            return launcher.launch().cmds(shell.buildCommandLine(scriptFile)).stdout(listener).join();
        } else {
            /*
            We create a BatchFile obj instead a Shell classObject if the current OS is not Unix
            Also we comment those verbose printings about Slave's OS
             */
            BatchFile batch = new BatchFile(script);
            FilePath scriptFile = batch.createScriptFile(root);
            return launcher.launch().cmds(batch.buildCommandLine(scriptFile)).stdout(listener).join();
        }
        
    }
    public static ArrayList<String> getInstalledComponents(TaskListener listener, FilePath slaveConfigPath)
    {
        //String configPath = combinePaths(slaveConfigPath,Utils.configFileName);
        ArrayList<String> installeds = new ArrayList<String>();
        listener.getLogger().println(String.format("Config file to open: %s", slaveConfigPath));
        try
        {
            BufferedReader br = new BufferedReader(new InputStreamReader(slaveConfigPath.read()));
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

    public static boolean setInstalledComponents(ArrayList<String> installedComponents,String slaveConfigPath)
    {
        try {
            File configFile = new File(slaveConfigPath + Utils.configFileName);
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
            FileWriter fileHdl = new FileWriter(new File(filePath + Utils.configFileName), true);
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
        // To return a path string get Os.Separator, and concatenate its.
        File file1 = new File(path1);
        File file2 = new File(file1, path2);
        return file2.getPath();
    }

    public static void displayInstalledComponents(TaskListener listener, ArrayList<String>installedComponents)
    {
        listener.getLogger().println("Installed components:");
        if(installedComponents.size()>0)
        {
            for(String component:installedComponents){
                listener.getLogger().println(component);
            }
            listener.getLogger().println();
        }
    }

    public static boolean createConfigFile(FilePath root,TaskListener listener)
    {
        try
        {
            FilePath configFile = root.child(configFileName);
            if(configFile != null){
                listener.getLogger().println("Creating new config file :" + configFile.getRemote());
                configFile.write();
                return true;
            }
            listener.getLogger().println("Error getting configFile FilePath during creation.");
            return false;

        }catch(Exception ex)
        {
            listener.getLogger().println("Error during creating new config file :" + ex.getMessage());
            return false;
        }
    }
}
