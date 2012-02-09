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

    f.block("<b>(ATTENTION, slaves should be of one kind of System " +
                "as there is only one script for all!)</b>")

}