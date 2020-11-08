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

/**
 * メールのデータを保持するメインテーブルにアクセスするクラス。
 * @author LazyGon
 */
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

    /**
     * 存在するメールのIDの中で最も大きいIDを取得する。事実上最新の送信されたメールのIDと等しい。
     * @return 最近送信されたメールのID
     */
    public int getLastInsertedId() {
        return database.query("SELECT MAX(id) AS maxId FROM " + NAME + "", rs -> {
            try {
                return rs.next() ? rs.getInt("maxId") : -1;
            } catch (SQLException e) {
                e.printStackTrace();
                return -1;
            }
        });
    }

    /**
     * 編集中メールの内容を実際に送信する。送信された後編集中メールは削除される。
     * @param senderId メールの送信者
     * @return 新たにつけられたメールID
     */
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

    /**
     * メールが送信されたことを送信先に通知する。
     * @param mailId メールID
     */
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

    /**
     * すべてのメールのIDのリストを取得する。
     * @return すべてのメールのIDのリスト
     */
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

    /**
     * 指定した日数より前に送信されたメールをすべて削除する。
     * @param daysAgo
     */
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

    /**
     * 指定された送信先が閲覧できるメールをすべて取得する。
     * @param recipientId 送信先
     * @return メールのIDのリスト
     */
    public ArrayList<Integer> getIdsByRecipient(int recipientId) {
        Set<Integer> result = new HashSet<>(database.mailRecipientsTable.getMailIdsByRecipient(recipientId));
        List<String> groupNames = database.groupMembersTable.getBelongingGroups(recipientId);
        result.addAll(database.mailRecipientGroupsTable.getMailIdsByGroups(groupNames));
        return new ArrayList<>(result);
    }

    /**
     * 指定した送信者の送信したメールをすべて取得する。
     * @param senderId 送信者
     * @return メールのIDのリスト
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
     * 指定したメールの送信者を設定する。
     * @param mailId メールのID
     * @param senderId 送信者のID
     */
    public void setSender(int mailId, int senderId) {
        database.execute("UPDATE " + NAME + " SET sender = " + senderId + " WHERE id = " + mailId);
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

    /**
     * 指定したメールの本文を取得する。
     * @param id メールのID
     * @return 本文
     */
    public String getMessage(int id) {
        return getString(id, "message");
    }

    /**
     * 指定したメールの本文を設定する。
     * @param id メールのID
     * @param message 本文
     */
    public void setMessage(int id, String message) {
        setString(id, "message", message);
    }

    /**
     * 添付アイテムボックスが拒否された理由を取得する。値がない場合は空文字列を返す。
     * @param id メールのID
     * @return 添付アイテムボックスが拒否された理由
     */
    public String getAttachmentsRefusedReason(int id) {
        String refusedReason = getString(id, "attachmentsRefusedReason");
        return refusedReason == null ? "" : refusedReason;
    }

    /**
     * 添付アイテムボックスが拒否された理由を設定する。
     * @param id メールのID
     * @param attachmentsRefusedReason 添付アイテムボックスが拒否された理由
     */
    public void setAttachmentsRefusedReason(int id, String attachmentsRefusedReason) {
        setString(id, "attachmentsRefusedReason", attachmentsRefusedReason == null ? "" : attachmentsRefusedReason);
    }

    /**
     * 指定したカラムから文字列を取得する。
     * @param id メールのID
     * @param column 取得するカラムの名前
     * @return カラムに収められていた文字列データ
     */
    public String getString(int id, String column) {
        return database.query("SELECT " + column + " FROM " + NAME + " WHERE id = " + id, rs -> {
            try {
                String value;
                if (rs.next() && (value = rs.getString(column)) != null) {
                    return value.replace("\\'", "'");
                }
                return null;
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    /**
     * 指定したカラムの文字列データを設定する。
     * @param id メールのID
     * @param column カラム
     * @param value 値
     */
    public void setString(int id, String column, String value) {
        database.execute("UPDATE " + NAME + " SET " + column + " = " + (value == null ? "NULL" : "'" + value.replace("'", "\\'") + "'") + " WHERE id = " + id);
    }

    /**
     * 指定されたメールの添付アイテムボックスを開くのに必要な金額を取得する。
     * @param id メールのID
     * @return 金額
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
     * 指定されたメールの添付アイテムボックスを開くのに必要な金額を設定する。
     * @param id メールのID
     * @param money 金額
     */
    public void setCostMoney(int id, double money) {
        database.execute("UPDATE " + NAME + " SET costMoney = " + (Math.floor(money * 10) / 10) + " WHERE id = " + id);
    }

    /**
     * 指定されたメールの添付アイテムボックスを開くのに必要なアイテムを取得する。
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
     * 指定されたメールの添付アイテムボックスを開くのに必要なアイテムを設定する。
     * @param id メールのID
     * @param item アイテム
     * @return 設定されたか
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
     * 指定したIDのメールがスパムかどうか取得する。TODO: 実装
     * @deprecated まだスパム機能は実装されておらず、このメソッドはどこにも使われていない。
     * @param id メールのID
     * @return 指定したIDのメールがスパムかどうか
     */
    @Deprecated
    public boolean isBulk(int id) {
        return getBool(id, "isBulk");
    }

    /**
     * 指定したIDのメールがスパムかどうかを設定する。
     * @deprecated まだスパム機能は実装されておらず、このメソッドはどこにも使われていない。
     * @param id メールのID
     * @param isBulk 指定したIDのメールがスパムかどうか
     */
    public void setBulk(int id, boolean isBulk) {
        setBool(id, "isBulk", isBulk);
    }

    /**
     * 指定したIDのメールの添付アイテムボックスが開かれたかどうかを取得する。
     * @param id メールのID
     * @return 添付アイテムボックスが開かれたかどうか
     */
    public boolean isAttachmentOpened(int id) {
        return getBool(id, "isAttachmentsOpened");
    }

    /**
     * 指定したIDのメールの添付アイテムボックスが開かれたかどうかを設定する。
     * @param id メールのID
     * @param isAttachmentOpened 添付アイテムボックスが開かれたかどうか
     */
    public void setAttachmentOpened(int id, boolean isAttachmentOpened) {
        setBool(id, "isAttachmentsOpened", isAttachmentOpened);
    }

    /**
     * 指定したIDのメールの添付アイテムボックスがキャンセルされたかどうかを取得する。
     * @param id メールのID
     * @return 添付アイテムボックスがキャンセルされたかどうか
     */
    public boolean isAttachmentCancelled(int id) {
        return getBool(id, "isAttachmentsCancelled");
    }

    /**
     * 指定したIDのメールの添付アイテムボックスがキャンセルされたかどうかを設定する。
     * @param id メールのID
     * @param isAttachmentCancelled 添付アイテムボックスがキャンセルされたか
     */
    public void setAttachmentCancelled(int id, boolean isAttachmentCancelled) {
        setBool(id, "isAttachmentCancelled", isAttachmentCancelled);
    }

    /**
     * 指定したIDのメールの添付アイテムボックスが拒否されたかどうかを取得する。
     * @param id メールのID
     * @return 添付アイテムボックスが拒否されたかどうか
     */
    public boolean isAttachmentRefused(int id) {
        return getBool(id, "isAttachmentsRefused");
    }

    /**
     * 指定したIDのメールの添付アイテムボックスが拒否されたかどうかを設定する。
     * @param id メールのID
     * @param isAttachmentRefused 添付アイテムボックスが拒否されたかどうか
     */
    public void setAttachmentRefused(int id, boolean isAttachmentRefused) {
        setBool(id, "isAttachmentsRefused", isAttachmentRefused);
    }

    /**
     * 指定したメールの指定したカラムの真偽値データを取得する。実際のデータベースにはByte値が格納されている。
     * @param id メールのID
     * @param column カラムの名前
     * @return 真偽値データ
     */
    private boolean getBool(int id, String column) {
        return database.query("SELECT " + column + " FROM " + NAME + " WHERE id = " + id, rs -> {
            try {
                if (rs.next()) {
                    return rs.getByte(column) == (byte)1;
                }
                return false;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * 指定したメールの指定したカラムの真偽値データを設定する。実際のデータベースにはByte値が格納されている。
     * @param id メールのID
     * @param column カラムの名前
     * @param value 値
     */
    private void setBool(int id, String column, boolean value) {
        database.execute("UPDATE " + NAME + " SET " + column + " = " + (value ? 1 : 0) + " WHERE id = " + id);
    }

    /**
     * 指定したIDのメールが送信された日時を取得する。
     * @param id メールのID
     * @return 日時
     */
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

    /**
     * 指定したIDのメールが送信された日時を設定する。
     * @param id メールのID
     * @param date 日時
     */
    public void setDate(int id, Date date) {
        database.execute("UPDATE " + NAME + " SET dateAndTime = " + date.getTime() + " WHERE id = " + id);
    }

    /**
     * 指定したIDのメールの送信された場所を取得する。コンソールが送信したメールの場合はnullを返す。
     * @param id メールのID
     * @return 送信場所
     */
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

    /**
     * 指定されたIDのメールが送信された場所を設定する。
     * @param id メールのID
     * @param location 送信場所
     */
    public void setLocation(int id, Location location) {
        if (location == null) {
            database.execute("UPDATE " + NAME + " SET location = NULL WHERE id = " + id);
        } else {
            database.execute("UPDATE " + NAME + " SET location = '" + Database.createDBLocationString(location) + "' WHERE id = " + id);
        }
    }

    /**
     * 指定されたIDのメールがデータベースに存在するかを調べる。
     * @param id メールのID
     * @return 存在する場合はtrue
     */
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

    /**
     * 指定されたメールIDのリストから、存在しないメールをすべて削除する。
     * @param ids メールIDのリスト
     */
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
