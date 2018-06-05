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
import hudson.model.TaskListener;

/**
 * Vamos a cachear el delmitador de master y el delimitador de cada nodo
 * mientras estamos ahaciendo su despliegue
 * 
 * We are going to store in cache the master delimiter and each node delimiter
 * while they are deployed.
 * @author ByteHeed
 */

public class Components {

    private final String FILENAME = "slave_setup.ini";

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
     * TODO: Add contruction description Check if config exists and create, then
     * store al remote objects to be used at deploy time
     * 
     * @throws InterruptedException
     * @throws IOException
     * 
     */
    public Components(FilePath remoteRootPath, Computer slave) throws IOException, InterruptedException {
        this.remotePath = remoteRootPath;

        this.configFile = this.remotePath.child(FILENAME);
        if (!this.configFile.exists()) {
            this.configFile.write();
            Components.debug("New config created on " + this.configFile.getRemote());
        }

        this.remoteSeparator = Utils.osLineSeparator(this.remotePath.getRemote());
        this.slave = slave;
        this.configs = SetupConfig.get().getSetupConfigItems();

    }

    /**
     * Check if slave contains some setup configured false, means that is first time
     * connection or any designed setup // If we want verbose just set listener and
     * verbose will work
     */
    public boolean newDeploy() {
        return this.getCache().size() > 0;
    }

    /**
     * Required method, to define logging procedures
     */
    public static void setLogger(TaskListener listener) {
        Components.listener = listener;
    }

    /**
     * Returns current cache, if null will create one empty
     */
    private List<String> getCache() {
        if (cache == null)
            cache = new ArrayList<String>();
        return cache;
    }

    /**
     * Append element to cache, and prevent to create duplicateds with diferent
     * versions or same
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
     * Slave setup flow, here, iterate all SetupItems and if not installed jet, will
     * call doDeploy
     */
    public boolean doSetup() throws AbortException, IOException, InterruptedException {
        if (!this.newDeploy()) {
            // If slave contains some setups, will read cache data from slave disk

            Components.info("Updating existing installations for " + slave.getName());
            cache = createConfigStream();
            Components.debug("Given cache contains this lines:\r\n " + String.join(remoteSeparator, cache));
        } else
            Components.info("Executing first install for " + slave.getName());

        for (SetupConfigItem item : configs) {
            if (Utils.labelMatches(item.getAssignedLabelString(), slave)) {

                if (!getCache().contains(item.remoteCache())) {
                    Components.info("Installing " + item.getAssignedLabelString());
                    this.doDeploy(item);
                    Components.info("Install " + item.getAssignedLabelString() + " succeded");
                } else
                    Components.info(String.format("%s slave have last version of %s", slave.getName(),
                            item.getAssignedLabelString()));
            }
        }
        closeConfigStream();
        return false;
    }

    /**
     * Entire Setup flow for one Item First, will execute scripts on master Second,
     * will deploy files to slave Third, Will run slave scrfipts
     * 
     * @throws InterruptedException
     * @throws IOException
     */
    private void doDeploy(SetupConfigItem installInfo) throws IOException, InterruptedException {
        EnvVars enviroment = SetupDeployer.createEnvVarsForComputer(this.slave);

        if (!StringUtils.isEmpty(installInfo.getPrepareScript())) {
            // If isn't empty script will execute on master
            validateResponse(SetupDeployer.executeScriptOnMaster(installInfo.getPrepareScript(), this.slave,
                    Components.listener, enviroment));
            installInfo.setPrepareScriptExecuted(true);
        }

        // Copy files from master to slave (only if option contains some path)
        SetupDeployer.copyFiles(installInfo.getFilesDir(), remotePath);

        if (!StringUtils.isEmpty(installInfo.getCommandLine())) {
            // If we had slave script, will cal now.
            validateResponse(
                    Utils.multiOsExecutor(Components.listener, installInfo.getCommandLine(), remotePath, enviroment));
        }
        // Add to cache in order to prevent reinstall this version.
        this.addCache(installInfo.remoteCache());
    }

    /**
     * TODO: Update-me Only works on Unix Operating Systems, this will use some unix
     * commands to clear temporally data
     * 
     * If file had more than 5 minutes in last modified tag, from OS, will be
     * removed Only will remove files containing jenkins in name
     * 
     * This function require at least (find) binary to be installed
     * 
     * @throws InterruptedException
     * @throws IOException
     * @throws AbortException
     * 
     */
    public void clearTemporally() throws AbortException, IOException, InterruptedException {

        String clearTmp = "find /tmp -type f -atime +10 -delete -iname *jenkins*.sh -maxdepth 0";
        EnvVars environ = SetupDeployer.createEnvVarsForComputer(this.slave);

        if (this.remotePath.toString().startsWith("/")) {
            // Clear slave temporally data
            Components.info("Clearing temporally data on " + this.slave.getName());
            validateResponse(Utils.multiOsExecutor(Components.listener, clearTmp, this.remotePath, environ));
        }

        if (SystemUtils.IS_OS_UNIX) {
            // Clear master temporally data
            Components.info("Clearing temporally data on jenkins master");
            validateResponse(SetupDeployer.executeScriptOnMaster(clearTmp, this.slave, Components.listener, environ));
        }
        Components.debug("Finished temporal data removed");
    }

    /**
     * Validate execution scripts code, in order to throw exception if not
     * 
     * @throws AbortException
     */
    private void validateResponse(int r) throws AbortException {
        if (r != 0) {
            Components.info("ScriptFailed " + r);
            throw new AbortException("script failed!");
        }
    }

    /**
     * Unlink remote file, after write pending cache
     * 
     * @throws InterruptedException
     * @throws IOException
     */
    private void closeConfigStream() throws IOException, InterruptedException {
        Components.debug(String.format("Updating %s with\n%s", this.configFile, StringUtils.join(getCache(), "\r\n")));
        configFile.write(StringUtils.join(cache, this.remoteSeparator), "UTF-8");
    }

    /**
     * From slave read cache file, and lock it.
     * 
     * @throws InterruptedException
     * @throws IOException
     */
    private List<String> createConfigStream() throws IOException, InterruptedException {
        return new ArrayList<String>(Arrays.asList(this.configFile.readToString().split(this.remoteSeparator)));

    }

    /**
     * Print only if debug enabled (debug purposes)
     */
    public static void debug(String message) {
        if (Components.debugMode)
            Components.info(message);
    }

    /**
     * Global printer to return logger to master
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
