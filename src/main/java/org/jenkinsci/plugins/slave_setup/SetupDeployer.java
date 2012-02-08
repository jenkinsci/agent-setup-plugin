package org.jenkinsci.plugins.slave_setup;

import hudson.model.Computer;
import hudson.model.Node;
import jenkins.model.Jenkins;

import java.util.List;

/**
 * Executes a deployment to all or a single node of the given fileset and executes the command line.
 *
 */
public class SetupDeployer {

    /**
     * Returns a list of all active slaves connected to the master.
     * @return
     */
    public List<Node> getAllActiveSlaves() {
        final List<Node> nodes = Jenkins.getInstance().getNodes();

        return nodes;
    }
}
