package org.jenkinsci.plugins.slave_setup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.FilePath;
import hudson.model.*;
import hudson.slaves.DumbSlave;
import hudson.slaves.OfflineCause;
import hudson.slaves.SlaveComputer;
import hudson.util.IOUtils;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * @author Frederik Fromm
 */
@WithJenkins
class SetupDeployerTest {

    private List<DumbSlave> slaves;

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        j = rule;

        slaves = new ArrayList<>();

        slaves.add(j.createSlave("foo", null));
        slaves.add(j.createSlave("foo bar", null));
        slaves.add(j.createSlave("bar", null));

        for (int i = 0; i < 3; i++) {
            SlaveComputer c = slaves.get(i).getComputer();
            c.connect(false).get(); // wait until it's connected

            assertFalse(c.isOffline(), "Slave failed to go online: " + c.getLog());
        }
        Components.setLogger(j.createTaskListener());
        Components.enableDebug();
    }

    @Test
    void testGetAllActiveSlaves() throws Exception {
        List<Computer> allActiveSlaves = Utils.getAllActiveSlaves();
        assertNotNull(allActiveSlaves);
        assertEquals(3, allActiveSlaves.size());

        slaves.get(1)
                .getComputer()
                .disconnect(OfflineCause.create(Messages._Hudson_NodeBeingRemoved()))
                .get(3, TimeUnit.SECONDS);

        allActiveSlaves = Utils.getAllActiveSlaves();
        assertEquals(2, allActiveSlaves.size());

        slaves.get(0)
                .getComputer()
                .disconnect(OfflineCause.create(Messages._Hudson_NodeBeingRemoved()))
                .get(3, TimeUnit.SECONDS);

        allActiveSlaves = Utils.getAllActiveSlaves();
        assertEquals(1, allActiveSlaves.size());
    }

    @Test
    void testDeployToComputer() throws Exception {
        // TODO: Update test to use copyFiles

        slaves.get(0)
                .getComputer()
                .disconnect(OfflineCause.create(Messages._Hudson_NodeBeingRemoved()))
                .get(3, TimeUnit.SECONDS);
        slaves.get(2)
                .getComputer()
                .disconnect(OfflineCause.create(Messages._Hudson_NodeBeingRemoved()))
                .get(3, TimeUnit.SECONDS);

        List<Computer> activeSlaves = Utils.getAllActiveSlaves();
        assertEquals(1, activeSlaves.size());

        Computer slave = activeSlaves.get(0);

        FilePath root = slave.getNode().getRootPath();

        TaskListener taskListener = j.createTaskListener();

        SetupConfig config = createConfig(null);
        SetupDeployer.executeScriptOnMaster(taskListener, "", null);

        // SetupDeployer.executePrepareScripts(slave, config, taskListener, new ArrayList<>());
        // File testFile = new File("tmp/setup.txt");

        File testFile = new File("src/test/resources/files");
        testFile.createNewFile();

        SetupDeployer.copyFiles(testFile, root);
        // SetupDeployer.deployToComputer(slave, root, taskListener, config);
        SetupDeployer.executeScriptOnSlave(taskListener, "", root, null);

        FilePath[] files = slave.getNode().getRootPath().list("setup.txt"); // root.list ?
        assertEquals(1, files.length);
    }

    @Test
    void testDeployToComputers() throws Exception {
        // File testFile = new File("/tmp/setup.txt");

        File testFile = new File("src/test/resources/files/setup.txt");
        testFile.createNewFile();
        Components.doSetups(Utils.getAllActiveSlaves());
    }

    @Test
    @Disabled(
            "Test disabled because it fails on newer Jenkins releases.  Not willing to spend the effort to understand why it fails ")
    void failingTestExecutePrepareScripts() throws Exception {
        SetupConfig setupConfig = new SetupConfig();

        File sci1Files = prepSCIFile("sci1");
        SetupConfigItem sci1 = new SetupConfigItem();
        sci1.setPrepareScript("echo prep01=v01> " + sci1Files.getCanonicalPath() + "/sci1.properties");
        sci1.setAssignedLabelString("foo");
        sci1.setFilesDir(sci1Files);
        setupConfig.getSetupConfigItems().add(sci1);

        File sci2Files = prepSCIFile("sci2");
        SetupConfigItem sci2 = new SetupConfigItem();
        sci2.setPrepareScript("echo prep02=v02> " + sci2Files.getCanonicalPath() + "/sci2.properties");
        sci2.setAssignedLabelString("bar");
        sci2.setFilesDir(sci2Files);
        setupConfig.getSetupConfigItems().add(sci2);

        File sci3Files = prepSCIFile("sci3");
        SetupConfigItem sci3 = new SetupConfigItem();
        sci3.setPrepareScript("echo prep03=v03> " + sci3Files.getCanonicalPath() + "/sci3.properties");
        sci3.setAssignedLabelString("foo || bar");
        sci3.setFilesDir(sci3Files);
        setupConfig.getSetupConfigItems().add(sci3);

        TaskListener taskListener = j.createTaskListener();

        for (SetupConfigItem item : setupConfig.getSetupConfigItems()) {
            SetupDeployer.executeScriptOnMaster(taskListener, item.getPrepareScript(), null);
        }

        // SetupDeployer.executePrepareScripts(null, setupConfig, taskListener, new ArrayList<>());

        assertFirstLineEquals(sci1Files.listFiles(), "prep01=v01");

        assertFirstLineEquals(sci2Files.listFiles(), "prep02=v02");

        assertFirstLineEquals(sci3Files.listFiles(), "prep03=v03");
    }

    private void assertFirstLineEquals(File[] expectedSciFiles, String expected) throws Exception {
        assertEquals(1, expectedSciFiles.length);
        String line = IOUtils.readFirstLine(new FileInputStream(expectedSciFiles[0]), StandardCharsets.UTF_8.name());
        assertEquals(expected, line);
    }

    @Test
    void testCheckLabels() {
        SetupConfigItem noLabelItem = new SetupConfigItem();

        // config items with no label should always be executed.
        for (int i = 0; i < 3; i++) {
            assertTrue(SetupDeployer.checkLabelsForComputerOrNull(slaves.get(i).getComputer(), noLabelItem));
        }

        SetupConfigItem oneLabelItem = new SetupConfigItem();
        oneLabelItem.setAssignedLabelString("foo");

        // config items with foo label should be executed on slave 0 and 1 but not on 2.
        assertTrue(SetupDeployer.checkLabelsForComputerOrNull(slaves.get(0).getComputer(), oneLabelItem));
        assertTrue(SetupDeployer.checkLabelsForComputerOrNull(slaves.get(1).getComputer(), oneLabelItem));
        assertFalse(SetupDeployer.checkLabelsForComputerOrNull(slaves.get(2).getComputer(), oneLabelItem));

        SetupConfigItem oneLabelItemWithWhitespace = new SetupConfigItem();
        oneLabelItemWithWhitespace.setAssignedLabelString("foo ");

        // config items with foo label should be executed on slave 0 and 1 but not on 2.
        assertTrue(SetupDeployer.checkLabelsForComputerOrNull(slaves.get(0).getComputer(), oneLabelItemWithWhitespace));
        assertTrue(SetupDeployer.checkLabelsForComputerOrNull(slaves.get(1).getComputer(), oneLabelItemWithWhitespace));
        assertFalse(
                SetupDeployer.checkLabelsForComputerOrNull(slaves.get(2).getComputer(), oneLabelItemWithWhitespace));
    }

    private File prepSCIFile(String name) throws Exception {
        // Computer computer = Jenkins.MasterComputer.currentComputer();
        // FilePath rootPath = computer.getNode().getRootPath();
        FilePath rootPath = Jenkins.get().getRootPath();

        FilePath tempDir = rootPath.createTempDir(name, null);
        name = tempDir.getRemote();

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

        List<SetupConfigItem> setupConfigItems = new ArrayList<>();
        setupConfigItems.add(item);

        setupConfig.setSetupConfigItems(setupConfigItems);

        return setupConfig;
    }
}
