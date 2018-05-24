package org.jenkinsci.plugins.slave_setup;


import com.google.common.base.Strings;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.DelegatingComputerLauncher;
import hudson.slaves.SlaveComputer;
import hudson.tasks.Shell;
import hudson.tasks.BatchFile;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * Implements the custom logic for an on-demand slave, executing scripts before connecting and after disconnecting
 */
public class SetupSlaveLauncher extends DelegatingComputerLauncher {

    private final String startScript;
    private final String stopScript;

    @DataBoundConstructor
    public SetupSlaveLauncher(ComputerLauncher launcher,
                              String startScript,
                              String stopScript) {
        super(launcher);
        this.startScript = startScript;
        this.stopScript = stopScript;
    }

    /**
     * Executes a script on the master node, with a bit of tracing.
     */
    private void execute(String script, TaskListener listener) throws IOException, InterruptedException {
        Jenkins jenkins = Jenkins.getInstance();

        if (jenkins == null) {
            listener.getLogger().println("Jenkins is not ready... doing nothing");
            return;
        }

        if (Strings.isNullOrEmpty(script)) {
            listener.getLogger().println("No script to be executed for this on-demand slave.");
            return;
        }

        Launcher launcher = jenkins.getRootPath().createLauncher(listener);

        FilePath root = jenkins.getRootPath();

        // New OS switching Method
        /*
            Added Unix and Win32 Platform Handling to add support to Windows OSs by using
            the BathFile obj letting us to configure automated WIN machines on our
            Integration enviroments.
            Observed that both Shell and BatchFile are heritages from CommandInterpreter
            so we added the BatchFile for WIN OSs support.

            We decided to fork and add this small portion of code instead creating a new
            one.
         */

        if(SystemUtils.IS_OS_WINDOWS) {
            /*
            Only used Shell(script) and shell.buildCommandLine(scriptfile) to execute commands.
             */
            Shell shell = new Shell(script);
            FilePath scriptFile = shell.createScriptFile(root);
            int r = launcher.launch().cmds(shell.buildCommandLine(scriptFile)).stdout(listener).join();
        }else{
            /*
            Get the current OS and execute the commands depending on the OS to create a Shell
            obj or a BatchFile obj to proceed depending the OS.
             */
            BatchFile batch = new BatchFile(script):
            FilePath scriptFile = batch.createScriptFile(root);
            int r = launcher.launch().cmds(batch.buildCommandLine(scriptFile)).stdout(listener).join();
        }



        if (r != 0) {
            throw new AbortException("Script failed with return code " + Integer.toString(r) + ".");
        }

        listener.getLogger().println("Script executed successfully.");

    }

    /*
     * Getters for Jelly
     */
    public String getStartScript() {
        return startScript;
    }

    public String getStopScript() {
        return stopScript;
    }

    /*
     *  Delegated methods that plug the additional logic for on-demand slaves
     */

    @Override
    public void launch(SlaveComputer computer, TaskListener listener) throws IOException, InterruptedException {
        execute(startScript, listener);
        super.launch(computer, listener);
    }

    @Override
    public void afterDisconnect(SlaveComputer computer, TaskListener listener) {
        super.afterDisconnect(computer, listener);

        try {
            execute(stopScript, listener);
        }  catch (Exception e) {
            listener.getLogger().println("Failed executing script '" + stopScript + "'.");
            e.printStackTrace(listener.getLogger());
        }
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ComputerLauncher> {
        public String getDisplayName() {
            return "Start and stop this node on-demand";
        }
    }

}

