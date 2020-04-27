/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2020
 */
package org.bitbucket.ucchy.undine;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import junit.framework.TestCase;

/**
 *
 * @author ucchy
 */
public class UUIDResolverTest extends TestCase {

    public void testGetUUID() {

        String myName = "ucchy";
        String myUuid = "9603ae84-5be8-40af-af14-a62ed0f14a29";


        long timeStart = System.currentTimeMillis();
        String uuid = UUIDResolver.getUUIDFromName(myName, new Date());
        System.out.println("ucchy uuid is " + uuid + ". Time : " + (System.currentTimeMillis() - timeStart));
        assertTrue(myUuid.equals(uuid));

        timeStart = System.currentTimeMillis();
        String uuid2 = UUIDResolver.getUUIDFromName(myName, new Date());
        System.out.println("ucchy uuid is " + uuid2 + ". Time : " + (System.currentTimeMillis() - timeStart));
        assertTrue(myUuid.equals(uuid2));

        //        Date date29DaysBefore = new Date(System.currentTimeMillis() - 1000L*24*3600* 29);
//        timeStart = System.currentTimeMillis();
//        String uuid6 = PCGFPluginLibBridge.getUUIDFromName(myName, true, true, date29DaysBefore);
//        System.out.println("ucchy uuid is " + uuid6 + ". Time : " + (System.currentTimeMillis() - timeStart));
//        assertTrue(myUuid.equals(uuid6));
//
//        Date date35DaysBefore = new Date(System.currentTimeMillis() - 1000L*24*3600* 35);
//        timeStart = System.currentTimeMillis();
//        String uuid3 = PCGFPluginLibBridge.getUUIDFromName(myName, true, true, date35DaysBefore);
//        System.out.println("ucchy uuid is " + uuid3 + ". Time : " + (System.currentTimeMillis() - timeStart));
//        assertTrue(myUuid.equals(uuid3));
//
//        Date date70DaysBefore = new Date(System.currentTimeMillis() - 1000L*24*3600* 70);
//        timeStart = System.currentTimeMillis();
//        String uuid4 = PCGFPluginLibBridge.getUUIDFromName(myName, true, true, date70DaysBefore);
//        System.out.println("ucchy uuid is " + uuid4 + ". Time : " + (System.currentTimeMillis() - timeStart));
//        assertTrue(myUuid.equals(uuid4));
//
//        timeStart = System.currentTimeMillis();
//        String uuid5 = PCGFPluginLibBridge.getUUIDFromName(myName, true, true, new Date());
//        System.out.println("ucchy uuid is " + uuid5 + ". Time : " + (System.currentTimeMillis() - timeStart));
//        assertTrue(myUuid.equals(uuid5));

        timeStart = System.currentTimeMillis();
        ArrayList<String> names = new ArrayList<>();
        for ( String n : new String[] {myName, "kotarobo", "RoboMWM", "foo", "bar", "uber", "aiueo",
                "test", "testttt", "testtttttttt", "thisIsInvalidID", "oooooooooo", "xxxxdddddxxxxxx"} ) {
            names.add(n);
        }
        Map<String, String> results = UUIDResolver.getUUIDsFromNames(names);
        System.out.println("getUUIDsFromNames. Time : " + (System.currentTimeMillis() - timeStart));
        for ( String key : results.keySet() ) {
            System.out.println(" - " + key + " --> " + results.get(key));
        }



        timeStart = System.currentTimeMillis();
        String name1 = UUIDResolver.getNameFromUUID(myUuid);
        System.out.println(myUuid + " is " + name1 + ". Time : " + (System.currentTimeMillis() - timeStart));
        assertTrue(myName.equals(name1));

        timeStart = System.currentTimeMillis();
        String name2 = UUIDResolver.getNameFromUUID("aiueo");
        System.out.println("aiueo is " + name2 + ". Time : " + (System.currentTimeMillis() - timeStart));
        assertTrue(name2 == null);
    }

}
