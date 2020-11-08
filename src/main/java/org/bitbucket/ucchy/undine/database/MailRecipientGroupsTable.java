/*
 * @author     LazyGon
 * @license    LGPLv3
 * @copyright  Copyright OKOCRAFT 2020
 */
package org.bitbucket.ucchy.undine.database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.bitbucket.ucchy.undine.database.Database.DatabaseType;

/**
 * メールの宛先グループを保持するテーブルにアクセスするクラス。
 * @author LazyGon
 */
public class MailRecipientGroupsTable {
    
    public static final String NAME = "undine_mailrecipientgroups";

    private final Database database;
    @SuppressWarnings("unused")
    private final MailDataTable mailDataTable;
    @SuppressWarnings("unused")
    private final GroupDataTable groupDataTable;
    
    MailRecipientGroupsTable(Database database, MailDataTable mailDataTable, GroupDataTable groupDataTable) {
        this.database = database;
        this.mailDataTable = mailDataTable;
        this.groupDataTable = groupDataTable;
        createTable();
    }

    void createTable() {
        database.execute(
            "CREATE TABLE IF NOT EXISTS " + NAME + " (" +
                "mailId INTEGER NOT NULL, " +
                "recipientGroup VARCHAR(64) NOT NULL" + (database.getDatabaseType() == DatabaseType.SQLITE ? " COLLATE NOCASE" : "") + ", " +
                "PRIMARY KEY (mailId, recipientGroup), " +
                "FOREIGN KEY (mailId) REFERENCES " + MailDataTable.NAME + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                "FOREIGN KEY (recipientGroup) REFERENCES " + GroupDataTable.NAME + "(name) ON DELETE CASCADE ON UPDATE CASCADE" +
            ")"
        );
    }

    /**
     * 指定したグループを含むメールのidをすべて取得する。
     * @param groupName グループ
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
     * 指定したgroupのどれか一つでも含むメールのidをすべて取得する。
     * @param groupNames グループIDのリスト
     * @return メールIDのリスト
     */
    public ArrayList<Integer> getMailIdsByGroups(List<String> groupNames) {
        if (groupNames.isEmpty()) {
            return new ArrayList<>();
        }
        return database.query(
            "SELECT mailId FROM " + NAME + " WHERE recipientGroup " + Database.createIn(groupNames), rs -> {
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
            database.execute("INSERT INTO " + NAME + " (mailId, recipientGroup) VALUES (" + mailId + ", '" + groupName + "') ON DUPLICATE KEY UPDATE recipientGroup = recipientGroup");
        } else if (database.getDatabaseType() == DatabaseType.SQLITE) {
            database.execute("INSERT INTO " + NAME + " (mailId, recipientGroup) VALUES (" + mailId + ", '" + groupName + "') ON CONFLICT(mailId, recipientGroup) DO NOTHING");
        }
    }

    /**
     * 指定されたグループを指定されたIDのメールの宛先から削除する。
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
