package org.jenkinsci.plugins.slave_setup;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.tasks.BatchFile;
import hudson.tasks.Shell;

import java.io.IOException;

public class Utils {

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
        listener.getLogger().println("Checking operating system of slave, isUnix: " + String.valueOf(launcher.isUnix()));
        if (launcher.isUnix()) {
            /*
            Originally this plugin used only Shell(script) and shell.buildCommandLine(scriptfile)(this portion of code)
            to execute commands in SetupSlaveLauncher.java during the execution and SetupDeployer.java on executeScript
            method.
             */
            listener.getLogger().println("Slave is detected as Unix executing script:\n" + script);
            Shell shell = new Shell(script);
            FilePath scriptFile = shell.createScriptFile(root);
            r = launcher.launch().cmds(shell.buildCommandLine(scriptFile)).stdout(listener).join();
        } else {
            /*
            We create a BatchFile obj instead a Shell classObject if the current OS is not Unix
             */
            listener.getLogger().println("Slave is detected as Windows executing script:\n" + script);
            BatchFile batch = new BatchFile(script);
            FilePath scriptFile = batch.createScriptFile(root);
            r = launcher.launch().cmds(batch.buildCommandLine(scriptFile)).stdout(listener).join();
        }
        return r;
    }
}
