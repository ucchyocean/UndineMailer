package org.bitbucket.ucchy.undine.database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.ucchyocean.itemconfig.ItemConfigParseException;
import com.github.ucchyocean.itemconfig.ItemConfigParser;

import org.bitbucket.ucchy.undine.database.Database.DatabaseType;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

public class MailAttachmentBoxTable {
    public static final String NAME = "undine_mailattachmentbox";

    private final Database database;
    @SuppressWarnings("unused")
    private final MailDataTable mailDataTable;

    MailAttachmentBoxTable(Database database, MailDataTable mailDataTable) {
        this.database = database;
        this.mailDataTable = mailDataTable;
        createTable();
    }

    void createTable() {
        database.execute(
            "CREATE TABLE IF NOT EXISTS " + NAME + " (" +
                "id INTEGER PRIMARY KEY " + database.getDatabaseType().autoIncrement + ", " +
                "mailId INTEGER NOT NULL, " +
                "item TEXT(8192) NOT NULL, " +
                "FOREIGN KEY (mailId) REFERENCES " + MailDataTable.NAME + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
                (database.getDatabaseType() == DatabaseType.MYSQL ? ", INDEX attachmentmailid (id, mailId)" : "") +
                
            ")"
        );
        if (database.getDatabaseType() == DatabaseType.SQLITE) {
            database.execute("CREATE INDEX IF NOT EXISTS attachmentmailid ON " + NAME + "(id, mailId)");
        }
    }

    public int getAttachBoxUsageCount(int senderId) {
        
        return database.query("SELECT mailId FROM " + NAME + " WHERE mailId " + Database.createIn(database.mailDataTable.getIdsBySenderId(senderId)), rs -> {
            try {
                Set<Integer> mailIdsWithAttachmentBox = new HashSet<>();
                while (rs.next()) {
                    mailIdsWithAttachmentBox.add(rs.getInt("mailId"));
                }
                return mailIdsWithAttachmentBox.size();
            } catch (SQLException e) {
                e.printStackTrace();
                return 0;
            }
        });
    }

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

    public void setAttachmentBox(int mailId, List<ItemStack> items) {
        database.execute("DELETE FROM " + NAME + " WHERE mailId = " + mailId);
        if (items.isEmpty()) {
            return;
        }

        StringBuilder valuesBuilder = new StringBuilder();        
        YamlConfiguration itemSection = new YamlConfiguration();
        for (ItemStack item : items) {
            ItemConfigParser.setItemToSection(itemSection, item);
            valuesBuilder.append("(").append(mailId).append(", '").append(itemSection.saveToString()).append("'), ");
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
