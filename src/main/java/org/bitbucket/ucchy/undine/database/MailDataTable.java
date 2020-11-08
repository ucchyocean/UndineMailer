package org.bitbucket.ucchy.undine.database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.ucchyocean.itemconfig.ItemConfigParseException;
import com.github.ucchyocean.itemconfig.ItemConfigParser;

import org.bitbucket.ucchy.undine.MailData;
import org.bitbucket.ucchy.undine.MailDataFlatFile;
import org.bitbucket.ucchy.undine.MailManager;
import org.bitbucket.ucchy.undine.Messages;
import org.bitbucket.ucchy.undine.Utility;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class MailDataTable {
    
    public static final String NAME = "undine_maildata";

    private final Database database;
    private final MailSenderTable mailSenderTable;

    private int checkedLatestId = 0;

    MailDataTable(Database database, MailSenderTable mailSenderTable) {
        this.database = database;
        this.mailSenderTable = mailSenderTable;
        createTable();

        // 他のサーバーから送信されたメールを確認して、存在する場合はチャットで通知する
        checkedLatestId = getLastInsertedId();
        new BukkitRunnable(){
            @Override
            public void run() {
                int currentId = getLastInsertedId();
                while (checkedLatestId < currentId) {
                    checkedLatestId++;
                    notifyMail(checkedLatestId);
                }
            };
        }.runTaskTimer(database.parent, 100L, 200L);
    }

    void createTable() {
        database.execute(
            "CREATE TABLE IF NOT EXISTS " + NAME + " (" +
                "id INTEGER PRIMARY KEY " + database.getDatabaseType().autoIncrement + ", " +
                "sender INTEGER NOT NULL, " +
                "message TEXT(4096), " +
                "costMoney DOUBLE NOT NULL DEFAULT 0, " +
                "costItem TEXT(8192), " +
                "isBulk TINYINT NOT NULL DEFAULT 0, " +
                "isAttachmentsOpened TINYINT NOT NULL DEFAULT 0, " +
                "isAttachmentsCancelled TINYINT NOT NULL DEFAULT 0, " +
                "isAttachmentsRefused TINYINT NOT NULL DEFAULT 0, " +
                "attachmentsRefusedReason TEXT(512), " +
                "dateAndTime BIGINT NOT NULL, " +
                "location VARCHAR(128), " +
                "UNIQUE (id, sender), " +
                "FOREIGN KEY (sender) REFERENCES " + MailSenderTable.NAME + "(id) ON DELETE CASCADE ON UPDATE CASCADE" +
            ")"
        );
    }

    public int getLastInsertedId() {
        return database.query("SELECT MAX(id) AS maxId FROM " + NAME + "", rs -> {
            try {
                int a = rs.next() ? rs.getInt("maxId") : -1;
                System.out.println("getLastInsertedId: " + a);
                return a;
            } catch (SQLException e) {
                e.printStackTrace();
                return -1;
            }
        });
    }

    public int newMail(int editId) {
        List<Integer> generatedKeys = database.insert("INSERT INTO " + NAME + " (sender, message, costMoney, costItem, dateAndTime) " +
            "SELECT sender, message, costMoney, costItem, " + System.currentTimeMillis() + " FROM " + DraftMailDataTable.NAME + " WHERE id = " + editId);
        int mailId = generatedKeys.isEmpty() ? -1 : generatedKeys.get(0);
        if (mailId == -1) {
            return -1;
        }   
        database.execute("INSERT INTO " + MailAttachmentBoxTable.NAME + " (mailId, item) SELECT " + mailId + ", item FROM " + DraftMailAttachmentBoxTable.NAME + " WHERE mailId = " + editId);
        database.execute("INSERT INTO " + MailRecipientsTable.NAME + " (mailId, recipient) SELECT " + mailId + ", recipient FROM " + DraftMailRecipientsTable.NAME + " WHERE mailId = " + editId);
        database.execute("INSERT INTO " + MailRecipientGroupsTable.NAME + " (mailId, recipientGroup) SELECT " + mailId + ", recipientGroup FROM " + DraftMailRecipientGroupsTable.NAME + " WHERE mailId = " + editId);

        database.draftMailDataTable.removeMail(editId);

        // 他のサーバーから送信されたメールを確認して、存在する場合は現在のIDを更新する前にチャットで通知する
        for (int mailIdFromOtherServer = checkedLatestId + 1; mailId > mailIdFromOtherServer; mailIdFromOtherServer++) {
            notifyMail(mailIdFromOtherServer);
        }
        checkedLatestId = mailId;
        
        return mailId;
    }

    private void notifyMail(int mailId) {
        MailManager manager = database.parent.getMailManager();
        MailData mail = manager.getMail(mailId);

        // 宛先の人がログイン中なら知らせる
        String msg = Messages.get("InformationYouGotMail", "%from", mail.getFrom().getName());

        if (mail.isAllMail()) {
            for (Player player : Utility.getOnlinePlayers()) {
                player.sendMessage(msg);
                String pre = Messages.get("ListVerticalParts");
                manager.sendMailLine(MailSender.getMailSender(player), pre, ChatColor.GOLD + mail.getInboxSummary(), mail);
            }
        } else {
            for (MailSender to : mail.getToTotal()) {
                if (to.isOnline()) {
                    to.sendMessage(msg);
                    String pre = Messages.get("ListVerticalParts");
                    manager.sendMailLine(to, pre, ChatColor.GOLD + mail.getInboxSummary(), mail);
                }
            }
        }
    }

    public void removeMail(int id) {
        database.execute("DELETE FROM " + NAME + " WHERE id = " + id);
    }
    
    public void removeMail(List<Integer> ids) {
        if (!ids.isEmpty()) {
            database.execute("DELETE FROM " + NAME + " WHERE id " + Database.createIn(ids));
        }
    }

    public ArrayList<Integer> getIds() {
        return database.query("SELECT id FROM " + NAME, rs -> {
            try {
                ArrayList<Integer> ids = new ArrayList<>();
                while (rs.next()) {
                    ids.add(rs.getInt("id"));
                }
                return ids;
            } catch (SQLException e) {
                e.printStackTrace();
                return new ArrayList<>();
            }
        });
    }

    public void removeMailsOlderThan(int daysAgo) {
        database.execute("DELETE FROM " + NAME + " WHERE dateAndTime < " + (System.currentTimeMillis() - daysAgo * 24L * 60L * 60L * 1000L));
    }

    public ArrayList<Integer> getIdsOlderThan(int daysAgo) {
        return database.query("SELECT id FROM " + NAME + " WHERE dateAndTime < " + (System.currentTimeMillis() - daysAgo * 24L * 60L * 60L * 1000L), rs -> {
            try {
                ArrayList<Integer> ids = new ArrayList<>();
                while (rs.next()) {
                    ids.add(rs.getInt("id"));
                }
                return ids;
            } catch (SQLException e) {
                e.printStackTrace();
                return new ArrayList<>();
            }
        });
    }

    public ArrayList<Integer> getIdsByRecipient(int recipientId) {
        Set<Integer> result = new HashSet<>(database.mailRecipientsTable.getMailIdsByRecipient(recipientId));
        List<String> groupNames = database.groupMembersTable.getBelongingGroups(recipientId);
        result.addAll(database.mailRecipientGroupsTable.getMailIdsByGroups(groupNames));
        return new ArrayList<>(result);
    }

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

    public void setSender(int senderId) {
        database.execute("UPDATE " + NAME + " SET sender = " + senderId);
    }

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

    public String getMessage(int id) {
        return getString(id, "message");
    }

    public void setMessage(int id, String message) {
        setString(id, "message", message);
    }

    public String getAttachmentsRefusedReason(int id) {
        String refusedReason = getString(id, "attachmentsRefusedReason");
        return refusedReason == null ? "" : refusedReason;
    }

    public void setAttachmentsRefusedReason(int id, String attachmentsRefusedReason) {
        setString(id, "attachmentsRefusedReason", attachmentsRefusedReason == null ? "" : attachmentsRefusedReason);
    }

    public String getString(int id, String prop) {
        return database.query("SELECT " + prop + " FROM " + NAME + " WHERE id = " + id, rs -> {
            try {
                String value;
                if (rs.next() && (value = rs.getString(prop)) != null) {
                    return value.replace("\\'", "'");
                }
                return null;
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    public void setString(int id, String prop, String value) {
        database.execute("UPDATE " + NAME + " SET " + prop + " = " + (value == null ? "NULL" : "'" + value.replace("'", "\\'") + "'") + " WHERE id = " + id);
    }

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

    public void setCostMoney(int id, double money) {
        database.execute("UPDATE " + NAME + " SET costMoney = " + (Math.floor(money * 10) / 10) + " WHERE id = " + id);
    }

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

    public boolean setCostItem(int id, ItemStack item) {
        if (item == null) {
            return database.execute("UPDATE " + NAME + " SET costItem = NULL WHERE id = " + id);
        }
        YamlConfiguration itemSection = new YamlConfiguration();
        ItemConfigParser.setItemToSection(itemSection, item);
        String itemStr = itemSection.saveToString();
        return database.execute("UPDATE " + NAME + " SET costItem = '" + itemStr.replace("'", "\\'") + "' WHERE id = " + id);
    }

    public boolean isBulk(int id) {
        return getBool(id, "isBulk");
    }

    public void setBulk(int id, boolean isBulk) {
        setBool(id, "isBulk", isBulk);
    }

    public boolean isAttachmentOpened(int id) {
        return getBool(id, "isAttachmentsOpened");
    }

    public void setAttachmentOpened(int id, boolean isAttachmentOpened) {
        setBool(id, "isAttachmentsOpened", isAttachmentOpened);
    }

    public boolean isAttachmentCancelled(int id) {
        return getBool(id, "isAttachmentsCancelled");
    }

    public void setAttachmentCancelled(int id, boolean isAttachmentCancelled) {
        setBool(id, "isAttachmentCancelled", isAttachmentCancelled);
    }

    public boolean isAttachmentRefused(int id) {
        return getBool(id, "isAttachmentsRefused");
    }

    public void setAttachmentRefused(int id, boolean isAttachmentRefused) {
        setBool(id, "isAttachmentsRefused", isAttachmentRefused);
    }

    private boolean getBool(int id, String prop) {
        return database.query("SELECT " + prop + " FROM " + NAME + " WHERE id = " + id, rs -> {
            try {
                if (rs.next()) {
                    return rs.getByte(prop) == (byte)1;
                }
                return false;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    private void setBool(int id, String prop, boolean value) {
        database.execute("UPDATE " + NAME + " SET " + prop + " = " + (value ? 1 : 0) + " WHERE id = " + id);
    }

    public Date getDate(int id) {
        long unix = database.query("SELECT dateAndTime FROM " + NAME + " WHERE id = " + id, rs -> {
            try {
                if (rs.next()) {
                    return rs.getLong("dateAndTime");
                }
                return 0L;
            } catch (SQLException e) {
                e.printStackTrace();
                return 0L;
            }
        });

        return new Date(unix);
    }

    public void setDate(int id, Date date) {
        database.execute("UPDATE " + NAME + " SET dateAndTime = " + date.getTime() + " WHERE id = " + id);
    }

    public Location getLocation(int id) {
        String loc = database.query("SELECT location FROM " + NAME + " WHERE id = " + id, rs -> {
            try {
                if (rs.next()) {
                    return rs.getString("location");
                }
                return null;
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        });

        return loc == null || loc.isBlank() ? null : Database.fromDBLocationString(loc);
    }

    public void setLocation(int id, Location location) {
        if (location == null) {
            database.execute("UPDATE " + NAME + " SET location = NULL WHERE id = " + id);
        } else {
            database.execute("UPDATE " + NAME + " SET location = '" + Database.createDBLocationString(location) + "' WHERE id = " + id);
        }
    }

    public boolean exists(int id) {
        return database.query("SELECT id FROM " + NAME + " WHERE id = " + id, rs -> {
            try {
                return rs.next();
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    public void retainExists(List<Integer> ids) {
        if (ids.isEmpty()) {
            return;
        }
        database.query("SELECT id FROM " + NAME + " WHERE id " + Database.createIn(ids), rs -> {
            try {
                List<Integer> found = new ArrayList<>();
                while (rs.next()) {
                    found.add(rs.getInt("id"));
                }
                ids.retainAll(found);
                return null;
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    /**
     * 指定したidを持つメールをデータベースから取得し、フラットファイル管理のクラスに変換して返す。
     * @param ids 取得するメールのidのリスト
     * @return メールデータ
     */
    public List<MailDataFlatFile> getMailDataFlatFileById(List<Integer> ids) {
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        List<Integer> mailIds = new ArrayList<>(ids);
        retainExists(mailIds);

        String inIds = Database.createIn(mailIds);

        Map<Integer, MailDataFlatFile> mailData = new HashMap<>();
        Map<Integer, Integer> sender = new HashMap<>();
        Map<Integer, List<Integer>> to = new HashMap<>();
        Map<Integer, List<Integer>> whoRead = new HashMap<>();
        Map<Integer, List<Integer>> whoTrash = new HashMap<>();
        for (int id : mailIds) {
            to.put(id, new ArrayList<>());
            whoRead.put(id, new ArrayList<>());
            whoTrash.put(id, new ArrayList<>());
        }

        database.query(
            "SELECT " +
                "id, " +
                "sender, " +
                "message, " +
                "costMoney, " +
                "costItem, " +
                "isAttachmentsOpened, " +
                "isAttachmentsCancelled, " +
                "isAttachmentsRefused, " +
                "attachmentsRefusedReason, " +
                "dateAndTime, " +
                "location " +
            "FROM " + NAME + " WHERE id " + inIds,
        rs -> {
            try {
                while (rs.next()) {
                    int mailId = rs.getInt("id");
                    MailDataFlatFile data = new MailDataFlatFile();
                    mailData.put(mailId, data);
                    data.setIndex(mailId);
                    data.addMessage(rs.getString("message"));
                    data.setCostMoney(rs.getDouble("costMoney"));
                    if (rs.getByte("isAttachmentsOpened") == (byte)1) {
                        data.setOpenAttachments();
                    }
                    if (rs.getByte("isAttachmentsCancelled") == (byte)1) {
                        data.cancelAttachments();
                    }
                    if (rs.getByte("isAttachmentsRefused") == (byte)1) {
                        String refuseReason = rs.getString("attachmentsRefusedReason");
                        data.refuseAttachments(refuseReason == null ? "" : refuseReason);
                    }
                    data.setLocation(Database.fromDBLocationString(rs.getString("location")));
                    data.setDate(new Date(rs.getLong("dateAndTime")));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        });

        database.query("SELECT mailId, item FROM " + MailAttachmentBoxTable.NAME + " WHERE mailId " + inIds, rs -> {
            try {
                YamlConfiguration itemSection = new YamlConfiguration();
                while (rs.next()) {
                    itemSection.loadFromString(rs.getString("item").replace("\\'", "'"));
                    mailData.get(rs.getInt("mailId")).getAttachments().add(ItemConfigParser.getItemFromSection(itemSection));
                }
            } catch (SQLException | InvalidConfigurationException | ItemConfigParseException e) {
                e.printStackTrace();
            }
            return null;
        });

        database.query("SELECT mailId, item FROM " + MailAttachmentBoxSnapshotTable.NAME + " WHERE mailId " + inIds, rs -> {
            try {
                YamlConfiguration itemSection = new YamlConfiguration();
                while (rs.next()) {
                    itemSection.loadFromString(rs.getString("item").replace("\\'", "'"));
                    mailData.get(rs.getInt("mailId")).getAttachmentsOriginal().add(ItemConfigParser.getItemFromSection(itemSection));
                }
            } catch (SQLException | InvalidConfigurationException | ItemConfigParseException e) {
                e.printStackTrace();
            }
            return null;
        });

        database.query("SELECT mailId, recipientGroup FROM " + MailRecipientGroupsTable.NAME + " WHERE mailId " + inIds, rs -> {
            try {
                while (rs.next()) {
                    mailData.get(rs.getInt("mailId")).addToGroup(rs.getString("recipientGroup"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        });

        database.query("SELECT mailId, recipient, isRead, isTrash FROM " + MailRecipientsTable.NAME + " WHERE mailId " + inIds, rs -> {
            try {
                while (rs.next()) {
                    int mailId = rs.getInt("mailId");
                    int recipientId = rs.getInt("recipient");
                    to.get(mailId).add(recipientId);
                    if (rs.getByte("isRead") == (byte)1) {
                        whoRead.get(mailId).add(recipientId);
                    }
                    if (rs.getByte("isTrash") == (byte)1) {
                        whoTrash.get(mailId).add(recipientId);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        });

        Set<Integer> mailSenderIds = to.values().stream().flatMap(List::stream).collect(Collectors.toSet());
        mailSenderIds.addAll(sender.values());
        Map<Integer, MailSender> converted = mailSenderTable.getByIds(mailSenderIds);

        mailData.forEach((id, data) -> {
            data.setFrom(converted.get(sender.get(id)));
            for (int recipientId : to.get(id)) {
                data.addTo(converted.get(recipientId));
            }
            for (int idWhoRead : whoRead.get(id)) {
                data.setReadFlag(converted.get(idWhoRead));
            }
            for (int idWhoTrash : whoTrash.get(id)) {
                data.setTrashFlag(converted.get(idWhoTrash));
            }
        });

        return new ArrayList<>(mailData.values());
    }
}
