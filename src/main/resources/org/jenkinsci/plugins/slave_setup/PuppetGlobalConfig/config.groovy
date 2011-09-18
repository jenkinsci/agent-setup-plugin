package org.jenkinsci.plugins.slave_setup.PuppetGlobalConfig

def f=namespace(lib.FormTagLib)

f.section(title:_("Slave setup script")) {
    f.entry(title:_("Script files directory"), field:"scriptDir") {
        f.textbox()
    }
    f.entry(title:_("Command line"), field:"commandLine") {
        f.expandableTextbox()
    }
}