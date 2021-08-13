package org.bitbucket.ucchy.undine.bridge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.node.matcher.NodeMatcher;
import net.luckperms.api.node.types.InheritanceNode;

import org.bukkit.plugin.Plugin;

/**
 * LuckPermsへアクセスするためのブリッジクラス
 *
 * @author ucchy
 */
public class LuckPermsBridge {

    /**
     * コンストラクタは使用不可
     */
    private LuckPermsBridge() {
    }

    public static LuckPerms luckperms = null;

    /**
     * LuckPermsをロードする
     *
     * @param plugin LuckPermsのプラグインインスタンス
     * @return ロードしたbridgeのインスタンス
     */
    public static LuckPermsBridge load(Plugin plugin) {
        if (plugin == null) return null;
        if (!(plugin instanceof LuckPerms)) {
            return null;
        }
        return new LuckPermsBridge();
    }

    /**
     * グループのオプション値を、Stringとして取得する
     *
     * @param groupName グループ名
     * @param option    オプション名
     * @return オプションの値
     */
    public String getGroupOptionAsString(String groupName, String option) {
        Group group = luckperms.getGroupManager().getGroup(groupName);
        if (group == null) return null;
        return group.getCachedData().getMetaData().getMetaValue(option);
    }

    /**
     * 指定のオプション値がtrueになっているグループのグループ名を取得する
     *
     * @param options オプション名
     * @return 指定のオプションが設定されているグループのグループ名一覧
     */
    public ArrayList<String> getGroupNamesByBooleanOption(ArrayList<String> options) {
        ArrayList<String> result = new ArrayList<String>();
        for (Group group : luckperms.getGroupManager().getLoadedGroups()) {
            for (String option : options) {
                String value = group.getCachedData().getMetaData().getMetaValue(option);
                if (value != null && value.equals("true")) {
                    result.add(group.getName());
                    break;
                }
            }
        }
        Collections.sort(result);
        return result;
    }

    /**
     * 指定したグループのメンバーのプレイヤー名一覧を取得する
     *
     * @param groupName グループ名
     * @return メンバーのプレイヤー名一覧
     */
    public ArrayList<String> getGroupUsers(String groupName) {
        ArrayList<String> result = new ArrayList<String>();

        UserManager userManager = luckperms.getUserManager();
        Group group = luckperms.getGroupManager().getGroup(groupName);
        for (UUID uuid : userManager.searchAll(NodeMatcher.key(InheritanceNode.builder(group).build())).join().keySet()) {
            User user = userManager.isLoaded(uuid) ? userManager.getUser(uuid) : userManager.loadUser(uuid).join();
            if (user == null) throw new IllegalStateException("Could not load data of " + uuid);
            result.add(user.getUsername());
        }
        return result;
    }
}
