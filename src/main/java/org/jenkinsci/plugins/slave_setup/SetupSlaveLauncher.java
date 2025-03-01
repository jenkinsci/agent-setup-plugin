package org.jenkinsci.plugins.slave_setup;

import com.google.common.base.Strings;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.DelegatingComputerLauncher;
import hudson.slaves.SlaveComputer;
import java.io.IOException;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Implements the custom logic for an on-demand slave, executing scripts before connecting and after disconnecting
 */
public class SetupSlaveLauncher extends DelegatingComputerLauncher {

    private final String startScript;
    private final String stopScript;

    @DataBoundConstructor
    public SetupSlaveLauncher(ComputerLauncher launcher, String startScript, String stopScript) {
        super(launcher);
        this.startScript = startScript;
        this.stopScript = stopScript;
    }

    /**
     * Executes a script on the master node, with a bit of tracing.
     * @param script String script to execute.
     * @param listener TaskListener of the job.
     */
    private void execute(String script, TaskListener listener) throws IOException, InterruptedException {
        Jenkins jenkins = Jenkins.getInstance();
        if (Strings.isNullOrEmpty(script)) {
            listener.getLogger().println("No script to be executed for this on-demand slave.");
            return;
        }

        FilePath root = jenkins.getRootPath();
        int r = Utils.multiOsExecutor(listener, script, root, null);

        if (r != 0) {
            throw new AbortException("Script failed with return code " + Integer.toString(r) + ".");
        }
        listener.getLogger().println("Script executed successfully.");
    }

    /**
     * Getters for Jelly
     * @return Object startScript
     *
     */
    public String getStartScript() {
        return startScript;
    }

    /**
     * @return Object stopScript
     */
    public String getStopScript() {
        return stopScript;
    }

    /**
     *  Delegated methods that plug the additional logic for on-demand slaves
     *
     * @param computer SlaveComputer target to perform the launch.
     * @param listener Job's TaskListener
     *
     */
    @Override
    public void launch(SlaveComputer computer, TaskListener listener) throws IOException, InterruptedException {
        execute(startScript, listener);
        listener.getLogger().println("Pre-Launch HERE Pls Print it " + String.valueOf(computer.isUnix()));
        super.launch(computer, listener);
    }

    @Override
    public void afterDisconnect(SlaveComputer computer, TaskListener listener) {
        super.afterDisconnect(computer, listener);

        try {
            execute(stopScript, listener);
        } catch (Exception e) {
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
