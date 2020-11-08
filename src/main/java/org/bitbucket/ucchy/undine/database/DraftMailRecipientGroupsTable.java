package org.bitbucket.ucchy.undine.database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.bitbucket.ucchy.undine.database.Database.DatabaseType;

/**
 * 編集中テーブルの宛先グループを保持するテーブルにアクセスするクラス。
 * @author LazyGon
 */
public class DraftMailRecipientGroupsTable {

    public static final String NAME = "undine_draftmailrecipientgroups";

    private final Database database;
    @SuppressWarnings("unused")
    private final DraftMailDataTable draftMailDataTable;
    @SuppressWarnings("unused")
    private final GroupDataTable groupDataTable;

    DraftMailRecipientGroupsTable(Database database, DraftMailDataTable draftMailDataTable, GroupDataTable groupDataTable) {
        this.database = database;
        this.draftMailDataTable = draftMailDataTable;
        this.groupDataTable = groupDataTable;
        createTable();
    }

    void createTable() {
        database.execute(
            "CREATE TABLE IF NOT EXISTS " + NAME + " (" +
                "mailId INTEGER NOT NULL, " +
                "recipientGroup VARCHAR(64) NOT NULL" + (database.getDatabaseType() == DatabaseType.SQLITE ? " COLLATE NOCASE" : "") + ", " +
                "PRIMARY KEY (mailId, recipientGroup), " +
                "FOREIGN KEY (mailId) REFERENCES " + DraftMailDataTable.NAME + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                "FOREIGN KEY (recipientGroup) REFERENCES " + GroupDataTable.NAME + "(name) ON DELETE CASCADE ON UPDATE CASCADE" +
            ")"
        );
    }

    /**
     * toGroupに指定したgroupを含むメールのidをすべて取得する。
     * @param group グループ
     * @return メールIDのリスト
     */
    public ArrayList<Integer> getMailIdsByGroup(String groupName) {
        return database.query("SELECT mailId FROM " + NAME + " WHERE recipientGroup = '" + groupName + "'", rs -> {
            try {
                ArrayList<Integer> ids = new ArrayList<>();
                while (rs.next()) {
                    ids.add(rs.getInt("mailId"));
                }
                return ids;
            } catch (SQLException e) {
                e.printStackTrace();
                return new ArrayList<>();
            }
        });
    }

    /**
     * 指定したgroupをどれか一つでも送信先に含んでいるメールのidをすべて取得する。
     * @param groups グループIDのリスト
     * @return メールIDのリスト
     */
    public ArrayList<Integer> getMailIdsByGroups(List<String> groupNames) {
        if (groupNames.isEmpty()) {
            return new ArrayList<>();
        }
        return database.query(
            "SELECT mailId FROM " + NAME + " WHERE recipientGroup " + Database.createIn(groupNames),
        rs -> {
            try {
                ArrayList<Integer> ids = new ArrayList<>();
                while (rs.next()) {
                    ids.add(rs.getInt("mailId"));
                }
                return ids;
            } catch (SQLException e) {
                e.printStackTrace();
                return new ArrayList<>();
            }
        });
    }
    
    /**
     * 指定されたIDのメールが宛先としているグループをすべて取得する。
     * @param mailId メールのID
     * @return グループのリスト
     */
    public ArrayList<String> getGroups(int mailId) {
        return database.query("SELECT recipientGroup FROM " + NAME + " WHERE mailId = " + mailId, rs -> {
            try {
                ArrayList<String> groupNames = new ArrayList<>();
                while (rs.next()) {
                    groupNames.add(rs.getString("recipientGroup"));
                }
                return groupNames;
            } catch (SQLException e) {
                e.printStackTrace();
                return new ArrayList<>();
            }
        });
    }

    /**
     * 指定されたグループを指定されたIDのメールの宛先に追加する。
     * @param mailId メールのID
     * @param groupName グループ
     */
    public void addGroup(int mailId, String groupName) {
        if (database.getDatabaseType() == DatabaseType.MYSQL) {
            database.execute("INSERT INTO " + NAME + " (mailId, recipientGroup) VALUES (" + mailId + ", '" + groupName + "') ON DUPLICATE KEY UPDATE mailId = mailId");
        } else if (database.getDatabaseType() == DatabaseType.SQLITE) {
            database.execute("INSERT INTO " + NAME + " (mailId, recipientGroup) VALUES (" + mailId + ", '" + groupName + "') ON CONFLICT(mailId, recipientGroup) DO NOTHING");
        }
    }

    /**
     * 指定されたグループを指定されたメールの宛先から削除する。
     * @param mailId メールのID
     * @param groupName グループ
     */
    public void removeGroup(int mailId, String groupName) {
        database.execute("DELETE FROM " + NAME + " WHERE mailId = " + mailId + " AND recipientGroup = '" + groupName + "'");
    }

    /**
     * 指定されたIDのメールの宛先グループをすべて削除する。
     * @param mailId メールのID
     */
    public void clearGroup(int mailId) {
        database.execute("DELETE FROM " + NAME + " WHERE mailId = " + mailId);
    }
}