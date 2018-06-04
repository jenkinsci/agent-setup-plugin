package org.jenkinsci.plugins.slave_setup;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.kenai.jffi.Util;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.TaskListener;

/**
 * Vamos a cahcear el delmitador de master y el delimitador de cada nodo
 * mientras estamos ahaciendo su despliegue
 */

public class Components {

    private final String FILENAME = "slave_setup.ini";

    // Temporall save variables until Class is finished
    private FilePath remotePath;
    private List<SetupConfigItem> configs; // I want this object to be null until constructor sets
    private Computer slave;
    private static TaskListener listener;

    private boolean newDeploy = false;
    private List<String> cache;
    private static boolean debugMode = false;
    private FilePath configFile;
    private String localSeparator;
    private String remoteSeparator;

    /**
     * 
     */
    public Components(FilePath remoteRootPath, Computer slave) {
        // store into memory full remote path of config File
        this.remotePath = remoteRootPath;
        this.configFile = this.remotePath.child(FILENAME);
        try {
            if (!this.configFile.exists()) {
                this.configFile.write();
                Components.println("New config created on " + this.configFile.getRemote());
                newDeploy = true;
            }
        } catch (IOException e) {
            Components.info(e.getMessage());
        } catch (InterruptedException e) {
            Components.info(e.getMessage());
        }

        this.remoteSeparator = Utils.osLineSeparator(this.remotePath.toString());
        this.slave = slave;
        this.configs = SetupConfig.get().getSetupConfigItems();
        this.localSeparator = Utils.osLineSeparator(SystemUtils.IS_OS_UNIX);

    }

    // If we want verbose just set listener and verbose will work
    public static void setLogger(TaskListener listener) {
        Components.listener = listener;
    }

    // Handle logger and write mensagge into it if exists

    // When you want to access cache you can access with this function, making
    // always one object,
    // with no elements if cache is empty This will enable to work with cache always
    // without errors.
    private List<String> getCache() {
        if (cache == null)
            cache = new ArrayList<String>();
        return cache;
    }

    private void setCache(List<String> newCache) {
        this.cache = newCache;
    }

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

    public boolean doSetup() throws AbortException, IOException, InterruptedException {
        // Here if newDeploy is false, will read at first time the remote config file.
        // and store it into cache trhougth overriding cache
        // Teorically this is first method that will be executed under Components Flow.
        if (!newDeploy) {
            Components.debug("Isn't first installation getting cache");
            // At this point at some time, we do some setup in the past, as well,
            // need to get installed components from slave,
            Components.info("Updating existing installations for " + slave.getName());
            cache = createConfigStream();

            Components.debug("Given cache contains this lines:\r\n " + String.join(remoteSeparator, cache));
        } else
            Components.info("Executing first install for " + slave.getName());

        // Now, we will iterate all SetupConfigItems
        // if item.tagPattern match with Slave.TagString
        for (SetupConfigItem item : configs) {
            if (Utils.labelMatches(item.getAssignedLabelString(), slave)) {

                if (!getCache().contains(item.remoteCache())) {
                    Components.info("Start setup for " + item.getAssignedLabelString());
                    this.doDeploy(item);
                    Components.info("Setup for " + item.getAssignedLabelString() + " succeded");
                } else
                    Components.info(String.format("%s slave have last version of %s", slave.getName(),
                            item.getAssignedLabelString()));
            }
        }
        closeConfigStream();
        return false;
    }

    private void doDeploy(SetupConfigItem installInfo) throws IOException, InterruptedException {
        EnvVars enviroment = SetupDeployer.createEnvVarsForComputer(this.slave);
        // At first point will create our EnvVars for computers
        // createEnvVarsforComputer(Computer)
        // Entire scripts flow execution
        // if prepare is empty ignore masterExecute
        if (!StringUtils.isEmpty(installInfo.getPrepareScript())) {
            validateResponse(SetupDeployer.executeScriptOnMaster(installInfo.getPrepareScript(), this.slave,
                    this.listener, enviroment));
            installInfo.setPrepareScriptExecuted(true);
        }

        // copyFiles will handle if emtpy or not is the value
        SetupDeployer.copyFiles(installInfo.getFilesDir(), remotePath);

        if (!StringUtils.isEmpty(installInfo.getCommandLine())) {
            validateResponse(Utils.multiOsExecutor(listener, installInfo.getCommandLine(), remotePath, enviroment));
        }

        // Maybe we need to set prepareScript to true if was installed, but Aaron think
        // that raise exception is better.

        // First script es prepare under master

        // continue with deploy files to slave

        // Execute script in slave
        this.addCache(installInfo.remoteCache());

    }

    private void validateResponse(int r) throws AbortException {
        if (r != 0) {
            Components.println("ScriptFailed " + r);
            throw new AbortException("script failed!");
        }
    }

    private void closeConfigStream() {
        // At this point will close a stream and write all pending data into it.
        // after data is writen to remote disk close connection (if need it)
        try {
            Components.debug(
                    String.format("Updating %s with\n%s", this.configFile, StringUtils.join(getCache(), "\r\n")));
            configFile.write(StringUtils.join(cache, this.remoteSeparator), "UTF-8");
            // FileWriter fileHdl = new FileWriter(configFile.getRemote(), false);
            // BufferedWriter writer = new BufferedWriter(fileHdl);
            // writer.write(StringUtils.join(cache, System.getProperty("line.separator")));

        } catch (Exception ex) {
            Components.println("Components:closeConfigStream" + ex.getMessage());
        }
    }

    private List<String> createConfigStream() {
        // Open Stream to config file, read it, close stream, but not connection
        //
        try {
            return new ArrayList(Arrays.asList(this.configFile.readToString().split(this.remoteSeparator)));

        } catch (Exception ex) {
            Components.info("Components:createConfigStream::" + ex.getMessage());
            return null;
        }
    }

    /**
     * @deprecated use Components.info or Components.debug instead will handle
     *             stdout if debugMode enabled or not. Actually this is redirected
     *             to Components.info.
     */
    @Deprecated
    public static void println(String message) {
        Components.info(message);
    }

    public static void debug(String message) {
        if (Components.debugMode)
            Components.info(message);
    }

    public static void info(String message) {
        if (Components.listener != null)
            Components.listener.getLogger().println(message);
    }

    public static void enableDebug() {
        Components.debugMode = true;
    }

    public static void disableDebug() {
        Components.debugMode = false;
    }
}
