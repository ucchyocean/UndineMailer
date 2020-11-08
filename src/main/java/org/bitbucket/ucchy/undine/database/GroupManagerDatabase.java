package org.bitbucket.ucchy.undine.database;

import java.util.ArrayList;
import java.util.List;

import org.bitbucket.ucchy.undine.UndineMailer;
import org.bitbucket.ucchy.undine.group.GroupData;
import org.bitbucket.ucchy.undine.group.GroupManager;
import org.bitbucket.ucchy.undine.group.SpecialGroupAll;
import org.bitbucket.ucchy.undine.group.SpecialGroupAllConnected;
import org.bitbucket.ucchy.undine.group.SpecialGroupAllLogin;
import org.bitbucket.ucchy.undine.group.SpecialGroupPex;
import org.bitbucket.ucchy.undine.sender.MailSender;

/**
 * メールグループをデータベースで管理するクラス。
 * @author LazyGon
 */
public class GroupManagerDatabase extends GroupManager {

    private GroupDataTable groupDataTable;

    /**
     * コンストラクタ
     * 
     * @param parent
     */
    public GroupManagerDatabase(UndineMailer parent) {
        super(parent);

        this.groupDataTable = parent.getDatabase().groupDataTable;

        // 特殊グループの登録
        addGroup(groupAll);
        addGroup(groupAllConnected);
        addGroup(groupAllLogin);
    }

    /**
     * 全データを再読み込みする
     */
    @Override
    public void reload() {
        // Do nothing. 常にデータベースと同期されているため、再読込する必要はない。
        return;
    }

    /**
     * グループを追加する。
     * 重複するグループ名が既に追加されている場合は、
     * 古いグループが上書きされてしまうことに注意する。
     * @param group グループ
     */
    @Override
    public void addGroup(GroupData group) {
        saveGroupData(group);
    }

    /**
     * 指定したグループ名のグループを取得する
     * @param name グループ名
     * @return グループ
     */
    @Override
    public GroupData getGroup(String name) {
        GroupData pexGroup = getPexGroup(name);
        if (pexGroup != null) {
            return pexGroup;
        }

        return groupDataTable.exists(name) 
            ? new GroupDataDatabase(parent, name)
            : null;
    }

    /**
     * 指定したグループ名のグループをすべて取得する。
     * @param names グループ名のリスト
     * @return グループのリスト
     */
    public ArrayList<GroupData> getGroups(List<String> names) {
        return convert(names, true);
    }

    /**
     * 指定したグループ名のグループを削除する
     * @param name グループ名
     */
    @Override
    public void removeGroup(String name) {
        groupDataTable.deleteByName(name);
    }

    /**
     * 全てのグループ名を取得する
     * @return 全てのグループ名
     */
    @Override
    public ArrayList<String> getAllGroupNames() {
        return groupDataTable.getNames();
    }

    /**
     * 全てのグループを取得する
     * @return 全てのグループ
     */
    @Override
    public ArrayList<GroupData> getAllGroups() {
        return convert(groupDataTable.getNames(), false);
    }

    /**
     * 指定されたグループ名は既に存在するかどうかを確認する
     * @return 存在するかどうか
     */
    @Override
    public boolean existGroupName(String name) {
        return groupDataTable.exists(name);
    }

    /**
     * 指定したグループを保存する
     * @param group グループ
     */
    @Override
    public void saveGroupData(GroupData group) {
        groupDataTable.add(group.getName(), group.getOwner(), group.getSendMode(), group.getModifyMode(), group.getDissolutionMode());
    }

    /**
     * @return サーバーの起動している間に接続したすべてのプレイヤーを示すグループ
     */
    @Override
    public int getOwnerGroupCount(MailSender sender) {
        return groupDataTable.getNamesByOwner(parent.getDatabase().mailSenderTable.getId(sender)).size();
    }

    /**
     * 指定されたグループ名からグループを作成して返す。
     * @param groupNames グループ名のリスト
     * @param filter データベースにグループが有るかチェックするかどうか
     * @return グループのリスト
     */
    private ArrayList<GroupData> convert(List<String> groupNames, boolean filter) {
        if (filter) {
            groupDataTable.retainExistsByName(groupNames);
        }
        ArrayList<GroupData> groups = new ArrayList<>();
        for (String name : groupNames) {
            if (name.equals(SpecialGroupAll.NAME)) {
                groups.add(parent.getGroupManager().getGroupAll());
            } else if (name.equals(SpecialGroupAllConnected.NAME)) {
                groups.add(parent.getGroupManager().getGroupAllConnected());
            } else if (name.equals(SpecialGroupAllLogin.NAME)) {
                groups.add(parent.getGroupManager().getGroupAllLogin());
            } else if (name.toLowerCase().startsWith(SpecialGroupPex.NAME_PREFIX)) {
                groups.add(parent.getGroupManager().getPexGroup(name));
            } else {
                groups.add(new GroupDataDatabase(parent, name));
            }
        }
        return groups;
    }
}
