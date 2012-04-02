package org.jenkinsci.plugins.slave_setup;

import antlr.ANTLRException;
import hudson.Util;
import hudson.model.labels.LabelAtom;
import hudson.model.labels.LabelExpression;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;

public class SetupConfigItem {
    
    private String prepareScript;
    private File filesDir;
    private String commandLine;
    private boolean deployNow;
    private String assignedLabelString;

    @DataBoundConstructor
    public SetupConfigItem(String prepareScript, File filesDir, String commandLine, boolean deployNow, String assignedLabelString) {
        this.prepareScript = prepareScript;
        this.filesDir = filesDir;        
        this.commandLine = commandLine;
        this.deployNow = deployNow;
        this.assignedLabelString = assignedLabelString;
    }

    public SetupConfigItem() {
    }

    public String getPrepareScript() {
        return prepareScript;
    }

    public void setPrepareScript(String prepareScript) {
        this.prepareScript = prepareScript;
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
        if (filesDir.getPath().length() == 0) filesDir = null;
        this.filesDir = filesDir;
    }

    public void setCommandLine(String commandLine) {
        this.commandLine = Util.fixEmpty(commandLine);
    }

    public void setDeployNow(boolean deployNow) {
        this.deployNow = deployNow;
    }

    /**
     * Gets the textual representation of the assigned label as it was entered by the user.
     */
    public String getAssignedLabelString() {
        if (StringUtils.isEmpty(this.assignedLabelString)) {
            return "";
        }

        try {
            LabelExpression.parseExpression(this.assignedLabelString);
            return this.assignedLabelString;
        } catch (ANTLRException e) {
            // must be old label or host name that includes whitespace or other unsafe chars
            return LabelAtom.escape(this.assignedLabelString);
        }
    }

    public void setAssignedLabelString(String assignedLabelString) {
        this.assignedLabelString = assignedLabelString;
    }
}