package org.jenkinsci.plugins.slave_setup;

import hudson.model.Node;
import hudson.slaves.DumbSlave;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;

import java.util.List;

import static org.junit.Assert.assertNotNull;

/**
 * Created by IntelliJ IDEA.
 * User: c031
 * Date: 08.02.12
 * Time: 11:28
 * To change this template use File | Settings | File Templates.
 */
public class SetupDeployerTest extends HudsonTestCase {

    private DumbSlave slave01;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.slave01 = this.createOnlineSlave();

    }

    @Test
    public void testGetAllActiveSlaves() throws Exception {
        SetupDeployer setupDeployer = new SetupDeployer();

        final List<Node> allActiveSlaves = setupDeployer.getAllActiveSlaves();
        assertNotNull(allActiveSlaves);
        assertEquals(1, allActiveSlaves.size());
    }
}
