package org.jenkinsci.plugins.slave_setup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.TaskListener;

/**
 * We are going to store in cache the master delimiter and each node delimiter
 * while they are deployed.
 * 
 * @author ByteHeed:
 * @author Mikel Royo Gutierrez
 * @author Aaron Giovannini
 */

public class Components {

    private static final String FILENAME = "slave_setup.ini";

    private FilePath remotePath;
    private List<SetupConfigItem> configs;
    private Computer slave;
    private static TaskListener listener;

    private List<String> cache;
    private static boolean debugMode = false;
    private FilePath configFile;
    private String remoteSeparator;

    /**
     * 
     * Add construction description, Checks if config exists and creates it, then
     * store all remote objects to be used at deploy time
     * 
     * @param remoteRootPath FilePath of remote/slave rootPath
     * @param slave          Computer slave.
     * 
     * @throws InterruptedException Pipe Broken
     * @throws IOException          IO error while accessing configFile
     * 
     */
    public Components(FilePath remoteRootPath, Computer slave) throws IOException, InterruptedException {
        this.remotePath = remoteRootPath;
        this.slave = slave;
        this.Initialize();
    }

    private void Initialize() throws IOException, InterruptedException{

        this.configFile = this.remotePath.child(FILENAME);
        if (!this.configFile.exists()) {
            this.configFile.write();
            Components.debug("New config created on " + this.configFile.getRemote());
        }

        this.remoteSeparator = Utils.osLineSeparator(this.remotePath.getRemote());
        this.configs = SetupConfig.get().getSetupConfigItems();
        this.cache = createConfigStream();
    }

    /**
     * 
     * Call Components(slave.getNode().getRootPath(), slave) Add construction
     * description, Checks if config exists and creates it, then store all remote
     * objects to be used at deploy time
     * 
     * @param slave Computer slave.
     * 
     * @throws InterruptedException Pipe Broken
     * @throws IOException          IO error while accessing configFile
     * 
     */
    public Components(Computer slave) throws IOException, InterruptedException {
        this.slave = slave;
        Node node = this.slave.getNode();
        if (node == null)
            throw new InterruptedException("slave return null RootPath");

        this.remotePath = node.getRootPath();
        if (this.remotePath == null)
            throw new InterruptedException("slave return null RootPath");

        this.Initialize();
    }

    /**
     * Check if slave contains some setup configured as false,it means that is first
     * time connection or any designed setup. If we want verbose just set listener
     * and verbose will work
     * 
     * @return Boolean checking if the cache is empty or not to tag it as newDeploy
     */
    public boolean newDeploy() {
        return this.getCache().size() == 0;
    }

    /**
     * Required method to define logging procedures
     * 
     * @param listener TaskListener of the job
     */
    public static void setLogger(TaskListener listener) {
        Components.listener = listener;
    }

    /**
     * Returns current cache, if null will create new one
     * 
     * @return List<String> of components
     */
    private List<String> getCache() {
        if (cache == null)
            cache = new ArrayList<String>();
        return cache;
    }

    /**
     * Appends element to cache, and prevents to create duplicated items with
     * diferent or same versions.
     * 
     * @param component String of component/installation to add.
     */
    private void addCache(String component) {

        // slow logic to find old version installations
        boolean writed = false;

        if (getCache().contains(component)) {
            return;
        }

        // Execute getCache() at firstime, to prevent empty list
        for (int i = 0; i < getCache().size(); i++) {
            if (cache.get(i).contains(component.split(SetupConfigItem.DELIMITER)[0])) {
                cache.set(i, component);
                writed = true;
                break;
            }
        }
        if (writed != true) {
            cache.add(component); // if we don't write cache yet, will add at end
        }
    }

    /**
     * Slave setup flow, here, the method iterates all SetupItems and if not
     * installed jet, will call doDeploy
     * 
     * @throws InterruptedException If connection is broken
     * @throws IOException          IOErrors accessing cache
     * @throws AbortException       User close/Cancelled
     * 
     */
    public void doSetup() throws AbortException, IOException, InterruptedException {
        if (!this.newDeploy()) {
            // If slave contains some setups, will read cache data from slave disk
            Components.info("Updating existing installations for " + slave.getName());
        } else
            Components.info("Executing first install for " + slave.getName());

        Components.debug("Given cache contains this lines:\r\n " + String.join(remoteSeparator, cache));
        for (SetupConfigItem item : configs) {
            this.singleSetup(item);
        }
        // Update 6.6.18 By Aaron, moved to single setup, in order to prevent some
        // install crash disable cache sync into slave
        // closeConfigStream();
    }

