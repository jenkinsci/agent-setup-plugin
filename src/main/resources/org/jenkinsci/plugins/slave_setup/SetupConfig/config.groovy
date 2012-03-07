package org.jenkinsci.plugins.slave_setup.SetupConfig

def f = namespace(lib.FormTagLib)

f.section(title: _("Slave setup script")) {
    f.entry(title: _("Setup files directory"), field: "filesDir") {
        f.textbox()
    }

    f.entry(title: _("Command line"), field: "commandLine") {
        f.expandableTextbox()
    }


    f.entry(title: _("Deploy on save to all currently active Slaves"), field: "deployNow") {
        f.checkbox()
    }

    if(app.labels.size() > 1 || app.clouds.size() > 0) {
        f.entry(title: _("Label Expression"), field: "assignedLabelString") {
            f.textbox([autoCompleteDelimChar: " "])
        }
    }


    f.block("<b>(ATTENTION, slaves should be of one kind of System " +
                "as there is only one script for all!)</b>")

}

/*
  <!-- master/slave -->
  <j:if test="${app.assignedLabelString.size() gt 1 || app.clouds.size() gt 0 || (it.assignedLabel!=null and it.assignedLabel!=app.selfLabel)}">
    <f:optionalBlock name="hasSlaveAffinity" title="${%Restrict where this project can be run}" checked="${it.assignedLabel!=null}"
        help="/help/project-config/slave.html">
      <f:entry title="${%Label Expression}" field="assignedLabelString">
        <f:textbox autoCompleteDelimChar=" "/>
      </f:entry>
    </f:optionalBlock>
  </j:if>




*/