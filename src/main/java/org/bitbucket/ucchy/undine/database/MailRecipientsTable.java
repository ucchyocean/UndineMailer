package org.bitbucket.ucchy.undine.database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bitbucket.ucchy.undine.database.Database.DatabaseType;

/**
 * メールの宛先を保持するテーブルにアクセスするクラス。
 * @author LazyGon
 */
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

    /**
     * 指定された受信者を宛先に含むメールのIDを全て取得する。
     * @param recipientId 受信者
     * @return メールのIDのリスト
     */
    public ArrayList<Integer> getMailIdsByRecipient(int recipientId) {
        if (recipientId == -1) {
            return new ArrayList<>();
        }
        return database.query("SELECT mailId FROM " + NAME + " WHERE recipient = " + recipientId, rs -> {
            try {
                ArrayList<Integer> mailIds = new ArrayList<>();
                while (rs.next()) {
                    mailIds.add(rs.getInt("mailId"));
                }
                return mailIds;
            } catch (SQLException e) {
                e.printStackTrace();
                return new ArrayList<>();
            }
        });
    }

    /**
     * 指定されたIDのメールの受信者をすべて取得する。
     * @param mailId メールのID
     * @return 受信者のリスト
     */
    public ArrayList<Integer> getRecipients(int mailId) {
        return getRecipientsWhere("WHERE mailId = " + mailId);
    }

    /**
     * 指定されたWHERE句で受信者を取得する。
     * @param where SQL文のWHERE句部分
     * @return 条件に当てはまった受信者
     */
    public ArrayList<Integer> getRecipientsWhere(String where) {
        return database.query("SELECT recipient FROM " + NAME + (where == null || where.isEmpty() ? "" : " " + where), rs -> {
            try {
                ArrayList<Integer> mailIds = new ArrayList<>();
                while (rs.next()) {
                    mailIds.add(rs.getInt("recipient"));
                }
                return mailIds;
            } catch (SQLException e) {
                e.printStackTrace();
                return new ArrayList<>();
            }
        });
    }

    /**
     * 指定されたすべての受信者を、指定されたIDのメールの宛先に追加する。
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
     * 指定されたメールの宛先をすべて削除する。
     * @param mailId メールのID
     */
    public void clearRecipient(int mailId) {
        database.execute("DELETE FROM " + NAME + " WHERE mailId = " + mailId);
    }

    /**
     * 指定されたメールに既読をつけた受信者をすべて取得する。
     * @param mailId メールのID
     * @return 受信者のリスト
     */
    public ArrayList<Integer> getWhoRead(int mailId) {
        return getRecipientsWhere("WHERE mailId = " + mailId + " AND isRead = 1");
    }

    /**
     * 指定されたメールをゴミ箱に移動した受信者をすべて取得する。
     * @param mailId メールのID
     * @return 受信者のリスト
     */
    public ArrayList<Integer> getWhoTrash(int mailId) {
        return getRecipientsWhere("WHERE mailId = " + mailId + " AND isTrash = 1");
    }

    /**
     * 指定されたIDのメールに受信者が既読をつけたかどうかを取得する。
     * @param mailId メールのID
     * @param recipientId 受信者
     * @return 既読したかどうか
     */
    public boolean isRead(int mailId, int recipientId) {
        return getBool(mailId, "isRead", recipientId);
    }

    /**
     * 指定されたメールのIDそれぞれについて、指定された受信者が既読をつけているかどうかを取得する。
     * @param mailIds メールのIDのリスト
     * @param recipientId 受信者
     * @return メールのIDと、既読されたかどうかのMap
     */
    public Map<Integer, Boolean> isReadAll(List<Integer> mailIds, int recipientId) {
        return getBoolAll(mailIds, "isRead", recipientId);
    }

    /**
     * すべてのメールについて、指定された受信者が既読をつけているかどうかを取得する。
     * @param recipientId 受信者
     * @return すべてのメールのIDと、既読されたかどうかのMap
     */
    public Map<Integer, Boolean> isReadAll(int recipientId) {
        return getBoolAll("isRead", recipientId);
    }

    /**
     * 指定されたIDのメールに、受信者が既読をつけたかどうかを設定する。
     * @param mailId メールのID
     * @param recipientId 受信者
     * @param isRead 既読をつけたか
     */
    public void setRead(int mailId, int recipientId, boolean isRead) {
        setBool(mailId, "isRead", recipientId, isRead);
    }

    /**
     * 指定されたすべてのメールに、受信者が既読をつけたかどうかを設定する。選択したメールをすべて既読にする機能を実装するときなどに利用できる。
     * @param mailIds メールのIDのリスト
     * @param recipientId 受信者
     * @param isRead 既読をつけたか
     */
    public void setReadAll(List<Integer> mailIds, int recipientId, boolean isRead) {
        setBoolAll(mailIds, "isRead", recipientId, isRead);
    }

    /**
     * すべてのメールに、受信者が既読をつけたかどうかを設定する。すべてのメールを既読にする機能を実装するときなどに利用できる。
     * @param recipientId 受信者
     * @param isRead 既読をつけたか
     */
    public void setReadAll(int recipientId, boolean isRead) {
        setBoolAll("isRead", recipientId, isRead);
    }

    /**
     * 指定されたIDのメールを受信者がゴミ箱に移動したかどうかを取得する。
     * @param mailId メールのID
     * @param recipientId 受信者
     * @return メールがゴミ箱に移動されているかどうか。
     */
    public boolean isTrash(int mailId, int recipientId) {
        return getBool(mailId, "isTrash", recipientId);
    }

    /**
     * 指定されたすべてのIDのメールを受信者がゴミ箱に移動したかどうかを取得する。
     * @param mailIds メールのIDのリスト
     * @param recipientId 受信者
     * @return メールがゴミ箱に移動されているかどうか。
     */
    public Map<Integer, Boolean> isTrashAll(List<Integer> mailIds, int recipientId) {
        return getBoolAll(mailIds, "isTrash", recipientId);
    }

    /**
     * 指定されたすべてのIDのメールを受信者がゴミ箱に移動したかどうかを取得する。
     * @param recipientId 受信者
     * @return メールがゴミ箱に移動されているかどうか
     */
    public Map<Integer, Boolean> isTrashAll(int recipientId) {
        return getBoolAll("isTrash", recipientId);
    }

    /**
     * 指定されたIDのメールを受信者がゴミ箱に移動したかどうかを設定する。
     * @param mailId メールのID
     * @param recipientId 受信者
     * @param isTrash ゴミ箱に移動したかどうか
     */
    public void setTrash(int mailId, int recipientId, boolean isTrash) {
        setBool(mailId, "isTrash", recipientId, isTrash);
    }

    /**
     * 指定されたすべてのメールを受信者がゴミ箱に移動したかどうかを設定する。選択したすべてのメールをゴミ箱に移動する機能を実装するときなどに利用できる。
     * @param mailIds メールのIDのリスト
     * @param recipientId 受信者
     * @param isTrash ゴミ箱に移動したかどうか
     */
    public void setTrashAll(List<Integer> mailIds, int recipientId, boolean isTrash) {
        setBoolAll(mailIds, "isTrash", recipientId, isTrash);
    }

    /**
     * すべてのメールを受信者がゴミ箱に移動したかどうかを設定する。すべてのメールをゴミ箱に移動する機能を実装するときなどに利用できる。
     * @param recipientId 受信者
     * @param isTrash ゴミ箱に移動したかどうか
     */
    public void setTrashAll(int recipientId, boolean isTrash) {
        setBoolAll("isTrash", recipientId, isTrash);
    }

    /**
     * 指定された受信者を宛先に持っているかつ指定されたIDであるメールについて、カラムの値を取得する。
     * @param mailIds メールのIDのリスト
     * @param column カラム
     * @param recipientId 受信者
     * @return 値
     */
    private Map<Integer, Boolean> getBoolAll(List<Integer> mailIds, String column, int recipientId) {
        if (recipientId != -1 && !mailIds.isEmpty()) {
            return getBoolWhere(column, "WHERE mailId " + Database.createIn(mailIds) + " AND recipient = " + recipientId);
        }
        return new HashMap<>();
    }

    /**
     * 指定された受信者を宛先に持つ全てのメールについて、カラムの値を取得する。
     * @param column カラム
     * @param recipientId 受信者
     * @return メールIDと値のMap
     */
    private Map<Integer, Boolean> getBoolAll(String column, int recipientId) {
        if (recipientId != -1) {
            return getBoolWhere(column, "WHERE recipient = " + recipientId);
        }
        return new HashMap<>();
    }


    /**
     * 指定された受信者を宛先に持つすべてのメールについて、カラムの値を設定する。
     * @param column カラム
     * @param recipientId 受信者
     * @param value 値
     */
    private void setBoolAll(String column, int recipientId, boolean value) {
        setBoolAll(mailDataTable.getIdsByRecipient(recipientId), column, recipientId, value);
    }

    /**
     * 指定された受信者を宛先にもっているかつ、指定されたIDのメールについて、カラムの値を取得する。
     * @param mailId メールのID
     * @param column カラム
     * @param recipientId 受信者
     * @return 値
     */
    private boolean getBool(int mailId, String column, int recipientId) {
        if (recipientId != -1) {
            return getBoolWhere(column, "WHERE mailId = " + mailId + " AND recipient = " + recipientId).getOrDefault(mailId, false);
        }
        return false;
    }

    /**
     * 指定された受信者を宛先にもつかつ、指定されたIDのメールについて、カラムの値を設定する。
     * @param mailId メールのID
     * @param column カラム
     * @param recipientId 受信者
     * @param value 値
     */
    private void setBool(int mailId, String column, int recipientId, boolean value) {
        setBoolAll(Arrays.asList(mailId), column, recipientId, value);
    }

    /**
     * 指定された受信者をもつかつ、指定されたすべてのIDのメールについて、カラムの値を設定する。
     * @param mailIds メールのIDのリスト
     * @param column カラム
     * @param recipientId 受信者
     * @param value 値
     */
    private void setBoolAll(List<Integer> mailIds, String column, int recipientId, boolean value) {
        if (recipientId == -1 || mailIds.isEmpty()) {
            return;
        }
        
        StringBuilder valuesBuilder = new StringBuilder();
        for (Integer mailId : mailIds) {
            valuesBuilder.append("(").append(mailId).append(", ").append(recipientId).append(", ").append(value ? 1 : 0).append("), ");
        }
        valuesBuilder.delete(valuesBuilder.length() - 2, valuesBuilder.length());

        String insert = "INSERT INTO " + NAME + " (mailId, recipient, " + column + ") VALUES " + valuesBuilder.toString() + " ";
        if (database.getDatabaseType() == DatabaseType.MYSQL) {
            database.execute(insert + " ON DUPLICATE KEY UPDATE " + column + " = " + (value ? 1 : 0));
        } else if (database.getDatabaseType() == DatabaseType.SQLITE) {
            database.execute(insert + " ON CONFLICT(mailId, recipient) DO UPDATE SET " + column + " = " + (value ? 1 : 0));
        }
    }

    /**
     * 指定されたWHERE句でカラムの値を取得する。
     * @param column カラム
     * @param where SQL文うち、WHERE句の部分
     * @return メールのIDと値のMap
     */
    private Map<Integer, Boolean> getBoolWhere(String column, String where) {
        Map<Integer, Boolean> result = new HashMap<>();
        return database.query("SELECT mailId, " + column + " FROM " + NAME + (where.isEmpty() ? "" : " ") + where, rs -> {
            try {
                while (rs.next()) {
                    result.put(rs.getInt("mailId"), rs.getByte(column) == (byte)1);
                }
                return result;
            } catch (SQLException e) {
                e.printStackTrace();
                return result;
            }
        });
    }
}
