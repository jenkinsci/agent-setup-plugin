package org.jenkinsci.plugins.slave_setup;

import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import hudson.util.LogTaskListener;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.util.List;

/**
 * Keeps track of the configuration of slave_setup execution.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
public class SetupConfig extends GlobalConfiguration {
    private File filesDir;
    private String commandLine;
    private boolean deployNow;

    public SetupConfig() {
        load();
    }

    public File getFilesDir() {
        return filesDir;
    }

    public String getCommandLine() {
        return commandLine;
    }
    
    public boolean getDeployNow() {
        return this.deployNow;
    }

    public void setFilesDir(File filesDir) {
        if (filesDir.getPath().length()==0)     filesDir=null;
        this.filesDir = filesDir;
    }

    public void setCommandLine(String commandLine) {
        this.commandLine = Util.fixEmpty(commandLine);
    }
    
    public void setDeployNow(boolean deployNow) {
        this.deployNow = deployNow;
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        req.bindJSON(this,json);
        save();

        if(this.deployNow) {
            SetupDeployer deployer = new SetupDeployer();
            
            List<Computer> allActiveSlaves = deployer.getAllActiveSlaves();

            deployer.deployToComputers(allActiveSlaves, this);
        }

        return true;
    }

    public static SetupConfig get() {
        return GlobalConfiguration.all().get(SetupConfig.class);
    }

    public FormValidation doCheckFilesDir(@QueryParameter String value) {
        Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
        if (Util.fixEmpty(value)==null)
            return FormValidation.ok(); // no value
        if (!new File(value).isDirectory())
            return FormValidation.error("Directory "+value+" doesn't exist");
        return FormValidation.ok();
    }


}
