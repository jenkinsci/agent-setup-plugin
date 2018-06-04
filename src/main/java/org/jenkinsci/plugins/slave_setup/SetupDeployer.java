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
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Executes a deployment to all or a single node of the given fileset and
 * executes the command line.
 *
 * @author Frederik Fromm
 */
public class SetupDeployer {
    /**
     * the logger.
     */
    private static final Logger LOGGER = Logger.getLogger(SetupDeployer.class.getName());

    public static String StringFy(Object obj){
        return Utils.StringFy(obj);
    }

    /**
     * Returns a list of all active slaves connected to the master.
     *
     * @return a list of all active slaves connected to the master
     */
    @Deprecated
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
    public void deployToComputer(Computer c, FilePath root, TaskListener listener, SetupConfig config)
            throws IOException, InterruptedException {
        List<SetupConfigItem> setupConfigItems = config.getSetupConfigItems();

        for (SetupConfigItem setupConfigItem : setupConfigItems) {
            deployToComputer(c, root, listener, setupConfigItem);
        }
    }

    /**
     * @param c               the computer to upload th files to
     * @param root            the computer's target directory
     * @param listener        the listener for logging etc.
     * @param setupConfigItem the SetupConfigItem object containing the source dir
     *                        and the command line
     * @throws IOException
     * @throws InterruptedException
     */
    public void deployToComputer(Computer c, FilePath root, TaskListener listener, SetupConfigItem setupConfigItem)
            throws IOException, InterruptedException {
        // do not deploy is prepare script for this setup config item did not work
        if (!setupConfigItem.isPrepareScriptExecuted()) {
            listener.getLogger().println("Slave NOT set up as prepare script was not executed successfully.");
            return;
        }

        // do not deploy if label of computer and config do not match.
        if (checkLabels(c, setupConfigItem, listener)) {
            // copy files
            File sd = setupConfigItem.getFilesDir();
            if (sd != null && StringUtils.isNotBlank(sd.getPath())) {
                listener.getLogger().println("Copying setup script files from " + sd);
                new FilePath(sd).copyRecursiveTo(root);
            }

            // execute command line
            String cmdLine = setupConfigItem.getCommandLine();

            // executeScript(c.getNode(), root, listener, cmdLine, createEnvVarsForComputer(c));
            Utils.multiOsExecutor(listener, cmdLine, root, createEnvVarsForComputer(c));
        } else {
            listener.getLogger()
                    .println("Slave " + c.getName() + " NOT set up as assigned label expression '"
                            + setupConfigItem.getAssignedLabelString() + "' does not match with node label '"
                            + c.getNode().getLabelString() + "'");
        }
    }

    /**
     * Checks if this script should be executed on the target computer. If the
     * computer is not set (on save of the jenkins configuration page) or if the
     * label expression of the config matches with the given computer.
     */
    @Deprecated
    private boolean checkLabelsForComputerOrNull(Computer c, SetupConfigItem item, TaskListener listener) {

        listener.getLogger().println("DEBUG:On checkLabelsForComputerOrNull");
        listener.getLogger().println("Checking for nulls");
        return c == null || checkLabels(c, item, listener);
    }

    /**
     * Returns true if the given setup config item is responsible for the given
     * slave computer.
     *
     * @param c               the slave computer
     * @param setupConfigItem the setup config item to check
     * @return true if the given setup config item is responsible for the given
     *         slave computer
     */
    @Deprecated
    public boolean oldCheckLabels(Computer c, SetupConfigItem setupConfigItem) {
        String assignedLabel = setupConfigItem.getAssignedLabelString();
        if (StringUtils.isBlank(assignedLabel)) {
            return true;
        }
        Label label = Label.get(assignedLabel);

        // TODO: check it this link => https://github.com/beerdn/slave-setup-plugin/commit/511b19f24d6a59902c6d6b6c838c7c8a85674d85
        return label.matches(c.getNode());
    }

