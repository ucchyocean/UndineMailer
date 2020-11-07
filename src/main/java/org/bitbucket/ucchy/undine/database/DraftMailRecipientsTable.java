package org.bitbucket.ucchy.undine.database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bitbucket.ucchy.undine.database.Database.DatabaseType;

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

    public ArrayList<Integer> getRecipients(int id) {
        return database.query("SELECT recipient FROM " + NAME + " WHERE mailId = " + id, rs -> {
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

    public void addRecipients(int id, List<Integer> recipientIds) {
        if (recipientIds.isEmpty()) {
            return;
        }
        StringBuilder valuesBuilder = new StringBuilder();
        for (int recipientId : recipientIds) {
            valuesBuilder.append("(").append(id).append(", ").append(recipientId).append("), ");
        }
        valuesBuilder.delete(valuesBuilder.length() - 2, valuesBuilder.length());

        if (database.getDatabaseType() == DatabaseType.MYSQL) {
            database.execute("INSERT INTO " + NAME + " (mailId, recipient) VALUES " + valuesBuilder.toString() + " ON DUPLICATE KEY UPDATE recipientId = recipientId");
        } else if (database.getDatabaseType() == DatabaseType.SQLITE) {
            database.execute("INSERT INTO " + NAME + " (mailId, recipient) VALUES " + valuesBuilder.toString() + " ON CONFLICT(mailId, recipient) DO NOTHING");
        }
    }

    public void addRecipient(int id, int recipientId) {
        addRecipients(id, Arrays.asList(recipientId));
    }

    public void removeRecipient(int id, int recipientId) {
        if (recipientId != -1) {
            database.execute("DELETE FROM " + NAME + " WHERE mailId = " + id + " AND recipient = " + recipientId);
        }
    }

    public void clearRecipient(int id) {
        database.execute("DELETE FROM " + NAME + " WHERE mailId = " + id);
    }
}
