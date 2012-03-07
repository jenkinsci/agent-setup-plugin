package org.jenkinsci.plugins.slave_setup;

import antlr.ANTLRException;
import hudson.Extension;
import hudson.Util;
import hudson.model.*;
import hudson.model.labels.LabelAtom;
import hudson.model.labels.LabelExpression;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    private String assignedLabelString;

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

    /**
     * Gets the textual representation of the assigned label as it was entered by the user.
     */
    public String getAssignedLabelString() {
        if(StringUtils.isEmpty(this.assignedLabelString)) {
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

    public AutoCompletionCandidates doAutoCompleteAssignedLabelString(@QueryParameter String value) {
        AutoCompletionCandidates c = new AutoCompletionCandidates();
        Set<Label> labels = Jenkins.getInstance().getLabels();
        List<String> queries = new AutoCompleteSeeder(value).getSeeds();

        for (String term : queries) {
            for (Label l : labels) {
                if (l.getName().startsWith(term)) {
                    c.add(l.getName());
                }
            }
        }
        return c;
    }

    public FormValidation doCheckFilesDir(@QueryParameter String value) {
        Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
        if (Util.fixEmpty(value)==null)
            return FormValidation.ok(); // no value
        if (!new File(value).isDirectory())
            return FormValidation.error("Directory "+value+" doesn't exist");
        return FormValidation.ok();
    }

    public FormValidation doCheckAssignedLabelString(@QueryParameter String value) {
        if (Util.fixEmpty(value)==null)
            return FormValidation.ok(); // nothing typed yet
        try {
            Label.parseExpression(value);
        } catch (ANTLRException e) {
            return FormValidation.error(e,
                    Messages.AbstractProject_AssignedLabelString_InvalidBooleanExpression(e.getMessage()));
        }
        Label l = Jenkins.getInstance().getLabel(value);
        if (l.isEmpty()) {
            for (LabelAtom a : l.listAtoms()) {
                if (a.isEmpty()) {
                    LabelAtom nearest = LabelAtom.findNearest(a.getName());
                    return FormValidation.warning(Messages.AbstractProject_AssignedLabelString_NoMatch_DidYouMean(a.getName(),nearest.getDisplayName()));
                }
            }
            return FormValidation.warning(Messages.AbstractProject_AssignedLabelString_NoMatch());
        }
        return FormValidation.ok();
    }

    /**
     * Utility class for taking the current input value and computing a list
     * of potential terms to match against the list of defined labels.
     */
    static class AutoCompleteSeeder {
        private String source;

        AutoCompleteSeeder(String source) {
            this.source = source;
        }

        List<String> getSeeds() {
            ArrayList<String> terms = new ArrayList<String>();
            boolean trailingQuote = source.endsWith("\"");
            boolean leadingQuote = source.startsWith("\"");
            boolean trailingSpace = source.endsWith(" ");

            if (trailingQuote || (trailingSpace && !leadingQuote)) {
                terms.add("");
            } else {
                if (leadingQuote) {
                    int quote = source.lastIndexOf('"');
                    if (quote == 0) {
                        terms.add(source.substring(1));
                    } else {
                        terms.add("");
                    }
                } else {
                    int space = source.lastIndexOf(' ');
                    if (space > -1) {
                        terms.add(source.substring(space+1));
                    } else {
                        terms.add(source);
                    }
                }
            }

            return terms;
        }
    }
}
