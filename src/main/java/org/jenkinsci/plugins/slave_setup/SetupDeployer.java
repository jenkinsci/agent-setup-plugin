package org.jenkinsci.plugins.slave_setup;

import hudson.*;
import hudson.model.Computer;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.EnvironmentVariablesNodeProperty;
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
 * Executes a deployment to all or a single node of the given fileset and
 * executes the command line.
 *
 * @author Frederik Fromm
 * 
 *         /// Modified by @author ByteHeed
 * @author Mikel Royo Gutierrez
 * @author Aaron Giovannini
 */
public class SetupDeployer {

    /**
     * Linked with Utils StringFy Useful to print object data into string
     */
    public static String StringFy(Object obj) {
        return Utils.StringFy(obj);
    }

    /**
     * Copy localPath into remotePath and return boolean if operation succeded
     * 
     * @return Boolean , true if everything went OK
     * 
     * @throws InterruptedException BrokenPipe
     * @throws IOException IO error while accesing disk
     */
    public static boolean copyFiles(File localPath, FilePath remotePath) throws IOException, InterruptedException {
        if (localPath != null && StringUtils.isNotBlank(localPath.getPath())) {
            Components.info("copying files from " + localPath);
            int tmp = new FilePath(localPath).copyRecursiveTo(remotePath);
            return tmp == 0;
        } else
            return false;
    }

    /**
     * Checks if this script should be executed on the target computer. If the
     * computer is not set (on save of the jenkins configuration page) or if the
     * label expression of the config matches with the given computer.
     * 
     */
    public static boolean checkLabelsForComputerOrNull(Computer c, SetupConfigItem item) {
        return c == null || Utils.labelMatches(item.getAssignedLabelString(), c);
    }

    /**
     * Executes the given script on the given slave independently of their OSs.
     * 
     * @param listener   TaskListener of the job
     * @param script     String script to execute
     * @param root       Slave rootPath as jenkins.FilePath
     * @param enviroment Enviroment variables for this job on this node. From SetupDeployer.createEnvVars(L)
     * 
     * @throws InterruptedException BrokenPipe
     * @throws IOException IO error while accesing disk
     */
    public static int executeScriptOnSlave(TaskListener listener, String script, FilePath root, EnvVars enviroment)
            throws IOException, InterruptedException {
        return Utils.multiOsExecutor(listener, script, root, enviroment);
    }

    /**
     * Executes the given script on Jenkins MasterNode with his envVars logging with given listener 
     * @param script     Script to execute on Master
     * @param listener   Job's listener handle
     * @param enviroment Enviroment's EnvVars from
     *                   SetupDeployer.createEnvVarsForComputer(c)
     * @return int R result from Utils.multiOSexecutor Launcher return.
     */
    public static int executeScriptOnMaster(TaskListener listener, String script, EnvVars enviroment) {

        Node node = Jenkins.getInstance();
        FilePath filePath = node.getRootPath();
        Components.debug("Master given path is " + filePath.getRemote());
        try {
            return Utils.multiOsExecutor(listener, script, filePath, enviroment);

        } catch (Exception e) {
            Components.info("script failed with exception: " + e.getMessage());
            return 0xffffff0A;
        }
    }
    

    /**
     * Gets the current enviroment variables for the given computer
     * @param c Computer instance.
     * @return Computer's EnvVars
     */
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
}
