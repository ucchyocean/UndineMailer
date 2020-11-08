/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bitbucket.ucchy.undine.group.SpecialGroupAll;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bitbucket.ucchy.undine.sender.MailSenderPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import com.github.ucchyocean.itemconfig.ItemConfigParseException;
import com.github.ucchyocean.itemconfig.ItemConfigParser;

/**
 * メールのデータ
 * @author ucchy
 */
public class MailDataFlatFile extends MailData {

    // 編集中に設定される属性
    private List<MailSender> to;
    private List<String> toGroups;
    private MailSender from;
    private List<String> message;
    private List<ItemStack> attachments;
    private double costMoney;
    private ItemStack costItem;

    // 送信後に設定される属性
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
    public MailDataFlatFile() {
        this(new ArrayList<MailSender>(), null, new ArrayList<String>());
    }

    /**
     * コンストラクタ
     * 
     * @param to      宛先
     * @param from    送り主
     * @param message メッセージ
     */
    public MailDataFlatFile(List<MailSender> to, MailSender from, String message) {
        this(to, from, new ArrayList<String>());
        this.message.add(message);
    }

    /**
     * コンストラクタ
     * 
     * @param to      宛先
     * @param from    送り主
     * @param message メッセージ
     */
    public MailDataFlatFile(List<MailSender> to, MailSender from, List<String> message) {
        this(to, from, message, new ArrayList<ItemStack>());
    }

    /**
     * コンストラクタ
     * 
     * @param to          宛先
     * @param from        送り主
     * @param message     メッセージ
     * @param attachments 添付アイテム
     */
    public MailDataFlatFile(List<MailSender> to, MailSender from, List<String> message, List<ItemStack> attachments) {
        this(to, from, message, attachments, 0, null);
    }

    /**
     * コンストラクタ
     * 
     * @param to          宛先
     * @param from        送り主
     * @param message     メッセージ
     * @param attachments 添付アイテム
     * @param costMoney   受け取りにかかる金額
     * @param costItem    受け取りにかかる取引アイテム
     */
    public MailDataFlatFile(List<MailSender> to, MailSender from, List<String> message, List<ItemStack> attachments,
            double costMoney, ItemStack costItem) {
        this(to, from, message, attachments, costMoney, costItem, new ArrayList<String>());
    }

