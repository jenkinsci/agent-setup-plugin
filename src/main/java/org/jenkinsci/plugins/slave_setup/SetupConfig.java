package org.jenkinsci.plugins.slave_setup;

import hudson.Extension;
import hudson.Util;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;

/**
 * Keeps track of the configuration of slave_setup execution.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
public class SetupConfig extends GlobalConfiguration {
    private File filesDir;
    private String commandLine;

    public SetupConfig() {
        load();
    }

    public File getFilesDir() {
        return filesDir;
    }

    public String getCommandLine() {
        return commandLine;
    }

    public void setFilesDir(File filesDir) {
        if (filesDir.getPath().length()==0)     filesDir=null;
        this.filesDir = filesDir;
    }

    public void setCommandLine(String commandLine) {
        this.commandLine = Util.fixEmpty(commandLine);
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        req.bindJSON(this,json);
        save();
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
