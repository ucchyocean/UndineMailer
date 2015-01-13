/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.mc;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

/**
 * メールのデータ
 * @author ucchy
 */
public class MailData {

    private static final int SUMMARY_MAX_SIZE = 40;

    private List<String> to;
    private String from;
    private List<String> message;
    private List<ItemStack> attachments;
    private int feeMoney;
    private ItemStack feeItem;

    private int index;
    private Map<String, Boolean> readFlags;
    private List<ItemStack> attachmentsOriginal;
    private Date date;

    /**
     * コンストラクタ
     */
    public MailData() {
        this.index = 0;
        this.to = new ArrayList<String>();
        this.message = new ArrayList<String>();
        this.attachments = new ArrayList<ItemStack>();
        this.readFlags = new HashMap<String, Boolean>();
    }

    /**
     * コンストラクタ
     * @param to 宛先
     * @param from 送り主
     * @param message メッセージ
     */
    public MailData(List<String> to, String from, List<String> message) {
        this.index = 0;
        this.to = to;
        this.from = from;
        this.message = message;
        this.attachments = new ArrayList<ItemStack>();
        this.readFlags = new HashMap<String, Boolean>();
    }

    /**
     * コンストラクタ
     * @param to 宛先
     * @param from 送り主
     * @param message メッセージ
     * @param attachments 添付アイテム
     */
    public MailData(List<String> to, String from, List<String> message,
            List<ItemStack> attachments) {
        this.index = 0;
        this.to = to;
        this.from = from;
        this.message = message;
        this.attachments = attachments;
        this.readFlags = new HashMap<String, Boolean>();
    }

    /**
     * コンストラクタ
     * @param to 宛先
     * @param from 送り主
     * @param message メッセージ
     * @param attachments 添付アイテム
     * @param feeMoney 受け取りにかかる金額
     * @param feeItem 受け取りにかかる取引アイテム
     */
    public MailData(List<String> to, String from, List<String> message,
            List<ItemStack> attachments, int feeMoney, ItemStack feeItem) {
        this.index = 0;
        this.to = to;
        this.from = from;
        this.message = message;
        this.attachments = attachments;
        this.feeMoney = feeMoney;
        this.feeItem = feeItem;
        this.readFlags = new HashMap<String, Boolean>();
    }