    @Deprecated
    public void FastPrint(TaskListener lis, String message) {
        lis.getLogger().println(message);
    }

    @Deprecated
    public boolean checkLabels(Computer c, SetupConfigItem setupConfigItem, TaskListener listener) {

        listener.getLogger().println("DEBUG:On checklabels");

        FastPrint(listener, String.format("Check args for %s\nComputer: %s\nSetupItem: %s\n", c.getNode().getDisplayName(), StringFy(c), setupConfigItem));


        /*
        try {
            EnvVars cVars = c.getEnvironment();
            listener.getLogger().println(cVars.firstKey());
            for (String currentVar : cVars.values()) {
                listener.getLogger().println("Enviroments " + currentVar);
            }

        } catch (Exception ex) {
            listener.getLogger().println(String.format("Error getting node rootPath : %s", ex.getMessage()));
            home = "Error Thrown";
        }
        */
        //listener.getLogger().println(String.format("Slave RootPath: %s", c.getNode().getRootPath().getRemote()));
        // PUT ME HEERE
        ArrayList<String> installedComponents = new ArrayList<String>();
        try {
            //installedComponents = setupConfigItem.getInstalledComponents(listener,
                    // c.getNode().getRootPath());
        } catch (Exception ex) {
            FastPrint(listener, String.format("values of node: " + StringFy(c.getNode())));
            listener.getLogger().println(String.format("Error getting Remote rootPath : %s", ex.getMessage()));
        }
        for (String component : installedComponents) {
            listener.getLogger().println(component);
        }

        String assignedLabel = setupConfigItem.getAssignedLabelString();
        if (StringUtils.isBlank(assignedLabel)) {
            return true;
        }

        // Label l =
        // Jenkins.getInstance().getLabel(setupConfigItem.getAssignedLabelString());

        listener.getLogger().println(String.format("AssignedLabel for node is : %s", assignedLabel));
        String[]labels = assignedLabel.split("\\s+");

        for(String currentLabel:labels)
        {
            if(!installedComponents.contains(currentLabel))
            {
                
                listener.getLogger().println(String.format("%s in Node's assigned label.", assignedLabel));
                return true;
            }
        }
        
        listener.getLogger().println(String.format("%s is NOT in Node's assigned label.Executing Script", assignedLabel));
        return false;

        //Label label = Label.get(assignedLabel);
        //return label.matches(c.getNode());
    }

    /**
     * TODO: Deprecate me, use Utils.multiOsExecutor instead
     */
    @Deprecated
    public void executeScript(TaskListener listener, FilePath root,  String cmdLine, EnvVars additionalEnvironment) throws IOException, InterruptedException {
        if (StringUtils.isNotBlank(cmdLine)) {
            // String nodeName = node.getNodeName();
            // 28.05.18 Retrieving verbose printing avoiding noise during multilineScripts
            // executions. (1.11.2 rev)
            // listener.getLogger().println("Executing script '" + cmdLine + "' on " +
            // (StringUtils.isEmpty(nodeName) ? "master" : nodeName));
            // 25.05.18
            // New Os Check & Switch to execute targeted scripts.
            int r = Utils.multiOsExecutor(listener, cmdLine, root,null);
            listener.getLogger().println("script executed successfully.");
        }
    }

    /**
     * @param computerList    the list of computers to upload the setup files and
     *                        execute command line
     * @param setupConfigItem the SetupConfigItem object
     */
    @Deprecated
    public void deployToComputers(List<Computer> computerList, SetupConfigItem setupConfigItem) {
        for (Computer computer : computerList) {
            try {
                FilePath root = computer.getNode().getRootPath();
                LogTaskListener listener = new LogTaskListener(LOGGER, Level.ALL);
                deployToComputer(computer, root, listener, setupConfigItem);
            } catch (IOException e) {
                LOGGER.severe(e.getMessage());
            } catch (InterruptedException e) {
                LOGGER.severe(e.getMessage());
            }
        }
    }

