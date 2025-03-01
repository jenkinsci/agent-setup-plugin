package org.jenkinsci.plugins.slave_setup;

import antlr.ANTLRException;
import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.labels.LabelAtom;
import hudson.model.labels.LabelExpression;
import java.io.File;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Represents a setup config for one set of labels. It may have its own prepare script, files to copy and command line.
 */
public class SetupConfigItem extends AbstractDescribableImpl<SetupConfigItem> {

    public static final String DELIMITER = "¼";

    /**
     * the prepare script code
     */
    private String prepareScript;

    /**
     * the directory to get the content to copy from
     */
    private File filesDir;

    /**
     * the command line code
     */
    private String commandLine;

    /**
     * set to true to execute setup script on save of the main jenkins configuration page
     */
    private boolean deployNow;

    /**
     * jenkins label to be assigned to this setup config
     */
    private String assignedLabelString;

    /**
     * true if prepare script was executed successfully
     */
    private boolean prepareScriptExecuted = false;

    /**
     * Constructor uesd to create the setup config instance
     *
     */
    @DataBoundConstructor
    public SetupConfigItem(
            String prepareScript, File filesDir, String commandLine, boolean deployNow, String assignedLabelString) {
        this.prepareScript = prepareScript;
        this.filesDir = filesDir;
        this.commandLine = commandLine;
        this.deployNow = deployNow;
        this.assignedLabelString = assignedLabelString;
    }

    /**
     * Default constructor
     */
    public SetupConfigItem() {}

    /**
     * Returns the prepare script code.
     *
     * @return the prepare script code
     */
    public String getPrepareScript() {
        return prepareScript;
    }

    /**
     * Sets the prepare script code
     *
     * @param prepareScript the script code as string
     */
    public void setPrepareScript(String prepareScript) {
        this.prepareScript = prepareScript;
    }

    /**
     * Returns the directory containing the setup relevant files and sub directories
     *
     * @return the directory as File
     */
    public File getFilesDir() {
        return filesDir;
    }

    /**
     * Returns the command line code.
     *
     * @return the command line code
     */
    public String getCommandLine() {
        return commandLine;
    }

    /**
     * Returns true if the setup config should be deployed on save of the jenkins config page.
     *
     * @return true if the setup config should be deployed on save of the jenkins config page
     */
    public boolean getDeployNow() {
        return this.deployNow;
    }

    /**
     * Sets the files dir.
     *
     * @param filesDir firectory to copy the setup files and sub directories from.
     */
    public void setFilesDir(File filesDir) {
        if (filesDir.getPath().length() == 0) {
            this.filesDir = null;
        } else {
            this.filesDir = filesDir;
        }
    }

    /**
     * sets the command line code.
     *
     * @param commandLine the command line code
     */
    public void setCommandLine(String commandLine) {
        this.commandLine = Util.fixEmpty(commandLine);
    }

    /**
     * sets the deploy flag.
     *
     * @param deployNow the deploy flag
     */
    public void setDeployNow(boolean deployNow) {
        this.deployNow = deployNow;
    }

    /**
     * Returns the prepare script executed status.
     * @return the prepare script executed status
     */
    public boolean isPrepareScriptExecuted() {
        return this.prepareScriptExecuted;
    }

    /**
     * sets the prepare script executed status.
     * @param prepareScriptExecuted the prepare script executed status
     */
    public void setPrepareScriptExecuted(boolean prepareScriptExecuted) {
        this.prepareScriptExecuted = prepareScriptExecuted;
    }

    /**
     * Gets the textual representation of the assigned label as it was entered by the user.
     *
     * @return assigned label as string
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

    /**
     * sets the assigned slave's labels
     *
     * @param assignedLabelString labels to be assigned to the agent
     */
    public void setAssignedLabelString(String assignedLabelString) {
        this.assignedLabelString = assignedLabelString;
    }

    @Extension
    public static class SetupConfigItemDescriptor extends Descriptor<SetupConfigItem> {

        @Override
        public String getDisplayName() {
            return "";
        }
    }

    /**
     * Every ConfigItem have a unique identifier for content which will match with slave hashCode
     *
     * @return unic value per all scripts
     */
    public int hashCode() {
        return (prepareScript + filesDir + commandLine).hashCode();
    }

    /**
     * Useful function to get same name as needs to be under slave cache.
     *
     * @return String containing tag + DELIMITER +  hashCode()
     */
    public String remoteCache() {
        return this.assignedLabelString + SetupConfigItem.DELIMITER + this.hashCode();
    }
}
