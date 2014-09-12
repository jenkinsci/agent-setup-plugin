package org.jenkinsci.plugins.slave_setup;

import hudson.*;
import hudson.model.Computer;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.tasks.Shell;
import hudson.util.LogTaskListener;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Executes a deployment to all or a single node of the given fileset and executes the command line.
 *
 * @author Frederik Fromm
 */
public class SetupDeployer {
    /**
     * the logger.
     */
    private static final Logger LOGGER = Logger.getLogger(SetupDeployer.class.getName());

    /**
     * Returns a list of all active slaves connected to the master.
     *
     * @return a list of all active slaves connected to the master
     */
    public List<Computer> getAllActiveSlaves() {
        final List<Computer> computers = Arrays.asList(Jenkins.getInstance().getComputers());

        List<Computer> activeComputers = new ArrayList<Computer>();

        for (Computer computer : computers) {
            if (!(computer instanceof Jenkins.MasterComputer) && computer.isOnline()) {
                activeComputers.add(computer);
            }
        }

        return activeComputers;
    }

    /**
     * @param c        the computer to upload th files to
     * @param root     the computer's target directory
     * @param listener the listener for logging etc.
     * @param config   the SetupConfig object containing the all SetupConfigItems
     * @throws IOException
     * @throws InterruptedException
     */
    public void deployToComputer(Computer c, FilePath root, TaskListener listener, SetupConfig config) throws IOException, InterruptedException {
        List<SetupConfigItem> setupConfigItems = config.getSetupConfigItems();

        for (SetupConfigItem setupConfigItem : setupConfigItems) {
            this.deployToComputer(c, root, listener, setupConfigItem);
        }
    }

    /**
     * @param c               the computer to upload th files to
     * @param root            the computer's target directory
     * @param listener        the listener for logging etc.
     * @param setupConfigItem the SetupConfigItem object containing the source dir and the command line
     * @throws IOException
     * @throws InterruptedException
     */
    public void deployToComputer(Computer c, FilePath root, TaskListener listener, SetupConfigItem setupConfigItem) throws IOException, InterruptedException {
        // do not deploy is prepare script for this setup config item did not work
        if (!setupConfigItem.isPrepareScriptExecuted()) {
            listener.getLogger().println("Slave NOT set up as prepare script was not executed successfully.");
            return;
        }

        //do not deploy if label of computer and config do not match.
        if (this.checkLabels(c, setupConfigItem)) {
            // copy files
            File sd = setupConfigItem.getFilesDir();
            if (sd != null && StringUtils.isNotBlank(sd.getPath())) {
                listener.getLogger().println("Copying setup script files from " + sd);
                new FilePath(sd).copyRecursiveTo(root);
            }

            // execute command line
            String cmdLine = setupConfigItem.getCommandLine();

            executeScript(c, root, listener, cmdLine, createEnvVarsForComputer(c));
        } else {
            listener.getLogger().println("Slave " + c.getName() + " NOT set up as assigned label expression '" + setupConfigItem.getAssignedLabelString() + "' does not match with node label '" + c.getNode().getLabelString() + "'");
        }
    }

    /**
     * Returns true if the given setup config item is responsible for the given slave computer.
     *
     * @param c               the slave computer
     * @param setupConfigItem the setup config item to check
     * @return true if the given setup config item is responsible for the given slave computer
     */
    public boolean checkLabels(Computer c, SetupConfigItem setupConfigItem) {
        if (StringUtils.isBlank(setupConfigItem.getAssignedLabelString())) {
            return true;
        }

        //Label l = Jenkins.getInstance().getLabel(setupConfigItem.getAssignedLabelString());
        Label label = Label.get(setupConfigItem.getAssignedLabelString());

        return label.contains(c.getNode());
    }

