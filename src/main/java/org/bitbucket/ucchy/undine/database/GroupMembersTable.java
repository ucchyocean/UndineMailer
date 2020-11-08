package org.bitbucket.ucchy.undine.database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.bitbucket.ucchy.undine.database.Database.DatabaseType;

/**
 * グループメンバーを保持するテーブルにアクセスするクラス。
 * @author LazyGon
 */
public class GroupMembersTable {

    public static final String NAME = "undine_groupmembers";

    private final Database database;
    @SuppressWarnings("unused")
    private final MailSenderTable mailSenderTable;
    @SuppressWarnings("unused")
    private final GroupDataTable groupDataTable;

    GroupMembersTable(Database database, MailSenderTable mailSenderTable, GroupDataTable groupDataTable) {
        this.database = database;
        this.mailSenderTable = mailSenderTable;
        this.groupDataTable = groupDataTable;
        createTable();
    }

    void createTable() {
        database.execute(
            "CREATE TABLE IF NOT EXISTS " + NAME + " (" +
                "groupName VARCHAR(64) NOT NULL" + (database.getDatabaseType() == DatabaseType.SQLITE ? " COLLATE NOCASE" : "") + ", " +
                "member INTEGER NOT NULL, " +
                "PRIMARY KEY (groupName, member), " +
                "FOREIGN KEY (groupName) REFERENCES " + GroupDataTable.NAME + "(name) ON DELETE CASCADE ON UPDATE CASCADE, " +
                "FOREIGN KEY (member) REFERENCES " + MailSenderTable.NAME + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
            ")"
        );
    }

    /**
     * 指定されたすべてのグループのメンバーの和集合を取得する。
     * @param groupNames グループ名のリスト
     * @return 各グループのメンバーをまとめたリスト
     */
    public ArrayList<Integer> getMemberIdsOf(List<String> groupNames) {
        if (groupNames.isEmpty()) {
            return new ArrayList<>();
        }
        return database.query(
            "SELECT member FROM " + NAME + " WHERE groupName " + Database.createIn(groupNames),
        rs -> {
            try {
                ArrayList<Integer> ids = new ArrayList<>(); 
                while (rs.next()) {
                    ids.add(rs.getInt("member"));
                }
                return ids;
            } catch (SQLException e) {
                e.printStackTrace();
                return new ArrayList<>();
            }
        });
    }

    /**
     * 指定したグループのメンバーを取得する。
     * @param groupName グループ
     * @return メンバーのリスト
     */
    public ArrayList<Integer> getMemberIdsOf(String groupName) {
        return database.query(
            "SELECT member FROM " + NAME + " WHERE groupName = '" + groupName + "'",
        rs -> {
            try {
                ArrayList<Integer> ids = new ArrayList<>(); 
                while (rs.next()) {
                    ids.add(rs.getInt("member"));
                }
                return ids;
            } catch (SQLException e) {
                e.printStackTrace();
                return new ArrayList<>();
            }
        });
    }

    /**
     * 指定したグループから指定したメンバーを削除する。
     * @param groupName グループ 
     * @param memberId メンバー
     * @return 削除したかどうか
     */
    public boolean removeMember(String groupName, int memberId) {
        return database.execute(
            "DELETE FROM " + NAME + " WHERE " +
                "member = " + memberId + " AND " +
                "groupName = '" + groupName + "'"
        );
    }
    
    /**
     * 指定したすべてのグループから、指定されたメンバーを削除する。
     * @param memberId メンバー
     * @param groupNames グループのリスト
     */
    public void removeMemberFromGroups(int memberId, ArrayList<String> groupNames) {
        database.execute(
            "DELETE FROM " + NAME + " WHERE " +
                "member = " + memberId + " AND " +
                "groupName " + Database.createIn(groupNames)
        );
    }

    /**
     * 指定されたメンバーをすべてのグループから削除する。
     * @param memberId メンバー
     */
    public void removeMemberFromAllGroups(int memberId) {
        database.execute("DELETE FROM " + NAME + " WHERE member = " + memberId);        
    }

    /**
     * 指定されたグループのメンバーをすべて削除する。
     * @param groupName グループ
     */
    public void clearMembers(String groupName) {
        database.execute("DELETE FROM" + NAME + " WHERE groupName = '" + groupName + "'");
    }

    /**
     * 指定されたグループにメンバーを追加する。
     * @param groupName グループ
     * @param memberId メンバー
     * @return 追加したか
     */
    public boolean addMember(String groupName, int memberId) {
        String insert = "INSERT INTO " + NAME + " (groupName, member) VALUES ('" + groupName + "', " + memberId + ")";
        if (database.getDatabaseType() == DatabaseType.MYSQL) {
            database.execute(insert + " ON DUPLICATE KEY UPDATE groupName = groupName");
        } else if (database.getDatabaseType() == DatabaseType.SQLITE) {
            database.execute(insert + " ON CONFLICT(groupName, member) DO NOTHING");
        } 
        return true;
    }

    /**
     * 指定されたグループにはいっているメンバーかどうか調べる。
     * @param groupName グループ
     * @param memberId メンバー
     * @return メンバーだったらtrue
     */
    public boolean isMember(String groupName, int memberId) {
        return database.query("SELECT member FROM " + NAME + " WHERE groupName = '" + groupName + "'", rs -> {
            try {
                return rs.next();
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * 指定されたすべてのメンバーを指定されたグループに追加する。
     * @param groupName グループ
     * @param memberIds メンバーのリスト
     */
    public void addAllToGroup(String groupName, ArrayList<Integer> memberIds) {
        StringBuilder valueBuilder = new StringBuilder();
        for (Integer memberId : memberIds) {
            if (memberId != null && memberId > 0) {
                valueBuilder.append("('").append(groupName).append("', ").append(memberId).append("), ");
            }
        }
        if (valueBuilder.length() == 0) {
            return;
        }
        valueBuilder.delete(valueBuilder.length() - 2, valueBuilder.length());

        String insert = "INSERT INTO " + NAME + " (groupName, member) VALUES " + valueBuilder.toString();
        if (database.getDatabaseType() == DatabaseType.MYSQL) {
            database.execute(insert + " ON DUPLICATE KEY UPDATE groupName = groupName");
        } else if (database.getDatabaseType() == DatabaseType.SQLITE) {
            database.execute(insert + " ON CONFLICT(groupName, member) DO NOTHING");
        } 
    }

    /**
     * メンバーからが所属しているすべてのグループを取得する。
     * @param memberId メンバー
     * @return グループのリスト
     */
    public ArrayList<String> getBelongingGroups(int memberId) {
        return getGroupsWhere("WHERE member = " + memberId);
    }

    /**
     * WHERE句を指定してグループを取得する。
     * @param where SQL文のWHERE句部分
     * @return 条件に当てはまったグループのリスト
     */
    public ArrayList<String> getGroupsWhere(String where) {
        return database.query("SELECT groupName FROM " + NAME + (where == null || where.isBlank() ? "" : " " + where), rs -> {
            try {
                ArrayList<String> groupNames = new ArrayList<>();
                while (rs.next()) {
                    groupNames.add(rs.getString("groupName"));
                }
                return groupNames;
            } catch (SQLException e) {
                e.printStackTrace();
                return new ArrayList<>();
            }
        });
    }
}
