package org.jenkinsci.plugins.slave_setup;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.TaskListener;

/**
*  Vamos a cahcear el delmitador de master y el delimitador de cada nodo mientras estamos ahaciendo su despliegue
 */



public class Components {

    private final String FILENAME = "slave_setup.ini";

    // Temporall save variables until Class is finished
    private FilePath remotePath;
    private List<SetupConfigItem> configs; // I want this object to be null until constructor sets

    private static boolean newDeploy = false;
    private Computer slave;
    private List<String> cache;

    private static TaskListener listener;
    private static boolean debugMode = false;

    /**
     * 
     */
    public Components(FilePath remoteRootPath, Computer slave) {
        // store into memory full remote path of config File
        this.remotePath = remoteRootPath.child(FILENAME);
        try {
            if (!this.remotePath.exists()) {
                this.remotePath.write();
                Components.println("New config created on " + this.remotePath.getRemote());
                Components.newDeploy = true;
            }
        } catch (IOException e) {
            Components.println(e.getMessage());
        } catch (InterruptedException e) {
            Components.println(e.getMessage());
        }

        this.slave = slave;
        this.configs = SetupConfig.get().getSetupConfigItems();

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
        if (Components.newDeploy)
            cache = new ArrayList<String>();
        return cache;
    }

    private void setCache(List<String> newCache) {
        this.cache = newCache;
    }

    private void addCache(String component) {
        // Pongo getCache, para asegurarme de que si no hay cache cree una vacia para
        // evitar exception.
        if (!getCache().contains(component))
            cache.add(component);

        // We need to decide and advanced pattern to add and handle subversioning of
        // saves
        // When we have one, I can continue adding pseudo to this setCache

    }

    public boolean doSetup() throws AbortException {
        // Here if newDeploy is false, will read at first time the remote config file.
        // and store it into cache trhougth overriding cache
        // Teorically this is first method that will be executed under Components Flow.
        if (!Components.newDeploy) {
            Components.debug("Isn't first installation getting cache");
            // At this point at some time, we do some setup in the past, as well,
            // need to get installed components from slave,
            cache = createConfigStream();

            Components.debug("Given cache is " + String.join(System.getProperty("line.separator"), cache));
        }

        // At this point if cache was required to be updated cache contain installed
        // components

        // From item we get tagPatter, and execute matches on Slave.TagsString

        // Now, we will iterate all SetupConfigItems
        // if item.tagPattern match with Slave.TagString
        for (SetupConfigItem item : configs) {
            if (Utils.labelMatches(item.getAssignedLabelString(), slave)) {
                if (!cache.contains(item.remoteCache())) {
                    this.doDeploy(item);

                    for (int i = 0; i < cache.size(); i++) {
                        if (cache.get(i).contains(item.getAssignedLabelString()))
                            cache.set(i, item.remoteCache());

                    }

                }
            }
        }
        closeConfigStream();
        return false;
    }

    private void doDeploy(SetupConfigItem installInfo) throws AbortException {
        EnvVars enviroment = SetupDeployer.createEnvVarsForComputer(this.slave);
        // At first point will create our EnvVars for computers
        // createEnvVarsforComputer(Computer)
        // Entire scripts flow execution
        // if prepare is empty ignore masterExecute
        if (!StringUtils.isEmpty(installInfo.getPrepareScript())) 
            validateResponse(SetupDeployer.executeScriptOnMaster(installInfo.getPrepareScript(), this.slave,
                    this.listener, enviroment));
            // Maybe we need to set prepareScript to true if was installed, but Aaron think
            // that raise exception is better.

            // First script es prepare under master

            // continue with deploy files to slave

           // Execute script in slave
        this.cache.add(installInfo.remoteCache());
        
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
            remotePath.write(StringUtils.join(cache, System.getProperty("line.separator")), "UTF-8");
            // FileWriter fileHdl = new FileWriter(remotePath.getRemote(), false);
            // BufferedWriter writer = new BufferedWriter(fileHdl);
            // writer.write(StringUtils.join(cache, System.getProperty("line.separator")));

        } catch (Exception ex) {
            Components.println("Components:closeConfigStream" + ex.getMessage());
        }
    }

    private List<String> getComponents(String contentString){
        return new ArrayList(Arrays.asList(contentString.split(System.getProperty("line.separator"))));
    }

    // private List<String> getComponents(String readerStream) {
    //     ArrayList<String> components = new ArrayList<String>();
    //     try {
    //         String line;
    //         while ((line = readerStream.readLine()) != null) {
    //             components.add(line);
    //         }

    //     } catch (Exception ex) {
    //         Components.println("Components:getComponents::" + ex.getMessage());
    //     }
    //     return components;
    // }

    private List<String> createConfigStream() {
        // Open Stream to config file, read it, close stream, but not connection
        //
        List<String> components;
        try {
            components = getComponents(remotePath.readToString());
            // InputStream fileContent = remotePath.read();
            // BufferedReader readStream = new BufferedReader(fileContent);
            // components = getComponents(readStream);
            // readStream.close();
            // fileContent.close();
        } catch (Exception ex) {
            components = null;
            Components.println("Components:createConfigStream::" + ex.getMessage());
        }
        return components;
    }


    /**
     * @deprecated use Components.info or Components.debug instead will handle
     *             stdout if debugMode enabled or not.
     * Actually this is redirected to Components.info.
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



