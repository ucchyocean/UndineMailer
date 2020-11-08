package org.bitbucket.ucchy.undine.database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bitbucket.ucchy.undine.UndineConfig;
import org.bitbucket.ucchy.undine.UndineMailer;
import org.bitbucket.ucchy.undine.database.Database.DatabaseType;
import org.bitbucket.ucchy.undine.group.GroupDataFlatFile;
import org.bitbucket.ucchy.undine.group.GroupPermissionMode;
import org.bitbucket.ucchy.undine.sender.MailSender;

/**
 * グループデータを保持するメインテーブルにアクセスするクラス。
 * @author LazyGon
 */
public class GroupDataTable {

    public static final String NAME = "undine_groupdata";

    private final Database database;
    private final MailSenderTable mailSenderTable;

    GroupDataTable(Database database, MailSenderTable mailSenderTable) {
        this.database = database;
        this.mailSenderTable = mailSenderTable;
        createTable();
    }

    void createTable() {
        UndineConfig config = database.parent.getUndineConfig();
        database.execute(
            "CREATE TABLE IF NOT EXISTS " + NAME + " (" +
                "name VARCHAR(64) PRIMARY KEY" + (database.getDatabaseType() == DatabaseType.SQLITE ? " COLLATE NOCASE" : "") + ", " +
                "owner INTEGER NOT NULL, " +
                "sendMode TINYINT NOT NULL DEFAULT " + config.getSendModeDefault().ordinal() + ", " +
                "modifyMode TINYINT NOT NULL DEFAULT " + config.getModifyModeDefault().ordinal() + ", " +
                "dissolutionMode TINYINT NOT NULL DEFAULT " + config.getDissolutionModeDefault().ordinal() + ", " +
                "FOREIGN KEY (owner) REFERENCES " + MailSenderTable.NAME + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
            ")"
        );
    }

    /**
     * 
     * @param name
     * @param owner
     * @return
     */
    public boolean add(String name, MailSender owner) {
        UndineConfig config = database.parent.getUndineConfig();
        return add(name, owner, config.getSendModeDefault(), config.getModifyModeDefault(), config.getDissolutionModeDefault());
    }

    public boolean add(String name, MailSender owner, GroupPermissionMode sendMode, GroupPermissionMode modifyMode, GroupPermissionMode dissolutionMode) {
        if (database.getDatabaseType() == DatabaseType.MYSQL) {
            return database.execute(
                "INSERT INTO " + NAME + " (name, owner, sendMode, modifyMode, dissolutionMode) VALUES (" +
                    "'" + name + "', " +
                    mailSenderTable.getId(owner) + ", " +
                    sendMode.ordinal() + ", " +
                    modifyMode.ordinal() + ", " +
                    dissolutionMode.ordinal() +
                ") ON DUPLICATE KEY UPDATE " +
                    "owner = VALUES(owner), " +
                    "sendMode = VALUES(sendMode), " +
                    "modifyMode = VALUES(modifyMode), " +
                    "dissolutionMode = VALUES(dissolutionMode)"
            );
        } else if (database.getDatabaseType() == DatabaseType.SQLITE) {
            return database.execute(
                "INSERT INTO " + NAME + " (name, owner, sendMode, modifyMode, dissolutionMode) VALUES (" +
                    "'" + name + "', " +
                    mailSenderTable.getId(owner) + ", " +
                    sendMode.ordinal() + ", " +
                    modifyMode.ordinal() + ", " +
                    dissolutionMode.ordinal() +
                ") ON CONFLICT(name) DO UPDATE SET " +
                    "owner = excluded.owner, " +
                    "sendMode = excluded.sendMode, " +
                    "modifyMode = excluded.modifyMode, " +
                    "dissolutionMode = excluded.dissolutionMode"
            );
        } else {
            return false;
        }
    }

