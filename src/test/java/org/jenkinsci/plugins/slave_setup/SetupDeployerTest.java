package org.jenkinsci.plugins.slave_setup;

import hudson.FilePath;
import hudson.model.*;
import hudson.slaves.DumbSlave;
import hudson.slaves.OfflineCause;
import hudson.slaves.SlaveComputer;
import hudson.util.IOUtils;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

        for (int i = 0; i < 3; i++) {
            SlaveComputer c = this.slaves.get(i).getComputer();
            c.connect(false).get(); // wait until it's connected
            if (c.isOffline()) {
                fail("Slave failed to go online: " + c.getLog());
            }
        }
        Components.setLogger(this.createTaskListener());
        Components.enableDebug();
    }

    @Test
    public void testGetAllActiveSlaves() throws Exception {
        SetupDeployer setupDeployer = new SetupDeployer();

        List<Computer> allActiveSlaves = Utils.getAllActiveSlaves();
        assertNotNull(allActiveSlaves);
        assertEquals(3, allActiveSlaves.size());

        this.slaves.get(1).getComputer().disconnect(OfflineCause.create(Messages._Hudson_NodeBeingRemoved()));
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        allActiveSlaves = Utils.getAllActiveSlaves();
        assertEquals(2, allActiveSlaves.size());

        this.slaves.get(0).getComputer().disconnect(OfflineCause.create(Messages._Hudson_NodeBeingRemoved()));
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        allActiveSlaves = Utils.getAllActiveSlaves();
        assertEquals(1, allActiveSlaves.size());
    }

    @Test
    public void testDeployToComputer() {
        // TODO: Update test to use copyFiles

        this.slaves.get(0).getComputer().disconnect(OfflineCause.create(Messages._Hudson_NodeBeingRemoved()));
        this.slaves.get(2).getComputer().disconnect(OfflineCause.create(Messages._Hudson_NodeBeingRemoved()));

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        List<Computer> activeSlaves = Utils.getAllActiveSlaves();
        assertEquals(1, activeSlaves.size());

        Computer slave = activeSlaves.get(0);

        FilePath root = slave.getNode().getRootPath();

        TaskListener taskListener = this.createTaskListener();

        try {
            SetupConfig config = createConfig(null);
            SetupDeployer.executeScriptOnMaster(taskListener, "", null);
            // setupDeployer.executePrepareScripts(slave, config, taskListener, new
            // ArrayList<String>());
            // File testFile = new File("tmp/setup.txt");

            File testFile = new File("src/test/resources/files");
            testFile.createNewFile();

            SetupDeployer.copyFiles(testFile, root);
            // SetupDeployer.deployToComputer(slave, root, taskListener, config);
            SetupDeployer.executeScriptOnSlave(taskListener, "", root, null);

            FilePath[] files = slave.getNode().getRootPath().list("setup.txt"); // root.list ?
            assertEquals(1, files.length);

        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * @throws IOException
     * 
     */
    @Test
    public void testDeployToComputers() throws IOException {
        // File testFile = new File("/tmp/setup.txt");
        
        File testFile = new File("src/test/resources/files/setup.txt");
        testFile.createNewFile();
        Components.doSetups(Utils.getAllActiveSlaves());
    }

    @Test
    public void testExecutePrepareScripts() throws Exception {
        SetupConfig setupConfig = new SetupConfig();

        File sci1Files = prepSCIFile("sci1");
        SetupConfigItem sci1 = new SetupConfigItem();
        sci1.setPrepareScript("echo prep01=v01 > " + sci1Files.getCanonicalPath() + "/sci1.properties");
        sci1.setAssignedLabelString("foo");
        sci1.setFilesDir(sci1Files);
        setupConfig.getSetupConfigItems().add(sci1);

        File sci2Files = prepSCIFile("sci2");
        SetupConfigItem sci2 = new SetupConfigItem();
        sci2.setPrepareScript("echo prep02=v02 > " + sci2Files.getCanonicalPath() + "/sci2.properties");
        sci2.setAssignedLabelString("bar");
        sci2.setFilesDir(sci2Files);
        setupConfig.getSetupConfigItems().add(sci2);

        File sci3Files = prepSCIFile("sci3");
        SetupConfigItem sci3 = new SetupConfigItem();
        sci3.setPrepareScript("echo prep03=v03 > " + sci3Files.getCanonicalPath() + "/sci3.properties");
        sci3.setAssignedLabelString("foo || bar");
        sci3.setFilesDir(sci3Files);
        setupConfig.getSetupConfigItems().add(sci3);

        TaskListener taskListener = this.createTaskListener();

        for (SetupConfigItem item : setupConfig.getSetupConfigItems()) {
            SetupDeployer.executeScriptOnMaster(taskListener, item.getPrepareScript(), null);
        }

        // SetupDeployer setupDeployer = new SetupDeployer();
        // setupDeployer.executePrepareScripts(null, setupConfig, taskListener, new
        // ArrayList<String>());

        assertFirstLineEquals(sci1Files.listFiles(), "prep01=v01");

        assertFirstLineEquals(sci2Files.listFiles(), "prep02=v02");

        assertFirstLineEquals(sci3Files.listFiles(), "prep03=v03");
    }


    private void assertFirstLineEquals(File[] expectedSciFiles, String expected) {
        assertEquals(1, expectedSciFiles.length);
        try {
            String line = IOUtils.readFirstLine(new FileInputStream(expectedSciFiles[0]), "UTF-8");
            assertEquals(expected, line);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testCheckLabels() {
        SetupDeployer setupDeployer = new SetupDeployer();

        SetupConfigItem noLabelItem = new SetupConfigItem();

        // config items with no label should always be executed.
        for (int i = 0; i < 3; i++) {
            assertTrue(SetupDeployer.checkLabelsForComputerOrNull(this.slaves.get(i).getComputer(), noLabelItem));
        }

        SetupConfigItem oneLabelItem = new SetupConfigItem();
        oneLabelItem.setAssignedLabelString("foo");

        // config items with foo label should be executed on slave 0 and 1 but not on 2.
        assertTrue(SetupDeployer.checkLabelsForComputerOrNull(this.slaves.get(0).getComputer(), oneLabelItem));
        assertTrue(SetupDeployer.checkLabelsForComputerOrNull(this.slaves.get(1).getComputer(), oneLabelItem));
        assertFalse(SetupDeployer.checkLabelsForComputerOrNull(this.slaves.get(2).getComputer(), oneLabelItem));

        SetupConfigItem oneLabelItemWithWhitespace = new SetupConfigItem();
        oneLabelItemWithWhitespace.setAssignedLabelString("foo ");

        // config items with foo label should be executed on slave 0 and 1 but not on 2.
        assertTrue(SetupDeployer.checkLabelsForComputerOrNull(this.slaves.get(0).getComputer(),
                oneLabelItemWithWhitespace));
        assertTrue(SetupDeployer.checkLabelsForComputerOrNull(this.slaves.get(1).getComputer(),
                oneLabelItemWithWhitespace));
        assertFalse(SetupDeployer.checkLabelsForComputerOrNull(this.slaves.get(2).getComputer(),
                oneLabelItemWithWhitespace));
    }

    private File prepSCIFile(String name) {
        // Computer computer = Jenkins.MasterComputer.currentComputer();
        // FilePath rootPath = computer.getNode().getRootPath();
        FilePath rootPath = Jenkins.getInstance().getRootPath();

        try {
            FilePath tempDir = rootPath.createTempDir(name, null);
            name = tempDir.getRemote();
        } catch (IOException e) {
            e.printStackTrace(); // To change body of catch statement use File | Settings | File Templates.
        } catch (InterruptedException e) {
            e.printStackTrace(); // To change body of catch statement use File | Settings | File Templates.
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
        if (StringUtils.isNotBlank(label)) {
            item.setAssignedLabelString(label);
        }

        List<SetupConfigItem> setupConfigItems = new ArrayList<SetupConfigItem>();
        setupConfigItems.add(item);

        setupConfig.setSetupConfigItems(setupConfigItems);

        return setupConfig;
    }
}
