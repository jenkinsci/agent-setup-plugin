package org.jenkinsci.plugins.slave_setup;

import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.Messages;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.DumbSlave;
import hudson.slaves.OfflineCause;
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
        this.slaves.add(this.createOnlineSlave());
        this.slaves.add(this.createOnlineSlave());
        this.slaves.add(this.createOnlineSlave());
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

            setupDeployer.deployToComputer(slave, root, taskListener, createConfig());
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

        TaskListener taskListener = this.createTaskListener();

        setupDeployer.deployToComputers(activeSlaves, createConfig());

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

    private SetupConfig createConfig() {
        SetupConfig setupConfig = new SetupConfig();
        File setupFiles = new File("src/test/resources/files");
        assertTrue(setupFiles.canRead() && setupFiles.isDirectory());
        setupConfig.setFilesDir(setupFiles);
        return setupConfig;
    }
}
