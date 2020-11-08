/*
 * @author     LazyGon
 * @license    LGPLv3
 * @copyright  Copyright OKOCRAFT 2020
 */
package org.bitbucket.ucchy.undine.database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.github.ucchyocean.itemconfig.ItemConfigParseException;
import com.github.ucchyocean.itemconfig.ItemConfigParser;

import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

/**
 * 編集中メールのメインテーブルにアクセスするクラス。
 * @author LazyGon
 */
public class DraftMailDataTable {

    public static final String NAME = "undine_draftmaildata";

    private final Database database;
    private final MailSenderTable mailSenderTable;

    DraftMailDataTable(Database database, MailSenderTable mailSenderTable) {
        this.database = database;
        this.mailSenderTable = mailSenderTable;
        createTable();
    }

    void createTable() {
        database.execute(
            "CREATE TABLE IF NOT EXISTS " + NAME + " (" +
                "id INTEGER PRIMARY KEY " + database.getDatabaseType().autoIncrement + ", " +
                "sender INTEGER NOT NULL, " +
                "message TEXT(4096), " +
                "costMoney DOUBLE NOT NULL DEFAULT 0, " +
                "costItem TEXT(8192), " +
                "UNIQUE (id, sender), " +
                "FOREIGN KEY (sender) REFERENCES " + MailSenderTable.NAME + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
            ")"
        );
    }

    /**
     * 新しい編集中メールを作成し、そのメールのIDを返す。
     * @param senderId 新しいメールの送信者
     * @return メールID
     */
    public int newMail(int senderId) {
        return database.insert("INSERT INTO " + NAME + " (sender) VALUES (" + senderId + ") ").get(0);
    }

    /**
     * 指定されたIDの送信者が送信したメールIDのリストを返す。
     * @param senderId 送信者
     * @return メールIDのリスト
     */
    public ArrayList<Integer> getIdsBySenderId(int senderId) {
        ArrayList<Integer> ids = new ArrayList<>();
        if (senderId == -1) {
            return ids;
        }
        database.query("SELECT id FROM " + NAME + " WHERE sender = " + senderId, rs -> {
            try {
                while (rs.next()) {
                    ids.add(rs.getInt("id"));
                }
                return null;
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        });

        return ids;
    }

    /**
     * 指定したメールIDのメールの送信者を設定する。
     * @param id メールのID
     * @param senderId 送信者
     */
    public void setSender(int id, int senderId) {
        database.execute("UPDATE " + NAME + " SET sender = " + senderId + " WHERE id = " + id);
    }

    /**
     * メールIDから送信者を取得する。
     * @param id メールのID
     * @return 送信者
     */
    public MailSender getSenderById(int id) {
        int senderId = database.query("SELECT sender FROM " + NAME + " WHERE id = " + id, rs -> {
            try {
                if (rs.next()) {
                    return rs.getInt("sender");
                }
                return -1;
            } catch (SQLException e) {
                e.printStackTrace();
                return -1;
            }
        });

        return mailSenderTable.getById(senderId);
    }

    /**
     * メールIDから本文を取得する。
     * @param id メールのID
     * @return 本文。改行を含む。
     */
    public String getMessage(int id) {
        return database.query("SELECT message FROM " + NAME + " WHERE id = " + id, rs -> {
            try {
                String message;
                if (rs.next() && (message = rs.getString("message")) != null) {
                    return message.replace("\\'", "'");
                }
                return null;
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    /**
     * 指定したIDのメールの本文を設定する。
     * @param id メールのID
     * @param message 本文
     */
    public void setMessage(int id, String message) {
        database.execute("UPDATE " + NAME + " SET message = " + (message == null ? "NULL" : "'" + message.replace("'", "\\'") + "'") + " WHERE id = " + id);
    }

    /**
     * メールのIDから添付アイテムボックスを開くのに必要な金額を取得する。
     * @param id メールのID
     * @return 添付アイテムボックスを開くのに必要な金額
     */
    public double getCostMoney(int id) {
        return database.query("SELECT costMoney FROM " + NAME + " WHERE id = " + id, rs -> {
            try {
                if (rs.next()) {
                    return rs.getDouble("costMoney");
                }
                return 0D;
            } catch (SQLException e) {
                e.printStackTrace();
                return 0D;
            }
        });
    }

    /**
     * 指定したIDのメールの添付アイテムボックスを開くのに必要な金額を設定する。
     * @param id メールのID
     * @param money 金額
     */
    public void setCostMoney(int id, double money) {
        database.execute("UPDATE " + NAME + " SET = " + (Math.floor(money * 10) / 10) + " WHERE id = " + id);
    }

    /**
     * 指定したIDのメールの添付アイテムボックスを開くのに必要なアイテムを取得する。
     * @param id メールのID
     * @return アイテム
     */
    public ItemStack getCostItem(int id) {
        return database.query("SELECT costItem FROM " + NAME + " WHERE id = " + id, rs -> {
            try {
                String itemStr;
                if (!rs.next() || (itemStr = rs.getString("costItem")) == null) {
                    return null;
                }
                itemStr = itemStr.replace("\\'", "'");
                YamlConfiguration itemSection = new YamlConfiguration();
                itemSection.loadFromString(itemStr);
                return ItemConfigParser.getItemFromSection(itemSection);
            } catch (SQLException | InvalidConfigurationException | ItemConfigParseException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    /**
     * 指定したIDのメールの添付アイテムボックスを開くのに必要なアイテムを設定する。
     * @param id メールのID
     * @param item アイテム
     * @return 設定されたかどうか。
     */
    public boolean setCostItem(int id, ItemStack item) {
        if (item == null) {
            return database.execute("UPDATE " + NAME + " SET costItem = NULL WHERE id = " + id);
        }
        YamlConfiguration itemSection = new YamlConfiguration();
        ItemConfigParser.setItemToSection(itemSection, item);
        String itemStr = itemSection.saveToString();
        return database.execute("UPDATE " + NAME + " SET costItem = '" + itemStr.replace("'", "\\'") + "' WHERE id = " + id);
    }

    /**
     * 指定したIDのメールを削除する。
     * @param id メールのID
     */
    public void removeMail(int id) {
        database.execute("DELETE FROM " + NAME + " WHERE id = " + id);
    }
    
    /**
     * 複数指定したIDのメールをすべて削除する。
     * @param ids IDのリスト
     */
    public void removeMails(List<Integer> ids) {
        if (!ids.isEmpty()) {
            database.execute("DELETE FROM " + NAME + " WHERE id " + Database.createIn(ids));
        }
    }
}