    /**
     * Performs the execution of the config item on the matching label nodes.
     * 
     * @param item SetupConfigItem to perform.
     * 
     * @throws InterruptedException If connection is broken
     * @throws IOException          IOErrors accessing hashCode
     * 
     */
    public void singleSetup(SetupConfigItem item) throws IOException, InterruptedException {
        if (Utils.labelMatches(item.getAssignedLabelString(), slave)) {

            Components.debug("Start executing scripts for " + item.getAssignedLabelString() + " with version "
                    + item.hashCode());

            if (!getCache().contains(item.remoteCache())) {
                Components.info("Installing " + item.getAssignedLabelString());
                this.doDeploy(item);
                // 6.6.18 Aaron, At this point we only flush cache wen something is finished
                // install.
                this.closeConfigStream();
                Components.info("Install " + item.getAssignedLabelString() + " succeded");
            } else
                Components.info(String.format("%s slave have last version of %s", slave.getName(),
                        item.getAssignedLabelString()));

        }

    }

    /**
     * Iterates over all instance's SetupConfigItem performing/calling a singleSetup
     * for each SetupConfigItem
     * 
     * @throws InterruptedException If connection is broken
     * @throws IOException          IOErrors accessing cache
     * @throws AbortException       User close/Cancelled
     */
    public void doConfig() throws AbortException, IOException, InterruptedException {
        if (!this.newDeploy()) {
            // If slave contains some setups, will read cache data from slave disk
            Components.info("Updating existing installations for " + slave.getName());
        } else
            Components.info("Executing first install for " + slave.getName());

        Components.debug("Given cache contains this lines:\r\n " + String.join(remoteSeparator, cache));
        for (SetupConfigItem item : configs) {
            if (!item.getDeployNow())
                continue;
            this.singleSetup(item);
        }
        closeConfigStream();
    }

    /**
     * Iterates over all given computers performing the doConfig for each computer
     *
     * @param activeSlaves List of the conected slaveComputers
     * @return Boolean telling if all the node execution went ok with true and error
     *         with false.
     */
    public static boolean doConfigSetups(List<Computer> activeSlaves) {
        boolean succeded = true;
        for (Computer slave : activeSlaves) {
            if (slave.isOffline()) {
                Components.info(slave.getName() + " is offline");
                continue;
            }
            try {
                Components manager = new Components(slave);
                manager.doConfig();
                manager.clearTemporally();
            } catch (Exception ex) {
                Components.info(String.format("Failed to configure %s%nErr:%s", slave.getName(), ex.getMessage()));
                succeded = false;
            }
        }
        return succeded;
    }

    /**
     * Iterates over all the given computers list seting up or updating the
     * installation if required.
     * 
     * @param activeSlaves List conatining all the instance's slaveComputers
     * @return Boolean telling if all the executions went ok with true.
     */
    public static boolean doSetups(List<Computer> activeSlaves) {
        boolean succeded = true;
        for (Computer slave : activeSlaves) {
            if (slave.isOffline()) {
                Components.info(slave.getName() + " is offline");
                continue;
            }
            try {
                Components manager = new Components(slave);
                manager.doSetup();
                manager.clearTemporally();
            } catch (Exception ex) {
                Components.info(String.format("Failed to configure %s%nErr:%s", slave.getName(), ex.getMessage()));
                succeded = false;
            }
        }

        return succeded;
    }

