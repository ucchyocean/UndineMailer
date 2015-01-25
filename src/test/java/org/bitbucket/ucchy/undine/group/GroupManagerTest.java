package org.bitbucket.ucchy.undine.group;

import junit.framework.TestCase;

public class GroupManagerTest extends TestCase {

    public void testCanUseNameFromGroup() {

        assertTrue(GroupManager.canUseNameFromGroup("aiueoAIUEO12345"));
        assertFalse(GroupManager.canUseNameFromGroup("aiueoAIUEO123456"));
        assertTrue(GroupManager.canUseNameFromGroup("日本語だよ！"));
        assertTrue(GroupManager.canUseNameFromGroup("ファイル名に使える+"));
        assertFalse(GroupManager.canUseNameFromGroup("ファイル名に使えない<"));
        assertFalse(GroupManager.canUseNameFromGroup("スラッシュ/はダメ"));
        assertFalse(GroupManager.canUseNameFromGroup("えんマーク\\はダメ"));
        assertFalse(GroupManager.canUseNameFromGroup("はてな?だめです。"));
        assertFalse(GroupManager.canUseNameFromGroup("アスタ*ダメよ。"));
        assertFalse(GroupManager.canUseNameFromGroup("コロン:もダメ。"));
        assertFalse(GroupManager.canUseNameFromGroup("バー|はダメ。"));
        assertFalse(GroupManager.canUseNameFromGroup("引用符\"もダメ。"));
        assertFalse(GroupManager.canUseNameFromGroup("大なり>はダメ。"));
        assertFalse(GroupManager.canUseNameFromGroup(".はダメよ～ダメダメ"));
    }
}
