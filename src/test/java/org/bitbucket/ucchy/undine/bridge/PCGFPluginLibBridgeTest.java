/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2020
 */
package org.bitbucket.ucchy.undine.bridge;

import junit.framework.TestCase;

public class PCGFPluginLibBridgeTest extends TestCase {

    public void testGetUUID() {

        String uuid = PCGFPluginLibBridge.getUUIDFromName("ucchy", true, null);
        System.out.println("ucchy uuid is " + uuid + ".");
        assertTrue("9603ae845be840afaf14a62ed0f14a29".equals(uuid));
    }
}