    /**
     * Copy files from master to slave and run slave scripts for the given
     * SetupConfigItem
     * 
     * @param installInfo SetupConfigItem to be deployed.
     * 
     * @throws InterruptedException For broken connection.
     * @throws IOException          Some IOError
     */
    private void doDeploy(SetupConfigItem installInfo) throws IOException, InterruptedException {
        EnvVars enviroment = SetupDeployer.createEnvVarsForComputer(this.slave);

        if (!StringUtils.isEmpty(installInfo.getPrepareScript())) {
            // If isn't empty script will execute on master
            validateResponse(SetupDeployer.executeScriptOnMaster(Components.listener, installInfo.getPrepareScript(),
                    enviroment));
            installInfo.setPrepareScriptExecuted(true);
        }

        // Copy files from master to slave (only if option contains some path)
        SetupDeployer.copyFiles(installInfo.getFilesDir(), remotePath);

        if (!StringUtils.isEmpty(installInfo.getCommandLine())) {
            // If we had slave script, will call now.
            validateResponse(
                    Utils.multiOsExecutor(Components.listener, installInfo.getCommandLine(), remotePath, enviroment));
        }
        // Add to cache in order to prevent reinstall this version.
        this.addCache(installInfo.remoteCache());
    }

    /**
     * Only works on Unix Operating Systems, this will use some unix commands to
     * clear temporally data
     * 
     * If file has more than 5 minutes in the last modified tag, it will be removed.
     * Only will remove files which name is jenkins
     * 
     * This function require at least (find) binary to be installed
     * 
     * @throws InterruptedException Broken pipe.
     * @throws IOException          IO error accessing remotePath
     * @throws AbortException       User close/Cancelled
     * 
     */
    public void clearTemporally() throws AbortException, IOException, InterruptedException {

        String clearTmp = "find /tmp -type f -atime +10 -delete -iname *jenkins*.sh -maxdepth 0";
        EnvVars environ = SetupDeployer.createEnvVarsForComputer(this.slave);

        if (this.remotePath.getRemote().startsWith("/")) {
            // Clear slave temporally data
            Components.info("Clearing temporally data on " + this.slave.getName());
            validateResponse(Utils.multiOsExecutor(Components.listener, clearTmp, this.remotePath, environ));
        }

        if (SystemUtils.IS_OS_UNIX) {
            // Clear master temporally data
            Components.info("Clearing temporally data on jenkins master");
            validateResponse(SetupDeployer.executeScriptOnMaster(Components.listener, clearTmp, environ));
        }
        Components.debug("Finished temporal data removed");
    }

    /**
     * Validate execution scripts code, in order to throw exception if not
     * 
     * @param r int result of launching method cointaining the script ExitCode. 0 =
     *          OK
     * 
     * @throws AbortException User close/Cancelled
     */
    private void validateResponse(int r) throws AbortException {
        if (r != 0) {
            Components.info("ScriptFailed " + r);
            throw new AbortException("script failed!");
        }
    }

    /**
     * Unlink remote file, after writing pending cache
     * 
     * @throws InterruptedException Broken pipe.
     * @throws IOException          IO error accessing slave's configFile
     */
    private void closeConfigStream() throws IOException, InterruptedException {
        if (getCache().size() > 0) {
            Components.debug(
                    String.format("Updating %s with%n%s", this.configFile, StringUtils.join(getCache(), "\r\n")));
            configFile.write(StringUtils.join(cache, this.remoteSeparator).trim(), "UTF-8");
        } else
            Components.debug("Nothing to update on slave, stream closed");

    }

    /**
     * From slave read cache file, and lock it.
     * 
     * @return List<String> Of already installed components from slaveConfigFile
     * 
     * @throws InterruptedException Broken pipe.
     * @throws IOException          IO error accessing remotePath
     */
    private List<String> createConfigStream() throws IOException, InterruptedException {
        return new ArrayList<String>(Arrays.asList(this.configFile.readToString().split(this.remoteSeparator)));

    }

    /**
     * Print only if debug enabled (debug purposes)
     * 
     * @param message String to print as DEBUG
     */
    public static void debug(String message) {
        if (Components.debugMode)
            Components.info(message);
    }

    /**
     * Global printer to return logger to master
     * 
     * @param message String to print as INFO
     */
    public static void info(String message) {
        if (Components.listener != null)
            Components.listener.getLogger().println(message);
    }

    /**
     * Just enable debug mode
     */
    public static void enableDebug() {
        Components.debugMode = true;
    }

    /**
     * Just disable debug Mode
     */
    public static void disableDebug() {
        Components.debugMode = false;
    }
}
