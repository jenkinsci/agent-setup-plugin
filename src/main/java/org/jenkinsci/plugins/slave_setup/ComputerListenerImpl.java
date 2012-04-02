package org.jenkinsci.plugins.slave_setup;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.remoting.Channel;
import hudson.slaves.ComputerListener;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.tasks.Shell;

import java.io.File;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
@Extension
public class ComputerListenerImpl extends ComputerListener {

    /**
     * Prepares the slave before it gets online by copying the given content in root and executing the configured setup script.
     * @param c the computer to set up
     * @param channel not used
     * @param root the root of the slave
     * @param listener log listener
     * @throws IOException
     * @throws InterruptedException
     */
    @Override
    public void preOnline(Computer c, Channel channel, FilePath root, TaskListener listener) throws IOException, InterruptedException {
        SetupConfig config = SetupConfig.get();

        SetupDeployer deployer = new SetupDeployer();

        deployer.executePrepareScripts(config, listener);

        deployer.deployToComputer(c, root, listener, config);
    }

}