    public String fixCase(String groupName) {
        return database.query("SELECT name FROM " + NAME + " WHERE name = '" + groupName + "'", rs -> {
            try {
                return rs.next() ? rs.getString("name") : null;
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    public ArrayList<String> getNames() {
        return getNamesWhere(null);
    }

    public boolean exists(String groupName) {
        return database.query("SELECT name FROM " + NAME + " WHERE name = '" + groupName + "'", rs -> {
            try {
                return rs.next();
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    private ArrayList<String> getNamesWhere(String where) {
        return database.query("SELECT name FROM " + NAME + (where == null || where.isEmpty() ? "" : " " + where), rs -> {
            try {
                ArrayList<String> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(rs.getString("name"));
                }
                return result;
            } catch (SQLException e) {
                e.printStackTrace();
                return new ArrayList<>();
            }
        });
    }

    public void retainExistsByName(List<String> names) {
        if (!names.isEmpty()) {
            names.retainAll(getNamesWhere("WHERE name " + Database.createIn(names)));
        }
    }

    public ArrayList<String> getNamesByOwner(int ownerId) {
        if (ownerId <= 0) {
            return new ArrayList<>();
        }
        return getNamesWhere("WHERE owner = " + ownerId);
    }

    public int getOwnerId(String groupName) {
        return database.query("SELECT owner FROM " + NAME + " WHERE name = '" + groupName + "'", rs -> {
            try {
                if (rs.next()) {
                    return rs.getInt("owner");
                }
                return null;
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    public void setOwner(String groupName, int ownerId) {
        if (ownerId > 0) {
            database.execute("UPDATE " + NAME + " SET owner = " + ownerId + " WHERE name = '" + groupName + "'");
        }
    }

    private GroupPermissionMode getMode(String groupName, String columnName) {
        return database.query("SELECT " + columnName + " FROM " + NAME + " WHERE name = '" + groupName + "'", rs -> {
            try {
                if (rs.next()) {
                    return GroupPermissionMode.values()[(int)rs.getByte(columnName)];
                }
                return null;
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        });
    }
    
    public GroupPermissionMode getSendMode(String groupName) {
        return getMode(groupName, "sendMode");
    }
    
    public GroupPermissionMode getModifyMode(String groupName) {
        return getMode(groupName, "modifyMode");
    }
    
    public GroupPermissionMode getDissolutionMode(String groupName) {
        return getMode(groupName, "dissolutionMode");
    }
    
    private void setMode(String groupName, String columnName, GroupPermissionMode mode) {
        database.execute("UPDATE " + NAME + " SET " + columnName + " = " + mode.ordinal() + " WHERE name = '" + groupName + "'");
    }

    public void setSendMode(String groupName, GroupPermissionMode mode) {
        setMode(groupName, "sendMode", mode);
    }

    public void setModifyMode(String groupName, GroupPermissionMode mode) {
        setMode(groupName, "modifyMode", mode);
    }

    public void setDissolutionMode(String groupName, GroupPermissionMode mode) {
        setMode(groupName, "dissolutionMode", mode);
    }

    public boolean deleteByName(String groupName) {
        return database.execute("DELETE FROM " + NAME + " WHERE name = '" + groupName + "'");
    }

    public void deleteByNames(List<String> groupNames) {
        if (!groupNames.isEmpty()) {
            database.execute("DELETE FROM " + NAME + " WHERE name " + Database.createIn(groupNames));
        }
    }

    public void deleteAll() {
        database.execute("DELETE FROM " + NAME);
    }

    /**
     * グループ名のリストを用いてデータベースからデータをロードし、フラットファイルに保存可能なグループクラスを作成する。
     * 
     * @param groupNames 読み込むグループ名のリスト。
     * @return 読み込まれたグループデータのリスト
     */
    public ArrayList<GroupDataFlatFile> getAsFlatFiles(List<String> groupNames) {
        if (groupNames.isEmpty()) {
            groupNames = new ArrayList<>();
        }

        Map<String, GroupDataFlatFile> nameGroupMap = new HashMap<>();
        
        // OwnerをIdからMailSenderに一斉変換するためにマップを作っておく。
        Map<String, Integer> groupNameOwnerMap = new HashMap<>();

        String inNames = Database.createIn(groupNames);
        String select = "SELECT name, owner, sendMode, modifyMode, dissolutionMode FROM " + NAME + " WHERE name " + inNames;
        database.query(select, rs -> {
            try {
                // 指定した名前を持つグループのデータを一気に作る。ただし、オーナーはnullにしておいて後で代入する。
                while (rs.next()) {
                    String name = rs.getString("name");

                    GroupDataFlatFile data = new GroupDataFlatFile(UndineMailer.getInstance(), name, null);
                    GroupPermissionMode[] groupPermissions = GroupPermissionMode.values();
                    data.setSendMode(groupPermissions[(int)rs.getByte("gd.sendMode")]);
                    data.setModifyMode(groupPermissions[(int)rs.getByte("gd.modifyMode")]);
                    data.setDissolutionMode(groupPermissions[(int)rs.getByte("gd.dissolutionMode")]);

                    nameGroupMap.put(name, data);
                    groupNameOwnerMap.put(name, rs.getInt("owner"));
                }
                return null; // なにも返さない。
            } catch (SQLException e) {
                e.printStackTrace();
                nameGroupMap.clear();
                return null;
            }
        });

        // 指定した名前のグループがデータベースにない。
        if (nameGroupMap.isEmpty()) {
            return new ArrayList<>();
        }

        // 取得した各グループのオーナーを一斉に変換する。
        Map<Integer, MailSender> idOwnerMap = mailSenderTable.getByIds(new ArrayList<>(groupNameOwnerMap.values()));
        // 変換したオーナーをセットする。
        nameGroupMap.forEach((name, group) -> group.setOwner(idOwnerMap.get(groupNameOwnerMap.get(name))));

        Map<String, List<Integer>> membersMap = new HashMap<>();
        Set<Integer> mailSenderIds = new HashSet<>();

        database.query(
            "SELECT groupName, member FROM " + GroupMembersTable.NAME + "WHERE groupName " + inNames,
        rs -> {
            try {
                while (rs.next()) {
                    String name = rs.getString("groupName");
                    int memberId = rs.getInt("member");
                    mailSenderIds.add(memberId);

                    if (membersMap.containsKey(name)) {
                        membersMap.put(name, new ArrayList<>());
                    }
                    List<Integer> members = membersMap.get(name);
                    members.add(memberId);
                }
                return null; // 何も返さない。
            } catch (SQLException e) {
                e.printStackTrace();
                return null; // 何も返さない。
            }
        });

        Map<Integer, MailSender> idMemberMap = mailSenderTable.getByIds(new ArrayList<>(mailSenderIds));
        membersMap.forEach((groupName, members) -> {
            GroupDataFlatFile group = nameGroupMap.get(groupName);
            for (int memberId : membersMap.get(groupName)) {
                group.addMember(idMemberMap.get(memberId));
            }
        });

        return new ArrayList<>(nameGroupMap.values());
    }
}
