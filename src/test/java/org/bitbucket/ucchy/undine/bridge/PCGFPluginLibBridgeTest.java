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

        String myName = "ucchy";
        String myUuid = "9603ae84-5be8-40af-af14-a62ed0f14a29";

        long timeStart = System.currentTimeMillis();
        String uuid = PCGFPluginLibBridge.getUUIDFromName(myName, true, true, null);
        System.out.println("ucchy uuid is " + uuid + ". Time : " + (System.currentTimeMillis() - timeStart));
        assertTrue(myUuid.equals(uuid));

        timeStart = System.currentTimeMillis();
        String uuid2 = PCGFPluginLibBridge.getUUIDFromName(myName, true, true, new Date());
        System.out.println("ucchy uuid is " + uuid2 + ". Time : " + (System.currentTimeMillis() - timeStart));
        assertTrue(myUuid.equals(uuid2));

        Date date29DaysBefore = new Date(System.currentTimeMillis() - 1000L*24*3600* 29);
        timeStart = System.currentTimeMillis();
        String uuid6 = PCGFPluginLibBridge.getUUIDFromName(myName, true, true, date29DaysBefore);
        System.out.println("ucchy uuid is " + uuid6 + ". Time : " + (System.currentTimeMillis() - timeStart));
        assertTrue(myUuid.equals(uuid6));

        Date date35DaysBefore = new Date(System.currentTimeMillis() - 1000L*24*3600* 35);
        timeStart = System.currentTimeMillis();
        String uuid3 = PCGFPluginLibBridge.getUUIDFromName(myName, true, true, date35DaysBefore);
        System.out.println("ucchy uuid is " + uuid3 + ". Time : " + (System.currentTimeMillis() - timeStart));
        assertTrue(myUuid.equals(uuid3));

        Date date70DaysBefore = new Date(System.currentTimeMillis() - 1000L*24*3600* 70);
        timeStart = System.currentTimeMillis();
        String uuid4 = PCGFPluginLibBridge.getUUIDFromName(myName, true, true, date70DaysBefore);
        System.out.println("ucchy uuid is " + uuid4 + ". Time : " + (System.currentTimeMillis() - timeStart));
        assertTrue(myUuid.equals(uuid4));

        timeStart = System.currentTimeMillis();
        String uuid5 = PCGFPluginLibBridge.getUUIDFromName(myName, true, true, new Date());
        System.out.println("ucchy uuid is " + uuid5 + ". Time : " + (System.currentTimeMillis() - timeStart));
        assertTrue(myUuid.equals(uuid5));


        timeStart = System.currentTimeMillis();
        String name1 = PCGFPluginLibBridge.getNameFromUUID(myUuid);
        System.out.println(myUuid + " is " + name1 + ". Time : " + (System.currentTimeMillis() - timeStart));
        assertTrue(myName.equals(name1));

        /* MavenビルドでExceptionが記録されて邪魔なので、コメントアウト
        timeStart = System.currentTimeMillis();
        String name2 = PCGFPluginLibBridge.getNameFromUUID("aiueo");
        System.out.println("aiueo is " + name2 + ". Time : " + (System.currentTimeMillis() - timeStart));
        assertTrue("<unknown>".equals(name2));
        */
    }
}
