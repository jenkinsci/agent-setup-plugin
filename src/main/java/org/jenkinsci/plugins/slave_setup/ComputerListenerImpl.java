package org.jenkinsci.plugins.slave_setup;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.remoting.Channel;
import hudson.slaves.ComputerListener;
// import jenkins.model.Jenkins;

import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
@Extension
public class ComputerListenerImpl extends ComputerListener {

    /**
     * 
     * Prepares the slave before it gets online by copying the given content in root
     * and executing the configured setup script.
     * 
     * @param c        the computer to set up
     * @param channel  not used
     * @param root     the root of the slave
     * @param listener log listener
     * 
     * @throws IOException IO error accessing file on disk (cache)
     * @throws InterruptedException Pipe Broken
     */
    @Override
    public void preOnline(Computer c, Channel channel, FilePath root, TaskListener listener)
            throws IOException, InterruptedException {

        // Componentes printer will print only debug messages.
        Components.setLogger(listener); // If you remove this line will stop printing info

        // Uncomment this line to get verbose info
        // Components.enableDebug();
        Components.debug("Start preOnline Procedures, ");

        Components manager = new Components(root, c);

        manager.doSetup();
        Components.debug("Setup Ended");

        manager.clearTemporally();
    }

}
