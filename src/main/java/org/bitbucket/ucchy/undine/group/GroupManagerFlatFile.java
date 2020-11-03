/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine.group;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bitbucket.ucchy.undine.UndineMailer;
import org.bitbucket.ucchy.undine.sender.MailSender;

/**
 * メールグループをファイルで管理するクラス
 * @author ucchy
 */
public class GroupManagerFlatFile extends GroupManager {

    private HashMap<String, GroupData> groups;

    /**
     * コンストラクタ
     * 
     * @param parent
     */
    public GroupManagerFlatFile(UndineMailer parent) {
        super(parent);
    }

    /**
     * 全データを再読み込みする
     */
    public void reload() {

        long start = System.currentTimeMillis();

        File folder = parent.getGroupFolder();
        File[] files = folder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".yml");
            }
        });

        groups = new HashMap<String, GroupData>();

        if ( files != null ) {
            for ( File f : files ) {
                GroupData group = GroupData.loadFromFile(f);
                groups.put(group.getName().toLowerCase(), group);
            }
        }

        // 特殊グループを追加する
        GroupData all = new SpecialGroupAll();
        groups.put(all.getName().toLowerCase(), all);
        GroupData allConnected = new SpecialGroupAllConnected();
        groups.put(allConnected.getName().toLowerCase(), allConnected);
        GroupData allLogin = new SpecialGroupAllLogin();
        groups.put(allLogin.getName().toLowerCase(), allLogin);

        // アップグレード処理
        start = System.currentTimeMillis();

        int total = 0;
        for ( GroupData group : groups.values() ) {
            if ( group.upgrade() ) {
                saveGroupData(group);
                total++;
            }
        }

        if ( total > 0 ) {
            UndineMailer.getInstance().getLogger().info("Upgrade group data... Done.  Time: "
                    + (System.currentTimeMillis() - start) + "ms, Data: " + total + ".");
        }
    }

    @Override
    public void addGroup(GroupData group) {
        String name = group.getName().toLowerCase();
        groups.put(name, group);
        saveGroupData(group);
    }

    @Override
    public GroupData getGroup(String name) {
        name = name.toLowerCase();

        // PEXから取得する
        if ( name.startsWith(SpecialGroupPex.NAME_PREFIX) && pexGroupsCache != null ) {
            if ( pexGroupsCache.containsKey(name) ) {
                return pexGroupsCache.get(name);
            }
        }

        return groups.get(name);
    }

    @Override
    public ArrayList<GroupData> getGroups(List<String> names) {
        ArrayList<GroupData> groups = new ArrayList<>();
        for (String name : names) {
            groups.add(getGroup(name));
        }
        return groups;
    }

    @Override
    public void removeGroup(String name) {
        name = name.toLowerCase();
        if ( groups.containsKey(name) ) {
            groups.remove(name);
            File folder = parent.getGroupFolder();
            File file = new File(folder, name + ".yml");
            file.delete();
        }
    }

    @Override
    public ArrayList<String> getAllGroupNames() {
        ArrayList<String> names = new ArrayList<String>();
        for ( GroupData group : groups.values() ) {
            names.add(group.getName());
        }
        return names;
    }

    @Override
    public ArrayList<GroupData> getAllGroups() {
        return new ArrayList<GroupData>(groups.values());
    }

    @Override
    public boolean existGroupName(String name) {
        return groups.keySet().contains(name.toLowerCase());
    }

    @Override
    public void saveGroupData(GroupData group) {
        File folder = parent.getGroupFolder();
        File file = new File(folder, group.getName().toLowerCase() + ".yml");
        group.saveToFile(file);
    }

    @Override
    public int getOwnerGroupCount(MailSender sender) {
        int total = 0;
        for ( GroupData group : groups.values() ) {
            if ( group.getOwner().equals(sender) ) total++;
        }
        return total;
    }
}
