/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine.group;

/**
 * グループの権限モード
 * @author ucchy
 */
public enum GroupPermissionMode {

    /** OPのみ変更可能 */
    OP,

    /** オーナーとOPが可能 */
    OWNER,

    /** メンバーなら誰でも可能 */
    MEMBER,

    /** 誰でも可能 */
    EVERYONE,
    ;

    /**
     * 文字列からGroupPermissionModeを作成して返す
     * @param str 文字列
     * @return GroupPermissionMode
     */
    public static GroupPermissionMode getFromString(String str) {
        return getFromString(str, null);
    }

    /**
     * 文字列からGroupPermissionModeを作成して返す
     * @param str 文字列
     * @param def デフォルト
     * @return GroupPermissionMode
     */
    public static GroupPermissionMode getFromString(String str, GroupPermissionMode def) {
        if ( str == null ) return def;
        for ( GroupPermissionMode mode : values() ) {
            if ( mode.toString().equals(str.toUpperCase()) ) return mode;
        }
        return def;
    }
}
