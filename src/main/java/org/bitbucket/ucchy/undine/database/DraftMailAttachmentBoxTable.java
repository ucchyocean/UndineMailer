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

import org.bitbucket.ucchy.undine.database.Database.DatabaseType;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

/**
 * 編集中メールの添付アイテムボックスを保持するテーブルにアクセスするクラス。。
 * @author LazyGon
 */
public class DraftMailAttachmentBoxTable {
    public static final String NAME = "undine_draftmailattachmentbox";

    private final Database database;
    @SuppressWarnings("unused")
    private final DraftMailDataTable draftMailDataTable;

    DraftMailAttachmentBoxTable(Database database, DraftMailDataTable draftMailDataTable) {
        this.database = database;
        this.draftMailDataTable = draftMailDataTable;
        createTable();
    }

    void createTable() {
        database.execute(
            "CREATE TABLE IF NOT EXISTS " + NAME + " (" +
                "id INTEGER PRIMARY KEY " + database.getDatabaseType().autoIncrement + ", " +
                "mailId INTEGER NOT NULL, " +
                "item TEXT(8192) NOT NULL, " +
                "FOREIGN KEY (mailId) REFERENCES " + DraftMailDataTable.NAME + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
                (database.getDatabaseType() == DatabaseType.MYSQL ? ", INDEX attachmentmailid (id, mailId)" : "") +
            ")"
        );
        if (database.getDatabaseType() == DatabaseType.SQLITE) {
            database.execute("CREATE INDEX IF NOT EXISTS attachmentmailid ON " + NAME + "(id, mailId)");
        }
    }

    /**
     * 指定されたメールIDの添付アイテムのリストを取得する。
     * @param mailId メールID
     * @return 添付アイテムボックス
     */
    public ArrayList<ItemStack> getAttachmentBoxOf(int mailId) {
        return database.query("SELECT item FROM " + NAME + " WHERE mailId = " + mailId, rs -> {
            try {
                ArrayList<ItemStack> items = new ArrayList<>();
                YamlConfiguration itemSection = new YamlConfiguration();
                while (rs.next()) {
                    itemSection.loadFromString(rs.getString("item").replace("\\'", "'"));
                    items.add(ItemConfigParser.getItemFromSection(itemSection));
                }
                return items;
            } catch (SQLException | InvalidConfigurationException | ItemConfigParseException e) {
                e.printStackTrace();
                return new ArrayList<>();
            }
        });
    }

    /**
     * このメールIDの添付アイテムボックスを設定する。
     * @param mailId メールID
     * @param items 添付アイテムボックス
     */
    public void setAttachmentBox(int mailId, List<ItemStack> items) {
        database.execute("DELETE FROM " + NAME + " WHERE mailId = " + mailId);
        if (items.isEmpty()) {
            return;
        }

        StringBuilder valuesBuilder = new StringBuilder();        
        YamlConfiguration itemSection = new YamlConfiguration();
        for (ItemStack item : items) {
            ItemConfigParser.setItemToSection(itemSection, item);
            valuesBuilder.append("(").append(mailId).append(", '").append(itemSection.saveToString().replace("'", "\\'")).append("'), ");
        }
        valuesBuilder.delete(valuesBuilder.length() - 2, valuesBuilder.length());

        String insert = "INSERT INTO " + NAME + " (mailId, item) VALUES " + valuesBuilder.toString();
        if (database.getDatabaseType() == DatabaseType.MYSQL) {
            database.execute(insert + " ON DUPLICATE KEY UPDATE item = VALUES(item)");
        } else if (database.getDatabaseType() == DatabaseType.SQLITE) {
            database.execute(insert + " ON CONFLICT(id) DO UPDATE SET item = execluded.item");
        }
    }
}