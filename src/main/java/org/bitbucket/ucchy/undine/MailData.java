/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bitbucket.ucchy.undine.group.GroupData;
import org.bitbucket.ucchy.undine.group.GroupManager;
import org.bitbucket.ucchy.undine.group.SpecialGroupAll;
import org.bitbucket.ucchy.undine.item.ItemConfigParseException;
import org.bitbucket.ucchy.undine.item.ItemConfigParser;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bitbucket.ucchy.undine.sender.MailSenderPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

/**
 * メールのデータ
 * @author ucchy
 */
public class MailData implements Comparable<MailData>, Cloneable {

    public static final int MESSAGE_MAX_SIZE = 15;
    private static final int SUMMARY_MAX_SIZE = 45;

    // 編集中に設定される属性
    private List<MailSender> to;
    private List<String> toGroups;
    private MailSender from;
    private List<String> message;
    private List<ItemStack> attachments;
    private double costMoney;
    private ItemStack costItem;

    // 送信後に設定される属性
    private int index;
    private List<MailSender> toTotal;
    private List<MailSender> readFlags;
    private List<MailSender> trashFlags;
    private List<ItemStack> attachmentsOriginal;
    private boolean isAttachmentsOpened;
    private boolean isAttachmentsCancelled;
    private boolean isAttachmentsRefused;
    private String attachmentsRefusedReason;
    private Date date;
    private Location location;

    /**
     * コンストラクタ
     */
    public MailData() {
        this(new ArrayList<MailSender>(), null, new ArrayList<String>());
    }

    /**
     * コンストラクタ
     * @param to 宛先
     * @param from 送り主
     * @param message メッセージ
     */
    public MailData(List<MailSender> to, MailSender from, String message) {
        this(to, from, new ArrayList<String>());
        this.message.add(message);
    }

    /**
     * コンストラクタ
     * @param to 宛先
     * @param from 送り主
     * @param message メッセージ
     */
    public MailData(List<MailSender> to, MailSender from, List<String> message) {
        this(to, from, message, new ArrayList<ItemStack>());
    }

    /**
     * コンストラクタ
     * @param to 宛先
     * @param from 送り主
     * @param message メッセージ
     * @param attachments 添付アイテム
     */
    public MailData(List<MailSender> to, MailSender from, List<String> message,
            List<ItemStack> attachments) {
        this(to, from, message, attachments, 0, null);
    }

    /**
     * コンストラクタ
     * @param to 宛先
     * @param from 送り主
     * @param message メッセージ
     * @param attachments 添付アイテム
     * @param costMoney 受け取りにかかる金額
     * @param costItem 受け取りにかかる取引アイテム
     */
    public MailData(List<MailSender> to, MailSender from, List<String> message,
            List<ItemStack> attachments, double costMoney, ItemStack costItem) {
        this(to, from, message, attachments, costMoney, costItem, new ArrayList<String>());
    }

    /**
     * コンストラクタ
     * @param to 宛先
     * @param from 送り主
     * @param message メッセージ
     * @param attachments 添付アイテム
     * @param costMoney 受け取りにかかる金額
     * @param costItem 受け取りにかかる取引アイテム
     * @param toGroup 宛先グループ
     */
    public MailData(List<MailSender> to, MailSender from, List<String> message,
            List<ItemStack> attachments, double costMoney, ItemStack costItem,
            List<String> toGroup) {
        this.index = 0;
        this.to = to;
        this.toGroups = toGroup;
        this.from = from;
        this.message = message;
        this.attachments = attachments;
        this.costMoney = costMoney;
        this.costItem = costItem;
        this.readFlags = new ArrayList<MailSender>();
        this.trashFlags = new ArrayList<MailSender>();
        this.isAttachmentsOpened = false;
        this.isAttachmentsCancelled = false;
        this.isAttachmentsRefused = false;
    }

