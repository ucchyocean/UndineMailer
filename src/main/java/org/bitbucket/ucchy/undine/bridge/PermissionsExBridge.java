package org.bitbucket.ucchy.undine.bridge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.bukkit.plugin.Plugin;

import ru.tehkode.permissions.PermissionGroup;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;

/**
 * PermissionsExへアクセスするためのブリッジクラス
 * @author ucchy
 */
public class PermissionsExBridge {

    /** コンストラクタは使用不可 */
    private PermissionsExBridge() {
    }

    /**
     * PermissionsExをロードする
     * @param plugin PermissionsExのプラグインインスタンス
     * @return ロードしたbridgeのインスタンス
     */
    public static PermissionsExBridge load(Plugin plugin) {
        if ( plugin == null ) return null;
        if ( !(plugin instanceof PermissionsEx) ) {
            return null;
        }
        return new PermissionsExBridge();
    }

    /**
     * 全てのグループ名を取得して返す
     * @return 全てのグループ名
     */
    public Collection<String> getGroupNames() {
        return PermissionsEx.getPermissionManager().getGroupNames();
    }

    /**
     * グループのオプション値を、Booleanとして取得する
     * @param group グループ名
     * @param option オプション名
     * @return オプションの値
     */
    public boolean getGroupOptionAsBoolean(String group, String option) {
        PermissionGroup g = PermissionsEx.getPermissionManager().getGroup(group);
        if ( g == null ) return false;
        String value = g.getOption(option);
        if ( value != null && value.equals("true") ) {
            return true;
        }
        return false;
    }

    /**
     * グループのオプション値を、Stringとして取得する
     * @param group グループ名
     * @param option オプション名
     * @return オプションの値
     */
    public String getGroupOptionAsString(String group, String option) {
        PermissionGroup g = PermissionsEx.getPermissionManager().getGroup(group);
        if ( g == null ) return null;
        return g.getOption(option);
    }

    /**
     * 指定のオプション値がtrueになっているグループのグループ名を取得する
     * @param option オプション名
     * @return 指定のオプションが設定されているグループのグループ名一覧
     */
    public ArrayList<String> getGroupNamesByBooleanOption(String option) {
        ArrayList<String> result = new ArrayList<String>();
        for ( PermissionGroup group : PermissionsEx.getPermissionManager().getGroupList() ) {
            String value = group.getOption(option);
            if ( value == null || !value.equals("true") ) {
                continue;
            }
            result.add(group.getName());
        }
        Collections.sort(result);
        return result;
    }

    /**
     * 指定したグループのメンバーのプレイヤー名一覧を取得する
     * @param group グループ名
     * @return メンバーのプレイヤー名一覧
     */
    public ArrayList<String> getGroupUsers(String group) {
        ArrayList<String> result = new ArrayList<String>();
        PermissionGroup g = PermissionsEx.getPermissionManager().getGroup(group);
        for ( PermissionUser user : g.getUsers() ) {
            result.add(user.getName());
        }
        return result;
    }
}