    /**
     * コンストラクタ
     * 
     * @param to          宛先
     * @param from        送り主
     * @param message     メッセージ
     * @param attachments 添付アイテム
     * @param costMoney   受け取りにかかる金額
     * @param costItem    受け取りにかかる取引アイテム
     * @param toGroup     宛先グループ
     */
    public MailDataFlatFile(List<MailSender> to, MailSender from, List<String> message, List<ItemStack> attachments,
            double costMoney, ItemStack costItem, List<String> toGroup) {
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
     * このメールが送信されたかどうか、すなわち編集中かどうかを返す。
     * @return このメールが送信されたかどうか
     */
    @Override
    public boolean isSent() {
        return index != 0;
    }

    /**
     * ファイルへ保存する
     */
    @Override
    public void save() {
        // 編集中で未送信のメールは保存できません。
        if (!isSent()) {
            return;
        }
        String filename = String.format("%1$08d.yml", index);
        File folder = UndineMailer.getInstance().getMailFolder();
        File file = new File(folder, filename);
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
     * 
     * @param section コンフィグセクション
     */
    protected void saveToConfigSection(ConfigurationSection section) {

        ArrayList<String> toList = new ArrayList<String>();
        for (MailSender t : to) {
            toList.add(t.toString());
        }
        section.set("to", toList);

        section.set("toGroups", toGroups);

        if (toTotal != null) {
            ArrayList<String> toTotalList = new ArrayList<String>();
            for (MailSender t : toTotal) {
                toTotalList.add(t.toString());
            }
            section.set("toTotal", toTotalList);
        }

        section.set("from", from.toString());
        section.set("message", message);

        if (attachments != null) {
            ConfigurationSection sub = section.createSection("attachments");
            int i = 1;
            for (ItemStack item : attachments) {
                ConfigurationSection subsub = sub.createSection("attachment" + i++);
                ItemConfigParser.setItemToSection(subsub, item);
            }
        }

        section.set("costMoney", costMoney);

        if (costItem != null) {
            ConfigurationSection sub = section.createSection("costItem");
            ItemConfigParser.setItemToSection(sub, costItem);
        }

        section.set("index", index);

        ArrayList<String> flagList = new ArrayList<String>();
        for (MailSender t : readFlags) {
            flagList.add(t.toString());
        }
        section.set("readFlags", flagList);

        ArrayList<String> trashList = new ArrayList<String>();
        for (MailSender t : trashFlags) {
            trashList.add(t.toString());
        }
        section.set("trashFlags", trashList);

        if (attachmentsOriginal != null) {
            ConfigurationSection sub = section.createSection("attachmentsOriginal");
            int i = 1;
            for (ItemStack item : attachmentsOriginal) {
                ConfigurationSection subsub = sub.createSection("attachment" + i++);
                ItemConfigParser.setItemToSection(subsub, item);
            }
        }

        if (date != null) {
            section.set("date", date.getTime());
        }

        if (location != null) {
            World world = location.getWorld();
            if (world != null) {
                ConfigurationSection locSec = section.createSection("location");
                locSec.set("world", world.getName());
                locSec.set("x", location.getX());
                locSec.set("y", location.getY());
                locSec.set("z", location.getZ());
                locSec.set("yaw", location.getYaw());
                locSec.set("pitch", location.getPitch());
            }
        }

        section.set("isAttachmentsCancelled", isAttachmentsCancelled);
        section.set("isAttachmentsRefused", isAttachmentsRefused);
        section.set("attachmentsRefusedReason", attachmentsRefusedReason);
    }

    /**
     * 指定されたファイルからロードする
     * 
     * @param file ファイル
     * @return ロードされたMailData
     */
    protected static MailDataFlatFile load(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        return loadFromConfigSection(config);
    }

    /**
     * 指定されたコンフィグセクションからロードする
     * 
     * @param section コンフィグセクション
     * @return ロードされたMailData
     */
    protected static MailDataFlatFile loadFromConfigSection(ConfigurationSection section) {

        MailDataFlatFile data = new MailDataFlatFile();

        data.to = new ArrayList<MailSender>();
        for (String t : section.getStringList("to")) {
            MailSender sender = MailSender.getMailSenderFromString(t);
            if (sender != null) {
                data.to.add(sender);
            }
        }

        data.toGroups = section.getStringList("toGroups");

        if (section.contains("toTotal")) {
            data.toTotal = new ArrayList<MailSender>();
            for (String t : section.getStringList("toTotal")) {
                MailSender sender = MailSender.getMailSenderFromString(t);
                if (sender != null) {
                    data.toTotal.add(sender);
                }
            }
        }

        data.from = MailSender.getMailSenderFromString(section.getString("from"));
        data.message = section.getStringList("message");

        ConfigurationSection attachmentsSec = section.getConfigurationSection("attachments");
        if (attachmentsSec != null) {
            data.attachments = new ArrayList<ItemStack>();

            for (String name : attachmentsSec.getKeys(false)) {
                ConfigurationSection sub = section.getConfigurationSection("attachments." + name);
                try {
                    ItemStack item = ItemConfigParser.getItemFromSection(sub);
                    if (item != null)
                        data.attachments.add(item);
                } catch (ItemConfigParseException e) {
                    e.printStackTrace();
                }
            }
        }

        data.costMoney = section.getInt("costMoney", 0);
        if (section.contains("costItem")) {
            try {
                data.costItem = ItemConfigParser.getItemFromSection(section.getConfigurationSection("costItem"));
            } catch (ItemConfigParseException e) {
                e.printStackTrace();
            }
        }

        data.index = section.getInt("index");

        data.readFlags = new ArrayList<MailSender>();
        for (String t : section.getStringList("readFlags")) {
            MailSender sender = MailSender.getMailSenderFromString(t);
            if (sender != null) {
                data.readFlags.add(sender);
            }
        }

        data.trashFlags = new ArrayList<MailSender>();
        for (String t : section.getStringList("trashFlags")) {
            MailSender sender = MailSender.getMailSenderFromString(t);
            if (sender != null) {
                data.trashFlags.add(sender);
            }
        }

        ConfigurationSection attachmentsOrgSec = section.getConfigurationSection("attachmentsOriginal");
        if (attachmentsOrgSec != null) {
            data.attachmentsOriginal = new ArrayList<ItemStack>();

            for (String name : attachmentsOrgSec.getKeys(false)) {
                ConfigurationSection sub = attachmentsOrgSec.getConfigurationSection(name);
                if (sub != null) {
                    try {
                        ItemStack item = ItemConfigParser.getItemFromSection(sub);
                        data.attachmentsOriginal.add(item);
                    } catch (ItemConfigParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        if (section.contains("date")) {
            data.date = new Date(section.getLong("date"));
        }

        ConfigurationSection locSec = section.getConfigurationSection("location");
        if (locSec != null) {
            String worldName = locSec.getString("world");
            if (worldName != null) {
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    double x = locSec.getDouble("x");
                    double y = locSec.getDouble("y");
                    double z = locSec.getDouble("z");
                    double yaw = locSec.getDouble("yaw");
                    double pitch = locSec.getDouble("pitch");
                    data.location = new Location(world, x, y, z, (float) yaw, (float) pitch);
                }
            }
        }

        data.isAttachmentsCancelled = section.getBoolean("isAttachmentsCancelled", false);
        data.isAttachmentsRefused = section.getBoolean("isAttachmentsRefused", false);
        data.attachmentsRefusedReason = section.getString("attachmentsRefusedReason");

        return data;
    }

    /**
     * このオブジェクトの複製を作成して返す。
     * 
     * @see java.lang.Object#clone()
     */
    @Override
    public MailDataFlatFile clone() {
        return new MailDataFlatFile(new ArrayList<MailSender>(to), from, message, new ArrayList<ItemStack>(attachments),
                costMoney, costItem, toGroups);
    }

    /**
     * 設定されている宛先を全て消去する
     */
    @Override
    public void deleteAllTo() {
        to.clear();
        toGroups.clear();
    }

    /**
     * このメールの宛先を取得します。
     * 
     * @return 宛先
     */
    @Override
    public List<MailSender> getTo() {
        return to;
    }

    /**
     * このメールの宛先を設定します。
     * 
     * @param line 宛先番号（0から始まることに注意）
     * @param to   宛先
     */
    @Override
    public void setTo(int line, MailSender to) {
        if (this.to.size() <= line) {
            this.to.add(to);
        } else if (this.to.size() > line) {
            this.to.set(line, to);
        }

        // 全体メールだった場合は、全体グループを除去しておく。
        if (isAllMail()) {
            toGroups.remove(SpecialGroupAll.NAME);
        }
    }

    /**
     * このメールの宛先を追加します。
     * 
     * @param to 宛先
     */
    @Override
    public void addTo(MailSender to) {
        setTo(Integer.MAX_VALUE, to);
    }

    /**
     * このメールの宛后を複数追加します。
     */
    @Override
    public void addTo(List<MailSender> to) {
        getTo().addAll(to);
    }

    /**
     * このメールの指定された宛先を削除します。
     * 
     * @param line 宛先番号（0から始まることに注意）
     */
    @Override
    public void deleteTo(int line) {
        if (this.to.size() > line) {
            this.to.remove(line);
        }
    }

    /**
     * このメールの発信元を取得します。
     * 
     * @return 発信元
     */
    @Override
    public MailSender getFrom() {
        return from;
    }

    /**
     * このメールの発信元を設定します。
     * 
     * @param from 発信元
     */
    @Override
    public void setFrom(MailSender from) {
        this.from = from;
    }

    /**
     * このメールのメッセージを取得します。
     * 
     * @return メッセージ
     */
    @Override
    public List<String> getMessage() {
        return message;
    }

    /**
     * このメールのメッセージを設定します。
     * 
     * @param message メッセージ
     */
    @Override
    public void setMessage(List<String> message) {
        this.message = message;
    }

    /**
     * このメールのメッセージを設定します。
     * 
     * @param line    行番号（0から始まることに注意）
     * @param message メッセージ
     */
    @Override
    public void setMessage(int line, String message) {
        while (this.message.size() <= line) {
            this.message.add("");
        }
        this.message.set(line, message);
    }

    /**
     * このメールのメッセージに、指定した内容を追加します。
     * 
     * @param message メッセージ
     */
    @Override
    public void addMessage(String message) {
        String[] lines = message.split("\r\n|\r|\n", -1);
        for (String line : lines) {
            this.message.add(line);
        }
    }

    /**
     * このメールのメッセージの、指定された行番号を削除します。
     * 
     * @param line 宛先番号（0から始まることに注意）
     */
    @Override
    public void deleteMessage(int line) {
        if (this.message.size() > line && line >= 0) {
            this.message.remove(line);
        }
    }

    /**
     * 宛先グループを取得します。
     * 
     * @return 宛先グループ
     */
    @Override
    public List<String> getToGroups() {
        return toGroups;
    }

    /**
     * このメールの宛先グループを設定します。
     * 
     * @param line  宛先番号（0から始まることに注意）
     * @param group グループ
     */
    @Override
    public void setToGroup(int line, String group) {

        // 追加するグループが全体グループなら、
        // 他の宛先を全て削除する
        if (SpecialGroupAll.NAME.equals(group)) {
            this.to.clear();
            this.toGroups.clear();
            this.toGroups.add(group);
            return;
        }

        if (this.toGroups.size() <= line) {
            this.toGroups.add(group);
        } else if (this.toGroups.size() > line) {
            this.toGroups.set(line, group);
        }

        // 全体グループが含まれていたなら、全体グループを除去する
        if (toGroups.contains(SpecialGroupAll.NAME)) {
            toGroups.remove(SpecialGroupAll.NAME);
        }
    }

    /**
     * このメールの宛先グループの、指定された行番号を削除します。
     * 
     * @param line 宛先番号（0から始まることに注意）
     */
    @Override
    public void deleteToGroup(int line) {
        if (this.toGroups.size() > line && line >= 0) {
            this.toGroups.remove(line);
        }
    }

    /**
     * 統合宛先を設定する
     * 
     * @param total 統合宛先
     */
    @Override
    protected void setToTotal(List<MailSender> total) {
        this.toTotal = total;
    }

    /**
     * 統合宛先（宛先＋宛先グループの和集合）を取得する。未送信メールの場合はnullになる。
     * 
     * @return 統合宛先
     */
    @Override
    public List<MailSender> getToTotal() {
        return toTotal;
    }

    /**
     * このメールに添付されたアイテムを取得します。
     * 
     * @return 添付アイテム
     */
    @Override
    public List<ItemStack> getAttachments() {
        return attachments;
    }

    /**
     * このメールの添付アイテムを設定します。
     * 
     * @param attachments 添付アイテム
     */
    @Override
    public void setAttachments(List<ItemStack> attachments) {
        this.attachments = attachments;
    }

    /**
     * 指定されたアイテムを添付アイテムに追加します。
     * 
     * @param item アイテム
     */
    @Override
    public void addAttachment(ItemStack item) {
        this.attachments.add(item);
    }

    /**
     * このメールを読んだ人のリストを取得します。
     * 
     * @return 読んだ人のリスト
     */
    @Override
    public List<MailSender> getReadFlags() {
        return readFlags;
    }

    /**
     * このメールに削除フラグを付けた人のリストを取得します。
     * 
     * @return 削除フラグをつけている人のリスト
     */
    @Override
    public List<MailSender> getTrashFlags() {
        return trashFlags;
    }

    /**
     * このメールの添付アイテムを受け取るのに必要な金額を取得します。
     * 
     * @return 受け取り金額
     */
    @Override
    public double getCostMoney() {
        return costMoney;
    }

    /**
     * このメールの添付アイテムを受け取るのに必要な金額を設定します。
     * 
     * @param feeMoney 受け取り金額
     */
    @Override
    public void setCostMoney(double feeMoney) {
        this.costMoney = feeMoney;
    }

    /**
     * このメールの添付アイテムを受け取るのに必要な引き換えアイテムを取得します。
     * 
     * @return 引き換えアイテム
     */
    @Override
    public ItemStack getCostItem() {
        return costItem;
    }

    /**
     * このメールの添付アイテムを受け取るのに必要な引き換えアイテムを設定します。
     * 
     * @param feeItem 引き換えアイテム
     */
    @Override
    public void setCostItem(ItemStack feeItem) {
        this.costItem = feeItem;
    }

    /**
     * このメールの送信時間を取得します。
     * 
     * @return 送信時間
     */
    @Override
    public Date getDate() {
        return date;
    }

    /**
     * このメールの送信時間を設定します（メール送信時に自動で割り当てられます）。
     * 
     * @param date 送信時間
     */
    @Override
    public void setDate(Date date) {
        this.date = date;
    }

    /**
     * このメールが送信された地点を取得します。
     * 
     * @return 送信地点
     */
    @Override
    public Location getLocation() {
        return location;
    }

    /**
     * このメールの送信地点を設定します（メール送信時に自動で割り当てられます）。
     * 
     * @param location 送信地点
     */
    @Override
    public void setLocation(Location location) {
        this.location = location;
    }

    /**
     * メール送信時の添付アイテムを取得します。
     * 
     * @return メール送信時の添付アイテム
     */
    @Override
    public List<ItemStack> getAttachmentsOriginal() {
        return attachmentsOriginal;
    }

    /**
     * attachmentOriginalに、添付ファイルのコピーを行います （メール送信時に自動で行われます）。
     */
    @Override
    protected void makeAttachmentsOriginal() {
        attachmentsOriginal = new ArrayList<ItemStack>(attachments);
    }

    /**
     * 指定したプレイヤーが、このメールを読んだかどうかを返します。
     * 
     * @param player プレイヤー
     * @return 読んだかどうか
     */
    @Override
    public boolean isRead(MailSender player) {
        return readFlags.contains(player);
    }

    /**
     * 指定した名前のsenderの既読マークを付ける
     * 
     * @param sender sender
     */
    @Override
    public void setReadFlag(MailSender sender) {
        if (!readFlags.contains(sender)) {
            readFlags.add(sender);
        }
    }

    /**
     * 指定した人が、このメールに削除マークをつけているかどうかを返します。
     * 
     * @param sender
     * @return 削除マークをつけているかどうか
     */
    @Override
    public boolean isSetTrash(MailSender sender) {
        return trashFlags.contains(sender);
    }

    /**
     * 指定した人の削除マークを付ける
     * 
     * @param sender
     */
    @Override
    public void setTrashFlag(MailSender sender) {
        if (!trashFlags.contains(sender)) {
            trashFlags.add(sender);
        }
    }

    /**
     * 指定した人の削除マークを消す
     * 
     * @param sender
     */
    @Override
    public void removeTrashFlag(MailSender sender) {
        if (trashFlags.contains(sender)) {
            trashFlags.remove(sender);
        }
    }

    /**
     * 指定された名前のプレイヤーは、このメールの関係者かどうかを返す。
     * 
     * @param sender sender
     * @return 指定された名前がtoまたはfromに含まれるかどうか
     */
    @Override
    public boolean isRelatedWith(MailSender sender) {
        if (isAllMail())
            return true;
        if (from.equals(sender))
            return true;
        if (toTotal != null)
            return toTotal.contains(sender);
        return to.contains(sender);
    }

    /**
     * 指定された名前のプレイヤーは、このメールの受信者かどうかを返す。
     * 
     * @param sender sender
     * @return 指定された名前がtoに含まれるかどうか
     */
    @Override
    public boolean isRecipient(MailSender sender) {
        if (isAllMail())
            return true;
        if (toTotal != null)
            return toTotal.contains(sender);
        return to.contains(sender);
    }

    /**
     * このメールの添付アイテムがオープンされたのかどうかを返す
     * 
     * @return 添付アイテムがオープンされたのかどうか
     */
    @Override
    public boolean isAttachmentsOpened() {
        return isAttachmentsOpened;
    }

    /**
     * このメールの添付アイテムをオープンされたとして記録する。 受信者が添付ボックスを一度でも開いた事がある状態なら、
     * 送信者は添付アイテムをキャンセルすることができなくなる。
     */
    @Override
    public void setOpenAttachments() {
        this.isAttachmentsOpened = true;
    }

    /**
     * このメールの添付アイテムがキャンセルされたのかどうかを返す
     * 
     * @return 添付アイテムがキャンセルされたのかどうか
     */
    @Override
    public boolean isAttachmentsCancelled() {
        return isAttachmentsCancelled;
    }

    /**
     * このメールの添付アイテムをキャンセルする。 添付アイテムがキャンセルされると、受信者はボックスを開けなくなり、
     * 逆に送信者がボックスを開くことができるようになる。
     */
    @Override
    public void cancelAttachments() {
        this.isAttachmentsCancelled = true;
        this.costItem = null;
        this.costMoney = 0;
    }

    /**
     * このメールの添付アイテムが拒否されたのかどうかを返す
     * 
     * @return 添付アイテムが拒否されたのかどうか
     */
    @Override
    public boolean isAttachmentsRefused() {
        return isAttachmentsRefused;
    }

    /**
     * 受取拒否の理由を取得します。
     * 
     * @return 受取拒否の理由（設定されていない場合はnullになることに注意すること）
     */
    @Override
    public String getAttachmentsRefusedReason() {
        return attachmentsRefusedReason;
    }

    /**
     * このメールの添付アイテムを拒否する。 添付アイテムが拒否されると、受信者はボックスを開けなくなり、 逆に送信者がボックスを開くことができるようになる。
     * 
     * @param attachmentsRefusedReason 拒否理由
     */
    @Override
    public void refuseAttachments(String attachmentsRefusedReason) {
        this.isAttachmentsCancelled = true; // キャンセルフラグも立てる
        this.isAttachmentsRefused = true;
        if (attachmentsRefusedReason != null && attachmentsRefusedReason.length() > 0) {
            this.attachmentsRefusedReason = attachmentsRefusedReason;
        }
        this.costItem = null;
        this.costMoney = 0;
    }

    /**
     * データのアップグレードを行う。
     * @return アップグレードを実行したかどうか
     */
    @Override
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

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && o instanceof MailDataFlatFile;
    }
}
