/*
 * @author     LazyGon
 * @license    LGPLv3
 * @copyright  Copyright OKOCRAFT 2020
 */
package org.bitbucket.ucchy.undine.database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bitbucket.ucchy.undine.database.Database.DatabaseType;

/**
 * 編集中メールの宛先を保持するテーブルにアクセスするクラス。
 * @author LazyGon
 */
public class DraftMailRecipientsTable {

    public static final String NAME = "undine_draftmailrecipients";

    private final Database database;
    @SuppressWarnings("unused")
    private final MailSenderTable mailSenderTable;
    @SuppressWarnings("unused")
    private final DraftMailDataTable draftMailDataTable;

    DraftMailRecipientsTable(Database database, MailSenderTable mailSenderTable, DraftMailDataTable draftMailDataTable) {
        this.database = database;
        this.mailSenderTable = mailSenderTable;
        this.draftMailDataTable = draftMailDataTable;
        createTable();
    }

    void createTable() {
        database.execute(
            "CREATE TABLE IF NOT EXISTS " + NAME + " (" +
                "mailId INTEGER NOT NULL, " +
                "recipient INTEGER NOT NULL, " +
                "PRIMARY KEY (mailId, recipient), " +
                "FOREIGN KEY (mailId) REFERENCES " + DraftMailDataTable.NAME + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                "FOREIGN KEY (recipient) REFERENCES " + MailSenderTable.NAME + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
            ")"
        );
    }

    /**
     * 指定された受信者を宛先に含むメールのIDをすべて取得する。
     * @param recipientId 受信者
     * @return メールのID
     */
    public ArrayList<Integer> getMailIdsByRecipient(int recipientId) {
        if (recipientId == -1) {
            return new ArrayList<>();
        }
        return database.query("SELECT mailId FROM " + NAME + " WHERE recipient = " + recipientId, rs -> {
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
     * 指定されたIDのメールの宛先の受信者のIDをすべて取得する。
     * @param mailId メールのID
     * @return 受信者のIDのリスト
     */
    public ArrayList<Integer> getRecipients(int mailId) {
        return database.query("SELECT recipient FROM " + NAME + " WHERE mailId = " + mailId, rs -> {
            try {
                ArrayList<Integer> ids = new ArrayList<>();
                while (rs.next()) {
                    ids.add(rs.getInt("recipient"));
                }
                return ids;
            } catch (SQLException e) {
                e.printStackTrace();
                return new ArrayList<>();
            }
        });
    }

    /**
     * 指定された受信者をすべて指定されたIDのメールに追加する。
     * @param mailId メールのID
     * @param recipientIds 受信者のリスト
     */
    public void addRecipients(int mailId, List<Integer> recipientIds) {
        if (recipientIds.isEmpty()) {
            return;
        }
        StringBuilder valuesBuilder = new StringBuilder();
        for (int recipientId : recipientIds) {
            valuesBuilder.append("(").append(mailId).append(", ").append(recipientId).append("), ");
        }
        valuesBuilder.delete(valuesBuilder.length() - 2, valuesBuilder.length());

        if (database.getDatabaseType() == DatabaseType.MYSQL) {
            database.execute("INSERT INTO " + NAME + " (mailId, recipient) VALUES " + valuesBuilder.toString() + " ON DUPLICATE KEY UPDATE recipient = recipient");
        } else if (database.getDatabaseType() == DatabaseType.SQLITE) {
            database.execute("INSERT INTO " + NAME + " (mailId, recipient) VALUES " + valuesBuilder.toString() + " ON CONFLICT(mailId, recipient) DO NOTHING");
        }
    }

    /**
     * 指定された受信者を指定されたIDのメールの宛先に追加する。
     * @param mailId メールのID
     * @param recipientId 受信者
     */
    public void addRecipient(int mailId, int recipientId) {
        addRecipients(mailId, Arrays.asList(recipientId));
    }

    /**
     * 指定された受信者を指定されたIDのメールの宛先から削除する。
     * @param mailId メールのID
     * @param recipientId 受信者
     */
    public void removeRecipient(int mailId, int recipientId) {
        if (recipientId != -1) {
            database.execute("DELETE FROM " + NAME + " WHERE mailId = " + mailId + " AND recipient = " + recipientId);
        }
    }

    /**
     * 指定されたIDのメールの宛先をすべて削除する。
     * @param mailId メールのID
     */
    public void clearRecipient(int mailId) {
        database.execute("DELETE FROM " + NAME + " WHERE mailId = " + mailId);
    }
}