    /**
     * 指定されたファイルへ保存する
     * @param file 保存先
     */
    protected void save(File file) {

        YamlConfiguration config = new YamlConfiguration();
        saveToConfigSection(config);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 指定されたコンフィグセクションへ保存する
     * @param section コンフィグセクション
     */
    protected void saveToConfigSection(ConfigurationSection section) {

        ArrayList<String> toList = new ArrayList<String>();
        for ( MailSender t : to ) {
            toList.add(t.toString());
        }
        section.set("to", toList);

        section.set("toGroups", toGroups);

        if ( toTotal != null ) {
            ArrayList<String> toTotalList = new ArrayList<String>();
            for ( MailSender t : toTotal ) {
                toTotalList.add(t.toString());
            }
            section.set("toTotal", toTotalList);
        }

        section.set("from", from.toString());
        section.set("message", message);

        if ( attachments != null ) {
            ConfigurationSection sub = section.createSection("attachments");
            int i = 1;
            for ( ItemStack item : attachments ) {
                ConfigurationSection subsub = sub.createSection("attachment" + i++);
                ItemConfigParser.setItemToSection(subsub, item);
            }
        }

        section.set("costMoney", costMoney);

        if ( costItem != null ) {
            ConfigurationSection sub = section.createSection("costItem");
            ItemConfigParser.setItemToSection(sub, costItem);
        }

        section.set("index", index);

        ArrayList<String> flagList = new ArrayList<String>();
        for ( MailSender t : readFlags ) {
            flagList.add(t.toString());
        }
        section.set("readFlags", flagList);

        ArrayList<String> trashList = new ArrayList<String>();
        for ( MailSender t : trashFlags ) {
            trashList.add(t.toString());
        }
        section.set("trashFlags", trashList);

        if ( attachmentsOriginal != null ) {
            ConfigurationSection sub = section.createSection("attachmentsOriginal");
            int i = 1;
            for ( ItemStack item : attachmentsOriginal ) {
                ConfigurationSection subsub = sub.createSection("attachment" + i++);
                ItemConfigParser.setItemToSection(subsub, item);
            }
        }

        if ( date != null ) {
            section.set("date", date.getTime());
        }

        if ( location != null ) {
            ConfigurationSection locSec = section.createSection("location");
            locSec.set("world", location.getWorld().getName());
            locSec.set("x", location.getX());
            locSec.set("y", location.getY());
            locSec.set("z", location.getZ());
            locSec.set("yaw", location.getYaw());
            locSec.set("pitch", location.getPitch());
        }

        section.set("isAttachmentsCancelled", isAttachmentsCancelled);
        section.set("isAttachmentsRefused", isAttachmentsRefused);
        section.set("attachmentsRefusedReason", attachmentsRefusedReason);
    }

    /**
     * 指定されたファイルからロードする
     * @param file ファイル
     * @return ロードされたMailData
     */
    protected static MailData load(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        return loadFromConfigSection(config);
    }

    /**
     * 指定されたコンフィグセクションからロードする
     * @param section コンフィグセクション
     * @return ロードされたMailData
     */
    protected static MailData loadFromConfigSection(ConfigurationSection section) {

        MailData data = new MailData();

        data.to = new ArrayList<MailSender>();
        for ( String t : section.getStringList("to") ) {
            MailSender sender = MailSender.getMailSenderFromString(t);
            if ( sender != null ) {
                data.to.add(sender);
            }
        }

        data.toGroups = section.getStringList("toGroups");

        if ( section.contains("toTotal") ) {
            data.toTotal = new ArrayList<MailSender>();
            for ( String t : section.getStringList("toTotal") ) {
                MailSender sender = MailSender.getMailSenderFromString(t);
                if ( sender != null ) {
                    data.toTotal.add(sender);
                }
            }
        }

        data.from = MailSender.getMailSenderFromString(section.getString("from"));
        data.message = section.getStringList("message");

        if ( section.contains("attachments") ) {
            data.attachments = new ArrayList<ItemStack>();

            for ( String name : section.getConfigurationSection("attachments").getKeys(false) ) {
                ConfigurationSection sub = section.getConfigurationSection("attachments." + name);
                try {
                    ItemStack item = ItemConfigParser.getItemFromSection(sub);
                    data.attachments.add(item);
                } catch (ItemConfigParseException e) {
                    e.printStackTrace();
                }
            }
        }

        data.costMoney = section.getInt("costMoney", 0);
        if ( section.contains("costItem") ) {
            try {
                data.costItem = ItemConfigParser.getItemFromSection(
                        section.getConfigurationSection("costItem"));
            } catch (ItemConfigParseException e) {
                e.printStackTrace();
            }
        }

        data.index = section.getInt("index");

        data.readFlags = new ArrayList<MailSender>();
        for ( String t : section.getStringList("readFlags") ) {
            MailSender sender = MailSender.getMailSenderFromString(t);
            if ( sender != null ) {
                data.readFlags.add(sender);
            }
        }

        data.trashFlags = new ArrayList<MailSender>();
        for ( String t : section.getStringList("trashFlags") ) {
            MailSender sender = MailSender.getMailSenderFromString(t);
            if ( sender != null ) {
                data.trashFlags.add(sender);
            }
        }

        if ( section.contains("attachmentsOriginal") ) {
            data.attachmentsOriginal = new ArrayList<ItemStack>();

            for ( String name :
                    section.getConfigurationSection("attachmentsOriginal").getKeys(false) ) {
                ConfigurationSection sub =
                        section.getConfigurationSection("attachmentsOriginal." + name);
                try {
                    ItemStack item = ItemConfigParser.getItemFromSection(sub);
                    data.attachmentsOriginal.add(item);
                } catch (ItemConfigParseException e) {
                    e.printStackTrace();
                }
            }
        }

        if ( section.contains("date") ) {
            data.date = new Date(section.getLong("date"));
        }

        if ( section.contains("location") && section.contains("location.world") ) {
            ConfigurationSection locSec = section.getConfigurationSection("location");
            World world = Bukkit.getWorld(locSec.getString("world"));
            if ( world != null ) {
                double x = locSec.getDouble("x");
                double y = locSec.getDouble("y");
                double z = locSec.getDouble("z");
                double yaw = locSec.getDouble("yaw");
                double pitch = locSec.getDouble("pitch");
                data.location = new Location(world, x, y, z, (float)yaw, (float)pitch);
            }
        }

        data.isAttachmentsCancelled = section.getBoolean("isAttachmentsCancelled", false);
        data.isAttachmentsRefused = section.getBoolean("isAttachmentsRefused", false);
        data.attachmentsRefusedReason = section.getString("attachmentsRefusedReason");

        return data;
    }

    /**
     * このオブジェクトの複製を作成して返す。
     * @see java.lang.Object#clone()
     */
    public MailData clone() {
        return new MailData(
                new ArrayList<MailSender>(to), from, message,
                new ArrayList<ItemStack>(attachments), costMoney, costItem,
                toGroups);
    }

    /**
     * 設定されている宛先を全て消去する
     */
    public void deleteAllTo() {
        to.clear();
        toGroups.clear();
    }

    /**
     * このメールのインデクス番号を取得します。
     * @return インデクス番号
     */
    public int getIndex() {
        return index;
    }

    /**
     * このメールのインデクス番号を設定します（メール送信時に自動で割り当てられます）。
     * @param index インデクス番号
     */
    protected void setIndex(int index) {
        this.index = index;
    }

    /**
     * このメールの宛先を取得します。
     * @return 宛先
     */
    public List<MailSender> getTo() {
        return to;
    }

    /**
     * このメールの宛先を設定します。
     * @param line 宛先番号（0から始まることに注意）
     * @param to 宛先
     */
    public void setTo(int line, MailSender to) {
        if ( this.to.size() <= line ) {
            this.to.add(to);
        } else if ( this.to.size() > line ) {
            this.to.set(line, to);
        }

        // 全体メールだった場合は、全体グループを除去しておく。
        if ( isAllMail() ) {
            toGroups.remove(SpecialGroupAll.NAME);
        }
    }

    /**
     * このメールの宛先を追加します。
     * @param to 宛先
     */
    public void addTo(MailSender to) {
        setTo(Integer.MAX_VALUE, to);
    }

    /**
     * このメールの指定された宛先を削除します。
     * @param line 宛先番号（0から始まることに注意）
     */
    public void deleteTo(int line) {
        if ( this.to.size() > line ) {
            this.to.remove(line);
        }
    }

    /**
     * このメールの発信元を取得します。
     * @return 発信元
     */
    public MailSender getFrom() {
        return from;
    }

    /**
     * このメールの発信元を設定します。
     * @param from 発信元
     */
    public void setFrom(MailSender from) {
        this.from = from;
    }

    /**
     * このメールのメッセージを取得します。
     * @return メッセージ
     */
    public List<String> getMessage() {
        return message;
    }

    /**
     * このメールのメッセージを設定します。
     * @param message メッセージ
     */
    public void setMessage(List<String> message) {
        this.message = message;
    }

    /**
     * このメールのメッセージを設定します。
     * @param line 行番号（0から始まることに注意）
     * @param message メッセージ
     */
    public void setMessage(int line, String message) {
        while ( this.message.size() <= line ) {
            this.message.add("");
        }
        this.message.set(line, message);
    }

    /**
     * このメールのメッセージに、指定した内容を追加します。
     * @param message メッセージ
     */
    public void addMessage(String message) {
        String[] lines = message.split("\n");
        for ( String line : lines ) {
            this.message.add(line);
        }
    }

    /**
     * このメールのメッセージの、指定された行番号を削除します。
     * @param line 宛先番号（0から始まることに注意）
     */
    public void deleteMessage(int line) {
        if ( this.message.size() > line && line >= 0 ) {
            this.message.remove(line);
        }
    }

    /**
     * 宛先グループを取得します。
     * @return 宛先グループ
     */
    public List<String> getToGroups() {
        return toGroups;
    }

    /**
     * 宛先グループを取得します。
     * @return 宛先グループ
     */
    public List<GroupData> getToGroupsConv() {
        GroupManager manager = UndineMailer.getInstance().getGroupManager();
        List<GroupData> result = new ArrayList<GroupData>();
        for ( String name : toGroups ) {
            GroupData group = manager.getGroup(name);
            if ( group != null ) {
                result.add(group);
            }
        }
        return result;
    }

    /**
     * このメールの宛先グループを設定します。
     * @param line 宛先番号（0から始まることに注意）
     * @param group グループ
     */
    public void setToGroup(int line, String group) {

        // 追加するグループが全体グループなら、
        // 他の宛先を全て削除する
        if ( SpecialGroupAll.NAME.equals(group) ) {
            this.to.clear();
            this.toGroups.clear();
            this.toGroups.add(group);
            return;
        }

        if ( this.toGroups.size() <= line ) {
            this.toGroups.add(group);
        } else if ( this.toGroups.size() > line ) {
            this.toGroups.set(line, group);
        }

        // 全体グループが含まれていたなら、全体グループを除去する
        if ( toGroups.contains(SpecialGroupAll.NAME) ) {
            toGroups.remove(SpecialGroupAll.NAME);
        }
    }

    /**
     * このメールの宛先グループに、新しいグループを追加します。
     * @param group グループ名
     */
    public void addToGroup(String group) {
        setToGroup(Integer.MAX_VALUE, group);
    }

    /**
     * このメールの宛先グループの、指定された行番号を削除します。
     * @param line 宛先番号（0から始まることに注意）
     */
    public void deleteToGroup(int line) {
        if ( this.toGroups.size() > line && line >= 0 ) {
            this.toGroups.remove(line);
        }
    }

    /**
     * 統合宛先を設定する
     * @param total 統合宛先
     */
    protected void setToTotal(List<MailSender> total) {
        this.toTotal = total;
    }

    /**
     * 統合宛先（宛先＋宛先グループの和集合）を取得する。未送信メールの場合はnullになる。
     * @return 統合宛先
     */
    public List<MailSender> getToTotal() {
        return toTotal;
    }

    /**
     * このメールに添付されたアイテムを取得します。
     * @return 添付アイテム
     */
    public List<ItemStack> getAttachments() {
        return attachments;
    }

    /**
     * このメールの添付アイテムを設定します。
     * @param attachments 添付アイテム
     */
    public void setAttachments(List<ItemStack> attachments) {
        this.attachments = attachments;
    }

    /**
     * 指定されたアイテムを添付アイテムに追加します。
     * @param item アイテム
     */
    public void addAttachment(ItemStack item) {
        this.attachments.add(item);
    }

    /**
     * このメールを読んだ人のリストを取得します。
     * @return 読んだ人のリスト
     */
    public List<MailSender> getReadFlags() {
        return readFlags;
    }

    /**
     * このメールに削除フラグを付けた人のリストを取得します。
     * @return 削除フラグをつけている人のリスト
     */
    public List<MailSender> getTrashFlags() {
        return trashFlags;
    }

    /**
     * このメールの添付アイテムを受け取るのに必要な金額を取得します。
     * @return 受け取り金額
     */
    public double getCostMoney() {
        return costMoney;
    }

    /**
     * このメールの添付アイテムを受け取るのに必要な金額を設定します。
     * @param feeMoney 受け取り金額
     */
    public void setCostMoney(double feeMoney) {
        this.costMoney = feeMoney;
    }

    /**
     * このメールの添付アイテムを受け取るのに必要な引き換えアイテムを取得します。
     * @return 引き換えアイテム
     */
    public ItemStack getCostItem() {
        return costItem;
    }

    /**
     * このメールの添付アイテムを受け取るのに必要な引き換えアイテムを設定します。
     * @param feeItem 引き換えアイテム
     */
    public void setCostItem(ItemStack feeItem) {
        this.costItem = feeItem;
    }

    /**
     * このメールの送信時間を取得します。
     * @return 送信時間
     */
    public Date getDate() {
        return date;
    }

    /**
     * このメールの送信時間を設定します（メール送信時に自動で割り当てられます）。
     * @param date 送信時間
     */
    protected void setDate(Date date) {
        this.date = date;
    }

    /**
     * このメールが送信された地点を取得します。
     * @return 送信地点
     */
    public Location getLocation() {
        return location;
    }

    /**
     * このメールの送信地点を設定します（メール送信時に自動で割り当てられます）。
     * @param location 送信地点
     */
    public void setLocation(Location location) {
        this.location = location;
    }

    /**
     * メール送信時の添付アイテムを取得します。
     * @return メール送信時の添付アイテム
     */
    public List<ItemStack> getAttachmentsOriginal() {
        return attachmentsOriginal;
    }

    /**
     * attachmentOriginalに、添付ファイルのコピーを行います
     * （メール送信時に自動で行われます）。
     */
    protected void makeAttachmentsOriginal() {
        attachmentsOriginal = new ArrayList<ItemStack>(attachments);
    }

    /**
     * 指定したプレイヤーが、このメールを読んだかどうかを返します。
     * @param player プレイヤー
     * @return 読んだかどうか
     */
    public boolean isRead(MailSender player) {
        return readFlags.contains(player);
    }

    /**
     * 指定した名前のsenderの既読マークを付ける
     * @param sender sender
     */
    public void setReadFlag(MailSender sender) {
        if ( !readFlags.contains(sender) ) {
            readFlags.add(sender);
        }
    }

    /**
     * 指定した人が、このメールに削除マークをつけているかどうかを返します。
     * @param sender
     * @return 削除マークをつけているかどうか
     */
    public boolean isSetTrash(MailSender sender) {
        return trashFlags.contains(sender);
    }

    /**
     * 指定した人の削除マークを付ける
     * @param sender
     */
    public void setTrashFlag(MailSender sender) {
        if ( !trashFlags.contains(sender) ) {
            trashFlags.add(sender);
        }
    }

    /**
     * 指定した人の削除マークを消す
     * @param sender
     */
    public void removeTrashFlag(MailSender sender) {
        if ( trashFlags.contains(sender) ) {
            trashFlags.remove(sender);
        }
    }

    /**
     * 指定された名前のプレイヤーは、このメールの関係者かどうかを返す。
     * @param sender sender
     * @return 指定された名前がtoまたはfromに含まれるかどうか
     */
    public boolean isRelatedWith(MailSender sender) {
        if ( isAllMail() ) return true;
        if ( from.equals(sender) ) return true;
        if ( toTotal != null ) return toTotal.contains(sender);
        return to.contains(sender);
    }

    /**
     * 指定された名前のプレイヤーは、このメールの受信者かどうかを返す。
     * @param sender sender
     * @return 指定された名前がtoに含まれるかどうか
     */
    public boolean isRecipient(MailSender sender) {
        if ( isAllMail() ) return true;
        if ( toTotal != null ) return toTotal.contains(sender);
        return to.contains(sender);
    }

    /**
     * このメールは編集中モードなのかどうかを返す
     * @return 編集中かどうか
     */
    public boolean isEditmode() {
        return (index == 0);
    }

    /**
     * このメールの添付アイテムがオープンされたのかどうかを返す
     * @return 添付アイテムがオープンされたのかどうか
     */
    public boolean isAttachmentsOpened() {
        return isAttachmentsOpened;
    }

    /**
     * このメールの添付アイテムをオープンされたとして記録する。
     * 受信者が添付ボックスを一度でも開いた事がある状態なら、
     * 送信者は添付アイテムをキャンセルすることができなくなる。
     */
    public void setOpenAttachments() {
        this.isAttachmentsOpened = true;
    }

    /**
     * このメールの添付アイテムがキャンセルされたのかどうかを返す
     * @return 添付アイテムがキャンセルされたのかどうか
     */
    public boolean isAttachmentsCancelled() {
        return isAttachmentsCancelled;
    }

    /**
     * このメールの添付アイテムをキャンセルする。
     * 添付アイテムがキャンセルされると、受信者はボックスを開けなくなり、
     * 逆に送信者がボックスを開くことができるようになる。
     */
    public void cancelAttachments() {
        this.isAttachmentsCancelled = true;
        this.costItem = null;
        this.costMoney = 0;
    }

    /**
     * このメールの添付アイテムが拒否されたのかどうかを返す
     * @return 添付アイテムが拒否されたのかどうか
     */
    public boolean isAttachmentsRefused() {
        return isAttachmentsRefused;
    }

    /**
     * 受取拒否の理由を取得します。
     * @return 受取拒否の理由（設定されていない場合はnullになることに注意すること）
     */
    public String getAttachmentsRefusedReason() {
        return attachmentsRefusedReason;
    }

    /**
     * このメールの添付アイテムを拒否する。
     * 添付アイテムが拒否されると、受信者はボックスを開けなくなり、
     * 逆に送信者がボックスを開くことができるようになる。
     * @param attachmentsRefusedReason 拒否理由
     */
    public void refuseAttachments(String attachmentsRefusedReason) {
        this.isAttachmentsCancelled = true; // キャンセルフラグも立てる
        this.isAttachmentsRefused = true;
        if ( attachmentsRefusedReason != null
                && attachmentsRefusedReason.length() > 0 ) {
            this.attachmentsRefusedReason = attachmentsRefusedReason;
        }
        this.costItem = null;
        this.costMoney = 0;
    }

    /**
     * このメールが全体メールなのかどうかを返します。
     * @return 全体メールかどうか
     */
    public boolean isAllMail() {
        return toGroups.contains(SpecialGroupAll.NAME);
    }

    /**
     * インスタンス同士の比較を行う。このメソッドを実装しておくことで、
     * Java8でのHashMapのキー挿入における高速化が期待できる（らしい）。
     * @param other 他のインスタンス
     * @return 比較結果
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(MailData other) {
        if ( this.index > other.index ) return 1;
        if ( this.index < other.index ) return -1;
        return 0;
    }

    /**
     * このメールのInbox用サマリー文字列を返す。
     * "送信者 (送信日時) 1行目の内容"
     * @return サマリー
     */
    protected String getInboxSummary() {

        String fdate = getFormattedDate(date);
        String summary = String.format("%s (%s) %s",
                from.getName(), fdate, Utility.removeColorCode(message.get(0)));

        // 長すぎる場合は切る
        if ( summary.length() > SUMMARY_MAX_SIZE + 2 ) {
            summary = summary.substring(0, SUMMARY_MAX_SIZE) + "..";
        }

        return summary;
    }

    /**
     * このメールのOutbox用サマリー文字列を返す。
     * "受信者 (送信日時) 1行目の内容"
     * @return サマリー
     */
    protected String getOutboxSummary() {

        String fdate = getFormattedDate(date);
        String todesc = joinToAndGroup();
        if ( todesc.length() > 15 ) { // 長すぎる場合は切る
            todesc = todesc.substring(0, 15);
        }
        String summary = String.format("%s (%s) %s",
                todesc, fdate, Utility.removeColorCode(message.get(0)));

        // 長すぎる場合は切る
        if ( summary.length() > SUMMARY_MAX_SIZE + 2 ) {
            summary = summary.substring(0, SUMMARY_MAX_SIZE) + "..";
        }

        return summary;
    }

    /**
     * 宛先のリストを、コンマを使ってつなげる
     * @return 繋がった文字列
     */
    private String joinToAndGroup() {

        StringBuffer buffer = new StringBuffer();
        for ( MailSender item : to ) {
            if ( buffer.length() > 0 ) {
                buffer.append(", ");
            }
            buffer.append(item.getName());
        }
        for ( String group : toGroups ) {
            if ( buffer.length() > 0 ) {
                buffer.append(", ");
            }
            buffer.append(group);
        }
        return buffer.toString();
    }

    /**
     * 言語リソース設定に従ってフォーマットされた日時の文字列を取得します。
     * @param date フォーマットする日時
     * @return フォーマットされた文字列
     */
    private String getFormattedDate(Date date) {
        return new SimpleDateFormat(Messages.get("DateFormat")).format(date);
    }

    /**
     * データのアップグレードを行う。
     * @return アップグレードを実行したかどうか
     */
    protected boolean upgrade() {
        boolean upgraded = false;
        for ( MailSender ms : to ) {
            if ( ms instanceof MailSenderPlayer ) {
                if ( ((MailSenderPlayer) ms).upgrade() ) {
                    upgraded = true;
                }
            }
        }
        if ( from instanceof MailSenderPlayer ) {
            if ( ((MailSenderPlayer) from).upgrade() ) {
                upgraded = true;
            }
        }
        for ( MailSender ms : toTotal ) {
            if ( ms instanceof MailSenderPlayer ) {
                if ( ((MailSenderPlayer) ms).upgrade() ) {
                    upgraded = true;
                }
            }
        }
        for ( MailSender ms : readFlags ) {
            if ( ms instanceof MailSenderPlayer ) {
                if ( ((MailSenderPlayer) ms).upgrade() ) {
                    upgraded = true;
                }
            }
        }
        for ( MailSender ms : trashFlags ) {
            if ( ms instanceof MailSenderPlayer ) {
                if ( ((MailSenderPlayer) ms).upgrade() ) {
                    upgraded = true;
                }
            }
        }
        return upgraded;
    }
}
