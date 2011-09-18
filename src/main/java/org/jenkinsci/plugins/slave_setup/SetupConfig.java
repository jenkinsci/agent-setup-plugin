package org.jenkinsci.plugins.slave_setup;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;

/**
 * Keeps track of the configuration of slave_setup execution.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension
public class SetupConfig extends GlobalConfiguration {
    private File scriptDir;
    private String commandLine;

    public SetupConfig() {
        load();
    }

    public File getScriptDir() {
        return scriptDir;
    }

    public String getCommandLine() {
        return commandLine;
    }

    public void setScriptDir(File scriptDir) {
        this.scriptDir = scriptDir;
    }

    public void setCommandLine(String commandLine) {
        this.commandLine = commandLine;
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
}
