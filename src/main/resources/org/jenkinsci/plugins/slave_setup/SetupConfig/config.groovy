package org.jenkinsci.plugins.slave_setup.SetupConfig

def f=namespace(lib.FormTagLib)

f.section(title:_("Slave setup script")) {
    f.entry(title:_("Setup files directory"), field:"filesDir") {
        f.textbox()
    }
    f.entry(title:_("Command line"), field:"commandLine") {
        f.expandableTextbox()
    }
    f.entry(title:_("Deploy now to all currently active Slaves"), field:"deployNow") {
       f.checkbox()
    }
}