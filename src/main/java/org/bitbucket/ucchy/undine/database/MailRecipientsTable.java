package org.bitbucket.ucchy.undine.database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bitbucket.ucchy.undine.database.Database.DatabaseType;

public class MailRecipientsTable {
    
    public static final String NAME = "undine_mailrecipients";

    private final Database database;
    private final MailDataTable mailDataTable;
    @SuppressWarnings("unused")
    private final MailSenderTable mailSenderTable;

    MailRecipientsTable(Database database, MailSenderTable mailSenderTable,  MailDataTable mailDataTable) {
        this.database = database;
        this.mailSenderTable = mailSenderTable;
        this.mailDataTable = mailDataTable;
        createTable();
    }

    void createTable() {
        database.execute(
            "CREATE TABLE IF NOT EXISTS " + NAME + " (" +
                "mailId INTEGER NOT NULL, " +
                "recipient INTEGER NOT NULL, " +
                "isRead TINYINT NOT NULL DEFAULT 0, " +
                "isTrash TINYINT NOT NULL DEFAULT 0, " +
                "PRIMARY KEY (mailId, recipient), " +
                "FOREIGN KEY (mailId) REFERENCES " + MailDataTable.NAME + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
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
        return getRecipientsWhere("WHERE mailId = " + id);
    }

    public ArrayList<Integer> getRecipientsWhere(String where) {
        return database.query("SELECT recipient FROM " + NAME + (where == null || where.isEmpty() ? "" : " " + where), rs -> {
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
            database.execute("INSERT INTO " + NAME + " (mailId, recipient) VALUES " + valuesBuilder.toString() + " ON DUPLICATE KEY UPDATE recipient = recipient");
        } else if (database.getDatabaseType() == DatabaseType.SQLITE) {
            database.execute("INSERT INTO " + NAME + " (mailId, recipient) VALUES " + valuesBuilder.toString() + " ON CONFLICT(mailId, recipient) DO NOTHING");
        }
    }

    public void addRecipient(int id, int recipientId) {
        addRecipients(recipientId, Arrays.asList(recipientId));
    }

    public void removeRecipient(int id, int recipientId) {
        if (recipientId != -1) {
            database.execute("DELETE FROM " + NAME + " WHERE mailId = " + id + " AND recipient = " + recipientId);
        }
    }

    public void clearRecipient(int id) {
        database.execute("DELETE FROM " + NAME + " WHERE mailId = " + id);
    }

    public ArrayList<Integer> getWhoRead(int id) {
        return getRecipientsWhere("WHERE mailId = " + id + " AND isRead = 1");
    }

    public ArrayList<Integer> getWhoTrash(int id) {
        return getRecipientsWhere("WHERE mailId = " + id + " AND isTrash = 1");
    }

    public boolean isRead(int id, int recipientId) {
        return getBool(id, "isRead", recipientId);
    }

    public Map<Integer, Boolean> isReadAll(List<Integer> ids, int recipientId) {
        return getBoolAll(ids, "isRead", recipientId);
    }

    public Map<Integer, Boolean> isReadAll(int recipientId) {
        return getBoolAll("isRead", recipientId);
    }

    public void setRead(int id, int recipientId, boolean isRead) {
        setBool(id, "isRead", recipientId, isRead);
    }

    public void setReadAll(List<Integer> ids, int recipientId, boolean isRead) {
        setBoolAll(ids, "isRead", recipientId, isRead);
    }

    public void setReadAll(int recipientId, boolean isRead) {
        setBoolAll("isRead", recipientId, isRead);
    }

    public boolean isTrash(int id, int recipientId) {
        return getBool(id, "isTrash", recipientId);
    }

    public Map<Integer, Boolean> isTrashAll(List<Integer> ids, int recipientId) {
        return getBoolAll(ids, "isTrash", recipientId);
    }

    public Map<Integer, Boolean> isTrashAll(int recipientId) {
        return getBoolAll("isTrash", recipientId);
    }

    public void setTrash(int id, int recipientId, boolean isTrash) {
        setBool(id, "isTrash", recipientId, isTrash);
    }

    public void setTrashAll(List<Integer> ids, int recipientId, boolean isTrash) {
        setBoolAll(ids, "isTrash", recipientId, isTrash);
    }

    public void setTrashAll(int recipientId, boolean isTrash) {
        setBoolAll("isTrash", recipientId, isTrash);
    }

    private Map<Integer, Boolean> getBoolAll(List<Integer> ids, String prop, int recipientId) {
        if (recipientId != -1 && !ids.isEmpty()) {
            return getBoolWhere(prop, "WHERE mailId " + Database.createIn(ids) + " AND recipient = " + recipientId);
        }
        return new HashMap<>();
    }

    private Map<Integer, Boolean> getBoolAll(String prop, int recipientId) {
        if (recipientId != -1) {
            return getBoolWhere(prop, "WHERE recipient = " + recipientId);
        }
        return new HashMap<>();
    }


    private void setBoolAll(String prop, int recipientId, boolean value) {
        setBoolAll(mailDataTable.getIdsByRecipient(recipientId), prop, recipientId, value);
    }

    private boolean getBool(int id, String prop, int recipientId) {
        if (recipientId != -1) {
            return getBoolWhere(prop, "WHERE mailId = " + id + " AND recipient = " + recipientId).getOrDefault(id, false);
        }
        return false;
    }

    private void setBool(int id, String prop, int recipientId, boolean value) {
        setBoolAll(Arrays.asList(id), prop, recipientId, value);
    }

    private void setBoolAll(List<Integer> ids, String prop, int recipientId, boolean value) {
        if (recipientId == -1 || ids.isEmpty()) {
            return;
        }
        
        StringBuilder valuesBuilder = new StringBuilder();
        for (Integer id : ids) {
            valuesBuilder.append("(").append(id).append(", ").append(recipientId).append(", ").append(value ? 1 : 0).append("), ");
        }
        valuesBuilder.delete(valuesBuilder.length() - 2, valuesBuilder.length());

        String insert = "INSERT INTO " + NAME + " (mailId, recipient, " + prop + ") VALUES " + valuesBuilder.toString() + " ";
        if (database.getDatabaseType() == DatabaseType.MYSQL) {
            database.execute(insert + " ON DUPLICATE KEY UPDATE " + prop + " = " + (value ? 1 : 0));
        } else if (database.getDatabaseType() == DatabaseType.SQLITE) {
            database.execute(insert + " ON CONFLICT(mailId, recipient) DO UPDATE SET " + prop + " = " + (value ? 1 : 0));
        }
    }

    private Map<Integer, Boolean> getBoolWhere(String prop, String where) {
        Map<Integer, Boolean> result = new HashMap<>();
        return database.query("SELECT mailId, " + prop + " FROM " + NAME + (where.isEmpty() ? "" : " ") + where, rs -> {
            try {
                while (rs.next()) {
                    result.put(rs.getInt("mailId"), rs.getByte(prop) == (byte)1);
                }
                return result;
            } catch (SQLException e) {
                e.printStackTrace();
                return result;
            }
        });
    }
}
