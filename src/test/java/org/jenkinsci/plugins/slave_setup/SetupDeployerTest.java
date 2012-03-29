package org.jenkinsci.plugins.slave_setup;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.*;
import hudson.model.labels.LabelAtom;
import hudson.slaves.DumbSlave;
import hudson.slaves.OfflineCause;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.localizer.Localizable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Frederik Fromm
 */
public class SetupDeployerTest extends HudsonTestCase {

    private List<DumbSlave> slaves;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.slaves = new ArrayList<DumbSlave>();

        Label label1 = new LabelAtom("foo");
        Label label2 = new LabelAtom("foo || bar");
        Label label3 = new LabelAtom("bar");
        
        this.slaves.add(this.createOnlineSlave(label1));
        this.slaves.add(this.createOnlineSlave(label2));
        this.slaves.add(this.createOnlineSlave(label3));
    }

    @Test
    public void testGetAllActiveSlaves() throws Exception {
        SetupDeployer setupDeployer = new SetupDeployer();

        List<Computer> allActiveSlaves = setupDeployer.getAllActiveSlaves();
        assertNotNull(allActiveSlaves);
        assertEquals(3, allActiveSlaves.size());

        this.slaves.get(1).getComputer().disconnect(OfflineCause.create(Messages._Hudson_NodeBeingRemoved()));
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        allActiveSlaves = setupDeployer.getAllActiveSlaves();
        assertEquals(2, allActiveSlaves.size());

        this.slaves.get(0).getComputer().disconnect(OfflineCause.create(Messages._Hudson_NodeBeingRemoved()));
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        allActiveSlaves = setupDeployer.getAllActiveSlaves();
        assertEquals(1, allActiveSlaves.size());
    }

    @Test
    public void testDeployToComputer() {
        this.slaves.get(0).getComputer().disconnect(OfflineCause.create(Messages._Hudson_NodeBeingRemoved()));
        this.slaves.get(2).getComputer().disconnect(OfflineCause.create(Messages._Hudson_NodeBeingRemoved()));

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        SetupDeployer setupDeployer = new SetupDeployer();
        List<Computer> activeSlaves = setupDeployer.getAllActiveSlaves();
        assertEquals(1, activeSlaves.size());

        Computer slave = activeSlaves.get(0);

        FilePath root = slave.getNode().getRootPath();

        TaskListener taskListener = this.createTaskListener();

        try {

            setupDeployer.deployToComputer(slave, root, taskListener, createConfig(null));
            FilePath[] files = slave.getNode().getRootPath().list("setup.txt");
            assertEquals(1, files.length);

        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }


    @Test
    public void testDeployToComputers() {

        SetupDeployer setupDeployer = new SetupDeployer();
        List<Computer> activeSlaves = setupDeployer.getAllActiveSlaves();

        setupDeployer.deployToComputers(activeSlaves, createConfig(null));

        for (Computer activeSlave : activeSlaves) {
            try {
                FilePath[] files = activeSlave.getNode().getRootPath().list("setup.txt");

                assertEquals(1, files.length);
            } catch (IOException e) {
                e.printStackTrace();
                fail(e.getMessage());
            } catch (InterruptedException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
        }
    }

    @Test
    public void testDeployToComputers2() {

        SetupDeployer setupDeployer = new SetupDeployer();
        List<Computer> activeSlaves = setupDeployer.getAllActiveSlaves();
        Label fooLabel = Label.get("foo");

        setupDeployer.deployToComputers(activeSlaves, createConfig("foo"));

        for (Computer activeSlave : activeSlaves) {
            try {
                FilePath[] files = activeSlave.getNode().getRootPath().list("setup.txt");

                if(activeSlave.getNode().getAssignedLabels().contains(fooLabel)) {
                    assertEquals(1, files.length);
                } else {
                    assertEquals(0, files.length);
                }

            } catch (IOException e) {
                e.printStackTrace();
                fail(e.getMessage());
            } catch (InterruptedException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }
        }
    }

    private SetupConfig createConfig(String label) {
        SetupConfig setupConfig = new SetupConfig();
        File setupFiles = new File("src/test/resources/files");
        assertTrue(setupFiles.canRead() && setupFiles.isDirectory());
        SetupConfigItem item = new SetupConfigItem();
        item.setFilesDir(setupFiles);
        if(StringUtils.isNotBlank(label)) {
            item.setAssignedLabelString(label);
        }
        
        List<SetupConfigItem> setupConfigItems = new ArrayList<SetupConfigItem>();
        setupConfigItems.add(item);

        setupConfig.setSetupConfigItems(setupConfigItems);

        return setupConfig;
    }
}
