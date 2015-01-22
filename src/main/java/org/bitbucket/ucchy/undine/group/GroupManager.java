/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine.group;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;

import org.bitbucket.ucchy.undine.UndineMailer;

/**
 * メールグループ管理クラス
 * @author ucchy
 */
public class GroupManager {

    private UndineMailer parent;
    private HashMap<String, GroupData> groups;

    /**
     * コンストラクタ
     * @param parent
     */
    public GroupManager(UndineMailer parent) {
        this.parent = parent;
        reload();
    }

    /**
     * 全データを再読み込みする
     */
    public void reload() {
        File folder = parent.getGroupFolder();
        File[] files = folder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".yml");
            }
        });

        groups = new HashMap<String, GroupData>();
        for ( File f : files ) {
            GroupData group = GroupData.loadFromFile(f);
            groups.put(group.getName().toLowerCase(), group);
        }
    }

    /**
     * グループを追加する。
     * 重複するグループ名が既に追加されている場合は、
     * 古いグループが上書きされてしまうことに注意する。
     * @param group グループ
     */
    public void addGroup(GroupData group) {
        String name = group.getName().toLowerCase();
        groups.put(name, group);
    }

    /**
     * 指定したグループ名のグループを取得する
     * @param name グループ名
     * @return グループ
     */
    public GroupData getGroup(String name) {
        name = name.toLowerCase();
        if ( groups.containsKey(name) ) {
            return groups.get(name);
        }
        return null;
    }

    /**
     * 指定したグループ名のグループを削除する
     * @param name グループ名
     */
    public void removeGroup(String name) {
        name = name.toLowerCase();
        if ( groups.containsKey(name) ) {
            groups.remove(name);
            File folder = parent.getGroupFolder();
            File file = new File(folder, name + ".yml");
            file.delete();
        }
    }

    /**
     * 指定したグループを実データファイルに保存する
     * @param group グループ
     */
    public void saveGroupData(GroupData group) {
        File folder = parent.getGroupFolder();
        File file = new File(folder, group.getName().toLowerCase() + ".yml");
        group.saveToFile(file);
    }

    /**
     * グループ名として使用できる名前かどうかを確認する
     * @param name グループ名
     * @return 使用可能かどうか
     */
    public static boolean canUseNameFromGroup(String name) {
        return name.matches("[0-9a-zA-Z\\-_]{1,20}");
    }
}