    private void executeScript(Computer c, FilePath root, TaskListener listener, String cmdLine, EnvVars additionalEnvironment) throws IOException, InterruptedException {
        if (StringUtils.isNotBlank(cmdLine)) {
            listener.getLogger().println("Executing script '" + cmdLine + "' on " + c.getName());
            Node node = c.getNode();
            Launcher launcher = root.createLauncher(listener);
            Shell s = new Shell(cmdLine);
            FilePath script = s.createScriptFile(root);
            int r = launcher.launch().cmds(s.buildCommandLine(script)).envs(getEnvironment(node, additionalEnvironment)).stdout(listener).pwd(root).join();

            if (r != 0) {
                listener.getLogger().println("script failed!");
                throw new AbortException("script failed!");
            }

            listener.getLogger().println("script executed successfully.");
        }
    }

    /**
     * @param computerList    the list of computers to upload the setup files and execute command line
     * @param setupConfigItem the SetupConfigItem object
     */
    public void deployToComputers(List<Computer> computerList, SetupConfigItem setupConfigItem) {
        for (Computer computer : computerList) {
            try {
                FilePath root = computer.getNode().getRootPath();
                LogTaskListener listener = new LogTaskListener(LOGGER, Level.ALL);
                this.deployToComputer(computer, root, listener, setupConfigItem);
            } catch (IOException e) {
                LOGGER.severe(e.getMessage());
            } catch (InterruptedException e) {
                LOGGER.severe(e.getMessage());
            }
        }
    }

    /**
     * @param computerList the list of computers to upload the setup files and execute command line
     * @param config       the SetupConfig object
     */
    public void deployToComputers(List<Computer> computerList, SetupConfig config) {
        for (Computer computer : computerList) {
            try {
                FilePath root = computer.getNode().getRootPath();
                LogTaskListener listener = new LogTaskListener(LOGGER, Level.ALL);
                this.deployToComputer(computer, root, listener, config);
            } catch (IOException e) {
                LOGGER.severe(e.getMessage());
            } catch (InterruptedException e) {
                LOGGER.severe(e.getMessage());
            }
        }
    }

    /**
     * Returns 0 if all prepare scripts were executes without error.
     *
     * @return 0 if all prepare scripts were executes without error
     */
    public void executePrepareScripts(Computer c, SetupConfig config, TaskListener listener) {
        // execute prepare scripts on master relative to jenkins install dir
        Computer computer = Jenkins.MasterComputer.currentComputer();
        FilePath filePath = Jenkins.getInstance().getRootPath();

        for (SetupConfigItem setupConfigItem : config.getSetupConfigItems()) {
            if (StringUtils.isBlank(setupConfigItem.getPrepareScript())) {
                setupConfigItem.setPrepareScriptExecuted(true);
            } else {
                // execute this config's prepare script if the target computer is not set (on save of the
                // jenkins configuration page) or if the label expression of the config matches with the given
                // computer.
                if (c == null || this.checkLabels(c, setupConfigItem)) {
                    try {
                        this.executeScript(computer, filePath, listener, setupConfigItem.getPrepareScript(), createEnvVarsForComputer(c));
                        setupConfigItem.setPrepareScriptExecuted(true);
                    } catch (Exception e) {
                        listener.getLogger().println("prepare script failed with exception: " + e.getMessage());
                        setupConfigItem.setPrepareScriptExecuted(false);
                    }
                }
            }
        }
    }

    private EnvVars createEnvVarsForComputer(Computer c) {
        EnvVars additionalEnvironment = new EnvVars();
        if (c != null) {
            additionalEnvironment.put("NODE_TO_SETUP_NAME", c.getName());
            Node node = c.getNode();
            if (node != null) {
                additionalEnvironment.put("NODE_TO_SETUP_LABELS", Util.join(node.getAssignedLabels(), " "));
            }
        }
        return additionalEnvironment;
    }

    /**
     * Returns the environment variables for the given node.
     *
     * @param node node to get the environment variables from
     * @param additionalEnvironment environment added to the environment from the node. Take precedence over environment from the node.
     * @return the environment variables for the given node
     */
    private EnvVars getEnvironment(Node node, EnvVars additionalEnvironment) {
        EnvVars envVars = new EnvVars();
        EnvironmentVariablesNodeProperty env = node.getNodeProperties().get(EnvironmentVariablesNodeProperty.class);
        if (env != null) {
            envVars.putAll(env.getEnvVars());
        }
        envVars.putAll(additionalEnvironment);
        return envVars;
    }


}
