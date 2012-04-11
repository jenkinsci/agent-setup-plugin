package org.jenkinsci.plugins.slave_setup;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.*;
import hudson.model.labels.LabelAtom;
import hudson.slaves.DumbSlave;
import hudson.slaves.OfflineCause;
import hudson.slaves.SlaveComputer;
import hudson.util.IOUtils;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;
import org.jvnet.localizer.Localizable;

import java.io.File;
import java.io.FileInputStream;
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
        
        this.slaves.add(this.createSlave("foo", null));
        this.slaves.add(this.createSlave("foo bar", null));
        this.slaves.add(this.createSlave("bar", null));

        for(int i = 0; i < 3; i++) {
            SlaveComputer c = this.slaves.get(i).getComputer();
            c.connect(false).get(); // wait until it's connected
            if(c.isOffline()) {
                fail("Slave failed to go online: "+c.getLog());
            }
        }
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

    @Test
    public void testExecutePrepareScripts() {
        SetupConfig setupConfig = new SetupConfig();

        File sci1Files = prepSCIFile("sci1");
        SetupConfigItem sci1 = new SetupConfigItem();
        sci1.setPrepareScript("echo \"prep01=v01\" > sci1.properties");
        sci1.setAssignedLabelString("foo");
        sci1.setFilesDir(sci1Files);
        setupConfig.getSetupConfigItems().add(sci1);

        File sci2Files = prepSCIFile("sci2");
        SetupConfigItem sci2 = new SetupConfigItem();
        sci2.setPrepareScript("echo \"prep02=v02\" > sci2.properties");
        sci2.setAssignedLabelString("bar");
        sci2.setFilesDir(sci2Files);
        setupConfig.getSetupConfigItems().add(sci2);

        File sci3Files = prepSCIFile("sci3");
        SetupConfigItem sci3 = new SetupConfigItem();
        sci3.setPrepareScript("echo \"prep03=v03\" > sci3.properties");
        sci3.setAssignedLabelString("foo || bar");
        sci3.setFilesDir(sci3Files);
        setupConfig.getSetupConfigItems().add(sci3);

        TaskListener taskListener = this.createTaskListener();

        SetupDeployer setupDeployer = new SetupDeployer();
        int preparedStatus = setupDeployer.executePrepareScripts(null, setupConfig, taskListener);
        assertEquals(0, preparedStatus);

        File[] expectedSci1Files = sci1Files.listFiles();
        assertEquals(1, expectedSci1Files.length);
        try {
            String line = IOUtils.readFirstLine(new FileInputStream(expectedSci1Files[0]), "UTF-8");
            assertEquals("prep01=v01", line);
        } catch (IOException e) {
            fail(e.getMessage());
        }

        File[] expectedSci2Files = sci2Files.listFiles();
        assertEquals(1, expectedSci2Files.length);
        try {
            String line = IOUtils.readFirstLine(new FileInputStream(expectedSci2Files[0]), "UTF-8");
            assertEquals("prep02=v02", line);
        } catch (IOException e) {
            fail(e.getMessage());
        }

        File[] expectedSci3Files = sci3Files.listFiles();
        assertEquals(1, expectedSci3Files.length);
        try {
            String line = IOUtils.readFirstLine(new FileInputStream(expectedSci3Files[0]), "UTF-8");
            assertEquals("prep03=v03", line);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testCheckLabels() {
        SetupDeployer setupDeployer = new SetupDeployer();

        SetupConfigItem noLabelItem = new SetupConfigItem();

        //config items with no label should always be executed.
        for(int i = 0; i < 3; i++) {
            assertTrue(setupDeployer.checkLabels(this.slaves.get(i).getComputer(), noLabelItem));
        }

        SetupConfigItem oneLabelItem = new SetupConfigItem();
        oneLabelItem.setAssignedLabelString("foo");

        //config items with foo label should be executed on slave 0 and 1 but not on 2.
        assertTrue(setupDeployer.checkLabels(this.slaves.get(0).getComputer(), oneLabelItem));
        assertTrue(setupDeployer.checkLabels(this.slaves.get(1).getComputer(), oneLabelItem));
        assertFalse(setupDeployer.checkLabels(this.slaves.get(2).getComputer(), oneLabelItem));
    }


    private File prepSCIFile(String name) {
        Computer computer = Jenkins.MasterComputer.currentComputer();
        FilePath rootPath = computer.getNode().getRootPath();

        try {
            FilePath tempDir = rootPath.createTempDir(name, null);
            name = tempDir.getRemote();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        File sciFiles = new File(name);
        assertTrue(sciFiles.canRead() && sciFiles.isDirectory());
        for (File file : sciFiles.listFiles()) {
            file.delete();
        }
        assertEquals(0, sciFiles.listFiles().length);

        return sciFiles;
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
