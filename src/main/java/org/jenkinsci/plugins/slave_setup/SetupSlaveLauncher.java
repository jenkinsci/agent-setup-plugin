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
import hudson.EnvVars;
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
    private void execute(String script, TaskListener listener, SlaveComputer computer) throws IOException, InterruptedException {
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
            Shell shell = new Shell(script);
            FilePath root = jenkins.getRootPath();
            FilePath scriptFile = shell.createScriptFile(root);
            int r = launcher.launch().cmds(shell.buildCommandLine(scriptFile)).envs(createEnvVarsFromComputer(computer)).stdout(listener).join();

            if (r != 0) {
                throw new AbortException("Script failed with return code " + Integer.toString(r) + ".");
            }

            listener.getLogger().println("Script executed successfully.");
    }

    private EnvVars createEnvVarsFromComputer(SlaveComputer computer) {
        EnvVars additionalEnvironment = new EnvVars();
        if (computer != null) {
            additionalEnvironment.put("SLAVE_NAME", computer.getName());
        }
        return additionalEnvironment;
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
        execute(startScript, listener, computer);
        super.launch(computer, listener);
    }

    @Override
    public void afterDisconnect(SlaveComputer computer, TaskListener listener) {
        super.afterDisconnect(computer, listener);

        try {
            execute(stopScript, listener, computer);
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