    /**
     * @param computerList the list of computers to upload the setup files and
     *                     execute command line
     * @param config       the SetupConfig object
     */
    @Deprecated
    public void deployToComputers(List<Computer> computerList, SetupConfig config) {
        for (Computer computer : computerList) {
            try {
                FilePath root = computer.getNode().getRootPath();
                LogTaskListener listener = new LogTaskListener(LOGGER, Level.ALL);
                deployToComputer(computer, root, listener, config);
            } catch (IOException e) {
                LOGGER.severe(e.getMessage());
            } catch (InterruptedException e) {
                LOGGER.severe(e.getMessage());
            }
        }
    }

    @Deprecated 
    public boolean executePrepareScript(Computer c, SetupConfigItem config, TaskListener listener){
        //TODO: This will run one script only, Copy above code without SetupConfig iteration
        //TIP: you can improve syntax, and reutilization, moving instead copy  the inner code in iteration from above function.
        //and finally return boolean to get if failed or not
        try {
            Components.println("Executing prepareScript");
            if (StringUtils.isBlank(config.getPrepareScript())) {
                config.setPrepareScriptExecuted(true);
            } else if (checkLabelsForComputerOrNull(c, config, listener)) {
                // boolean successful = executeScriptOnMaster(config.getPrepareScript(), c, listener);
                // config.setPrepareScriptExecuted(successful);
                config.setPrepareScriptExecuted(true);
            }
            return true;
        } catch (Exception ex) {
            Components.println("SetupDeployer:executePrepareScript::" + ex.getMessage());
            return false;
        }
    }
    
    @Deprecated
    public void executePrepareScripts(Computer c, SetupConfig config, TaskListener listener, ArrayList<String>installedComponents) {
        listener.getLogger().println("Executing PrepareScripts By BH");
        for (SetupConfigItem setupConfigItem : config.getSetupConfigItems()) {
            listener.getLogger().println("Checking config " + setupConfigItem.toString());
            Utils.displayInstalledComponents(listener,installedComponents);
            if (StringUtils.isBlank(setupConfigItem.getPrepareScript())) {
                setupConfigItem.setPrepareScriptExecuted(true);
            } else if (checkLabelsForComputerOrNull(c, setupConfigItem, listener)) {
                // boolean successful = executeScriptOnMaster(setupConfigItem.getPrepareScript(), c, listener);
                setupConfigItem.setPrepareScriptExecuted(true);
            }
        }
    }

    /*
    public void executePreLaunchScripts(Computer c, SetupConfig config, TaskListener listener) throws AbortException {
        listener.getLogger().println("Executing PrelaunchScripts By BH");
        for (SetupConfigItem setupConfigItem : config.getSetupConfigItems()) {

            listener.getLogger().println("Checking config " + setupConfigItem.toString());

            if (!StringUtils.isBlank(setupConfigItem.getPreLaunchScript())
                    && checkLabels(c, setupConfigItem, listener)) {
                boolean successful = executeScriptOnMaster(setupConfigItem.getPreLaunchScript(), c, listener);
                if (!successful) {
                    throw new AbortException("pre-launch script not executed successfully");
                }
            }
        }
    }
    */

    public static int executeScriptOnMaster(String script, Computer c, TaskListener listener, EnvVars enviroment) {
        // execute scripts on master relative to jenkins install dir
        Node node = Jenkins.getInstance();
        FilePath filePath = node.getRootPath();
        Components.debug("Master given path is "+ filePath.toString());
        try {
            return Utils.multiOsExecutor(listener, script, filePath, enviroment);

        } catch (Exception e) {
            Components.println("script failed with exception: " + e.getMessage());
            return 0xffffff0A;
        }
    }

    public static EnvVars createEnvVarsForComputer(Computer c) {
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
     * @param node                  node to get the environment variables from
     * @param additionalEnvironment environment added to the environment from the
     *                              node. Take precedence over environment from the
     *                              node.
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
