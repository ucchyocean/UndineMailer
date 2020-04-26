/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2020
 */
package org.bitbucket.ucchy.undine.bridge;

import java.util.Date;

import junit.framework.TestCase;

public class PCGFPluginLibBridgeTest extends TestCase {

    public void testGetUUID() {

        long timeStart = System.currentTimeMillis();
        String uuid = PCGFPluginLibBridge.getUUIDFromName("ucchy", true, null);
        System.out.println("ucchy uuid is " + uuid + ". Time : " + (System.currentTimeMillis() - timeStart));
        assertTrue("9603ae845be840afaf14a62ed0f14a29".equals(uuid));

        timeStart = System.currentTimeMillis();
        String uuid2 = PCGFPluginLibBridge.getUUIDFromName("ucchy", true, new Date());
        System.out.println("ucchy uuid is " + uuid2 + ". Time : " + (System.currentTimeMillis() - timeStart));
        assertTrue("9603ae845be840afaf14a62ed0f14a29".equals(uuid2));

        Date date29DaysBefore = new Date(System.currentTimeMillis() - 1000L*24*3600* 29);
        timeStart = System.currentTimeMillis();
        String uuid6 = PCGFPluginLibBridge.getUUIDFromName("ucchy", true, date29DaysBefore);
        System.out.println("ucchy uuid is " + uuid6 + ". Time : " + (System.currentTimeMillis() - timeStart));
        assertTrue("9603ae845be840afaf14a62ed0f14a29".equals(uuid6));

        Date date35DaysBefore = new Date(System.currentTimeMillis() - 1000L*24*3600* 35);
        timeStart = System.currentTimeMillis();
        String uuid3 = PCGFPluginLibBridge.getUUIDFromName("ucchy", true, date35DaysBefore);
        System.out.println("ucchy uuid is " + uuid3 + ". Time : " + (System.currentTimeMillis() - timeStart));
        assertTrue("9603ae845be840afaf14a62ed0f14a29".equals(uuid3));

        Date date70DaysBefore = new Date(System.currentTimeMillis() - 1000L*24*3600* 70);
        timeStart = System.currentTimeMillis();
        String uuid4 = PCGFPluginLibBridge.getUUIDFromName("ucchy", true, date70DaysBefore);
        System.out.println("ucchy uuid is " + uuid4 + ". Time : " + (System.currentTimeMillis() - timeStart));
        assertTrue("9603ae845be840afaf14a62ed0f14a29".equals(uuid4));

        timeStart = System.currentTimeMillis();
        String uuid5 = PCGFPluginLibBridge.getUUIDFromName("ucchy", true, new Date());
        System.out.println("ucchy uuid is " + uuid5 + ". Time : " + (System.currentTimeMillis() - timeStart));
        assertTrue("9603ae845be840afaf14a62ed0f14a29".equals(uuid5));
    }
}