    /**
     * 指定されたファイルへ保存する
     * @param file 保存先
     */
    protected void save(File file) {

        YamlConfiguration config = new YamlConfiguration();

        config.set("to", to);
        config.set("from", from);
        config.set("message", message);

        if ( attachments != null ) {
            ConfigurationSection section = config.createSection("attachments");
            int i = 1;
            for ( ItemStack item : attachments ) {
                ConfigurationSection sub = section.createSection("attachment" + i++);
                ItemConfigParser.setItemToSection(sub, item);
            }
        }

        config.set("feeMoney", feeMoney);

        if ( feeItem != null ) {
            ConfigurationSection sub = config.createSection("feeItem");
            ItemConfigParser.setItemToSection(sub, feeItem);
        }

        config.set("index", index);

        ConfigurationSection rsec = config.createSection("readFlags");
        for ( String name : readFlags.keySet() ) {
            rsec.set(name, readFlags.get(name));
        }

        if ( attachmentsOriginal != null ) {
            ConfigurationSection section = config.createSection("attachmentsOriginal");
            int i = 1;
            for ( ItemStack item : attachmentsOriginal ) {
                ConfigurationSection sub = section.createSection("attachment" + i++);
                ItemConfigParser.setItemToSection(sub, item);
            }
        }

        if ( date != null ) {
            config.set("date", date.getTime());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 指定されたファイルからロードする
     * @param file ファイル
     * @return ロードされたMailData
     */
    protected static MailData load(File file) {

        MailData data = new MailData();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        data.to = config.getStringList("to");
        data.from = config.getString("from", "");
        data.message = config.getStringList("message");

        if ( config.contains("attachments") ) {
            data.attachments = new ArrayList<ItemStack>();

            for ( String name : config.getConfigurationSection("attachments").getKeys(false) ) {
                ConfigurationSection sub = config.getConfigurationSection("attachments." + name);
                try {
                    ItemStack item = ItemConfigParser.getItemFromSection(sub);
                    data.attachments.add(item);
                } catch (ItemConfigParseException e) {
                    e.printStackTrace();
                }
            }
        }

        data.feeMoney = config.getInt("feeMoney", 0);
        if ( config.contains("feeItem") ) {
            try {
                data.feeItem = ItemConfigParser.getItemFromSection(
                        config.getConfigurationSection("feeItem"));
            } catch (ItemConfigParseException e) {
                e.printStackTrace();
            }
        }

        data.index = config.getInt("index");

        for ( String name : config.getConfigurationSection("readFlags").getKeys(false) ) {
            data.readFlags.put(name, config.getBoolean("readFlags." + name, false));
        }

        if ( config.contains("attachmentsOriginal") ) {
            data.attachmentsOriginal = new ArrayList<ItemStack>();

            for ( String name :
                    config.getConfigurationSection("attachmentsOriginal").getKeys(false) ) {
                ConfigurationSection sub =
                        config.getConfigurationSection("attachmentsOriginal." + name);
                try {
                    ItemStack item = ItemConfigParser.getItemFromSection(sub);
                    data.attachmentsOriginal.add(item);
                } catch (ItemConfigParseException e) {
                    e.printStackTrace();
                }
            }
        }

        if ( config.contains("date") ) {
            data.date = new Date(config.getLong("date"));
        }

        return data;
    }

    /**
     * このオブジェクトの複製を作成して返す。
     * @see java.lang.Object#clone()
     */
    protected MailData clone() {
        ArrayList<ItemStack> attach_copy = new ArrayList<ItemStack>(attachments);
        return new MailData(to, from, message, attach_copy, feeMoney, feeItem);
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
    public List<String> getTo() {
        return to;
    }

    /**
     * このメールの宛先を設定します。
     * @param to 宛先
     */
    public void setTo(List<String> to) {
        this.to = to;
    }

    /**
     * このメールの発信元を取得します。
     * @return 発信元
     */
    public String getFrom() {
        return from;
    }

    /**
     * このメールの発信元を設定します。
     * @param from 発信元
     */
    public void setFrom(String from) {
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
     * このメールが既読かどうかを取得します。
     * @return 既読かどうか
     */
    public Map<String, Boolean> getReadFlags() {
        return readFlags;
    }

    /**
     * このメールの添付アイテムを受け取るのに必要な金額を取得します。
     * @return 受け取り金額
     */
    public int getFeeMoney() {
        return feeMoney;
    }

    /**
     * このメールの添付アイテムを受け取るのに必要な金額を設定します。
     * @param feeMoney 受け取り金額
     */
    public void setFeeMoney(int feeMoney) {
        this.feeMoney = feeMoney;
    }

    /**
     * このメールの添付アイテムを受け取るのに必要な引き換えアイテムを取得します。
     * @return 引き換えアイテム
     */
    public ItemStack getFeeItem() {
        return feeItem;
    }

    /**
     * このメールの添付アイテムを受け取るのに必要な引き換えアイテムを設定します。
     * @param feeItem 引き換えアイテム
     */
    public void setFeeItem(ItemStack feeItem) {
        this.feeItem = feeItem;
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
     * 指定した名前のプレイヤーが、このメールを読んだかどうかを返します。
     * @param name プレイヤー名
     * @return 読んだかどうか
     */
    public boolean isRead(String name) {
        return (readFlags.containsKey(name) ? readFlags.get(name) : false);
    }

    /**
     * 指定した名前のプレイヤーの既読マークを付ける
     * @param name プレイヤー名
     */
    public void setReadFlag(String name) {
        readFlags.put(name, true);
    }

    /**
     * メールの詳細情報を返す
     * @return 詳細情報
     */
    protected ArrayList<String> getDescription() {

        ArrayList<String> desc = new ArrayList<String>();

        String num = (index == 0) ? Messages.get("Editmode") : index + "";
        String fdate = getFormattedDate(date);
        String pre = Messages.get("MailDetailLinePre");

        desc.add(Messages.get("MailDetailFirstLine", "%number", num));
        desc.add(Messages.get("MailDetailFromLine", "%from", from));
        desc.add(Messages.get("MailDetailToLine", "%to", join(to, ",")));
        desc.add(Messages.get("MailDetailDateLine", "%date", fdate));
        desc.add(Messages.get("MailDetailMessageLine"));
        for ( String m : message ) {
            desc.add(pre + "  " + ChatColor.WHITE + m);
        }

        if ( attachments.size() > 0 ||
                (attachmentsOriginal != null && attachmentsOriginal.size() > 0) ) {

            desc.add(Messages.get("MailDetailAttachmentsLine"));
            if ( attachmentsOriginal != null ) {
                for ( ItemStack i : attachmentsOriginal ) {
                    desc.add(pre + "  " + ChatColor.WHITE + getItemDesc(i));
                }
            } else {
                for ( ItemStack i : attachments ) {
                    desc.add(pre + "  " + ChatColor.WHITE + getItemDesc(i));
                }
            }
        }

        desc.add(Messages.get("MailDetailLastLine"));

        return desc;
    }

    /**
     * このメールのInbox用サマリー文字列を返す
     * @return サマリー
     */
    protected String getInboxSummary() {

        String fdate = getFormattedDate(date);
        String summary = String.format("%s (%s) %s",
                from, fdate, message.get(0));

        // 長すぎる場合は切る
        if ( summary.length() > SUMMARY_MAX_SIZE ) {
            summary = summary.substring(0, SUMMARY_MAX_SIZE) + "...";
        }

        return summary;
    }

    /**
     * このメールのOutbox用サマリー文字列を返す
     * @return サマリー
     */
    protected String getOutboxSummary() {

        String fdate = getFormattedDate(date);
        String summary = String.format("%s (%s) %s",
                join(to, ","), fdate, message.get(0));

        // 長すぎる場合は切る
        if ( summary.length() > SUMMARY_MAX_SIZE ) {
            summary = summary.substring(0, SUMMARY_MAX_SIZE) + "...";
        }

        return summary;
    }

    /**
     * 文字列のリストを、指定された文字を使ってつなげる
     * @param list 文字列のリスト
     * @param delim つなげる文字
     * @return 繋がった文字列
     */
    private String join(List<String> list, String delim) {

        StringBuffer buffer = new StringBuffer();
        for ( String item : list ) {
            if ( buffer.length() > 0 ) {
                buffer.append(delim);
            }
            buffer.append(item);
        }
        return buffer.toString();
    }

    /**
     * アイテムを簡単な文字列表現にして返す
     * @param item アイテム
     * @return 文字列表現
     */
    private String getItemDesc(ItemStack item) {
        if ( item.getAmount() == 1 ) return item.getType().toString();
        return item.getType().toString() + ":" + item.getAmount();
    }

    /**
     * 指定された名前のプレイヤーは、このメールの関係者かどうかを返す。
     * @param name プレイヤー
     * @return 指定された名前がtoまたはfromに含まれるかどうか
     */
    public boolean isRelatedWith(String name) {
        return to.contains(name) || from.equals(name);
    }

    /**
     * 言語リソース設定に従ってフォーマットされた日時の文字列を取得します。
     * @param date フォーマットする日時
     * @return フォーマットされた文字列
     */
    private String getFormattedDate(Date date) {
        return new SimpleDateFormat(Messages.get("DateFormat")).format(date);
    }
}
