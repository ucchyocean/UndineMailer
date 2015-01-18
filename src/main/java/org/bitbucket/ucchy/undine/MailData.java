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

import org.bitbucket.ucchy.undine.item.ItemConfigParseException;
import org.bitbucket.ucchy.undine.item.ItemConfigParser;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bitbucket.ucchy.undine.sender.MailSenderPlayer;
import org.bitbucket.ucchy.undine.tellraw.ClickEventType;
import org.bitbucket.ucchy.undine.tellraw.MessageComponent;
import org.bitbucket.ucchy.undine.tellraw.MessageParts;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

/**
 * メールのデータ
 * @author ucchy
 */
public class MailData {

    private static final String COMMAND = Undine.COMMAND;

    private static final int SUMMARY_MAX_SIZE = 40;
    protected static final int TO_MAX_SIZE = 10;
    protected static final int MESSAGE_MAX_SIZE = 15;
    private static final int MESSAGE_ADD_SIZE = 3;

    private List<MailSender> to;
    private MailSender from;
    private List<String> message;
    private List<ItemStack> attachments;
    private int feeMoney;
    private ItemStack feeItem;

    private int index;
    private List<MailSender> readFlags;
    private List<ItemStack> attachmentsOriginal;
    private Date date;

    /**
     * コンストラクタ
     */
    public MailData() {
        this.index = 0;
        this.to = new ArrayList<MailSender>();
        this.message = new ArrayList<String>();
        this.attachments = new ArrayList<ItemStack>();
        this.readFlags = new ArrayList<MailSender>();
    }

    /**
     * コンストラクタ
     * @param to 宛先
     * @param from 送り主
     * @param message メッセージ
     */
    public MailData(List<MailSender> to, MailSender from, String message) {
        this.index = 0;
        this.to = to;
        this.from = from;
        this.message = new ArrayList<String>();
        this.message.add(message);
        this.attachments = new ArrayList<ItemStack>();
        this.readFlags = new ArrayList<MailSender>();
    }

    /**
     * コンストラクタ
     * @param to 宛先
     * @param from 送り主
     * @param message メッセージ
     */
    public MailData(List<MailSender> to, MailSender from, List<String> message) {
        this.index = 0;
        this.to = to;
        this.from = from;
        this.message = message;
        this.attachments = new ArrayList<ItemStack>();
        this.readFlags = new ArrayList<MailSender>();
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
        this.index = 0;
        this.to = to;
        this.from = from;
        this.message = message;
        this.attachments = attachments;
        this.readFlags = new ArrayList<MailSender>();
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
    public MailData(List<MailSender> to, MailSender from, List<String> message,
            List<ItemStack> attachments, int feeMoney, ItemStack feeItem) {
        this.index = 0;
        this.to = to;
        this.from = from;
        this.message = message;
        this.attachments = attachments;
        this.feeMoney = feeMoney;
        this.feeItem = feeItem;
        this.readFlags = new ArrayList<MailSender>();
    }

    /**
     * 指定されたファイルへ保存する
     * @param file 保存先
     */
    protected void save(File file) {

        YamlConfiguration config = new YamlConfiguration();

        ArrayList<String> toList = new ArrayList<String>();
        for ( MailSender t : to ) {
            toList.add(t.toString());
        }
        config.set("to", toList);
        config.set("from", from.toString());
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

        ArrayList<String> flagList = new ArrayList<String>();
        for ( MailSender t : readFlags ) {
            flagList.add(t.toString());
        }
        config.set("readFlags", flagList);

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

        data.to = new ArrayList<MailSender>();
        for ( String t : config.getStringList("to") ) {
            MailSender sender = MailSender.getMailSenderFromString(t);
            if ( sender != null ) {
                data.to.add(sender);
            }
        }
        data.from = MailSender.getMailSenderFromString(config.getString("from"));
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

        data.readFlags = new ArrayList<MailSender>();
        for ( String t : config.getStringList("readFlags") ) {
            MailSender sender = MailSender.getMailSenderFromString(t);
            if ( sender != null ) {
                data.readFlags.add(sender);
            }
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
        return new MailData(
                new ArrayList<MailSender>(to), from, message,
                new ArrayList<ItemStack>(attachments), feeMoney, feeItem);
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
     * @param to 宛先
     */
    public void setTo(List<MailSender> to) {
        this.to = to;
    }

    /**
     * このメールの宛先を設定します。
     * @param line 宛先番号（0から始まることに注意）
     * @param to 宛先
     */
    public void setTo(int line, MailSender to) {
        while ( this.to.size() <= line ) {
            this.to.add(to);
        }
        this.to.set(line, to);
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
     * このメールのメッセージの、指定された行番号を削除します。
     * @param line 宛先番号（0から始まることに注意）
     */
    public void deleteMessage(int line) {
        if ( this.message.size() > line ) {
            this.message.remove(line);
        }
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
     * このメールを読んだ人のリストを取得します。
     * @return 読んだ人のリスト
     */
    public List<MailSender> getReadFlags() {
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
     * 指定された名前のプレイヤーは、このメールの関係者かどうかを返す。
     * @param sender sender
     * @return 指定された名前がtoまたはfromに含まれるかどうか
     */
    public boolean isRelatedWith(MailSender sender) {
        return to.contains(sender) || from.equals(sender);
    }

    /**
     * このメールは編集中モードなのかどうかを返す
     * @return 編集中かどうか
     */
    public boolean isEditmode() {
        return (index == 0);
    }

    /**
     * メールの詳細情報を表示する
     * @param sender 表示するsender
     */
    protected void displayDescription(MailSender sender) {

        String num = (index == 0) ? Messages.get("Editmode") : index + "";
        String fdate = getFormattedDate(date);
        String pre = Messages.get("MailDetailLinePre");

        sender.sendMessage(Messages.get("MailDetailFirstLine", "%number", num));
        sender.sendMessage(Messages.get("MailDetailFromToLine",
                new String[]{"%from", "%to"},
                new String[]{from.getName(), join(to)}));
        sender.sendMessage(Messages.get("MailDetailDateLine", "%date", fdate));
        sender.sendMessage(Messages.get("MailDetailMessageLine"));
        for ( String m : message ) {
            sender.sendMessage(pre + "  " + ChatColor.WHITE + m);
        }

        if ( attachmentsOriginal != null && attachmentsOriginal.size() > 0 ) {

            if ( !(sender instanceof MailSenderPlayer) ) {
                sender.sendMessage(Messages.get("MailDetailAttachmentsLine"));
            } else {
                MessageComponent msg = new MessageComponent();
                msg.addText(Messages.get("MailDetailAttachmentsLine"));
                msg.addText(" ");
                MessageParts button = new MessageParts(
                        Messages.get("MailDetailAttachmentBox"), ChatColor.AQUA);
                button.setClickEvent(ClickEventType.RUN_COMMAND,
                        COMMAND + " attach " + index);
                msg.addParts(button);
                msg.send(sender);
            }
            for ( ItemStack i : attachmentsOriginal ) {
                sender.sendMessage(pre + "  " + ChatColor.WHITE + getItemDesc(i));
            }
        }

        sender.sendMessage(Messages.get("MailDetailLastLine"));
    }

    /**
     * このメールの編集画面を表示する
     * @param sender 表示するsender
     * @param config Undineのコンフィグ
     */
    protected void displayEditmode(MailSender sender, UndineConfig config) {

        // メッセージが3行に満たない場合は、この時点で空行を足しておく
        while ( message.size() < MESSAGE_ADD_SIZE ) {
            message.add("");
        }

        String pre = Messages.get("EditmodeLinePre");

        sender.sendMessage(Messages.get("EditmodeFirstLine"));

        for ( int i=0; i<to.size(); i++ ) {
            MessageComponent msg = new MessageComponent();
            msg.addText(pre);
            MessageParts buttonDelete = new MessageParts(
                    Messages.get("EditmodeToDelete"), ChatColor.AQUA);
            buttonDelete.setClickEvent(
                    ClickEventType.RUN_COMMAND,
                    COMMAND + " to delete " + (i+1));
            buttonDelete.setHoverText(Messages.get("EditmodeToDeleteToolTip"));
            msg.addParts(buttonDelete);
            msg.addText(" ");
            MessageParts button = new MessageParts(
                    Messages.get("EditmodeTo"), ChatColor.AQUA);
            button.setClickEvent(
                    ClickEventType.SUGGEST_COMMAND,
                    COMMAND + " to " + (i+1) + " " + to.get(i).getName());
            button.setHoverText(Messages.get("EditmodeToToolTip"));
            msg.addParts(button);
            msg.addText(" ");
            msg.addText(to.get(i).getName(), ChatColor.WHITE);
            msg.send(sender);
        }

        if ( to.size() < TO_MAX_SIZE ) {
            MessageComponent msg = new MessageComponent();
            msg.addText(pre);
            MessageParts button = new MessageParts(
                    Messages.get("EditmodeToAdd"), ChatColor.AQUA);
            button.setClickEvent(
                    ClickEventType.SUGGEST_COMMAND,
                    COMMAND + " to " + (to.size()+1) + " ");
            msg.addParts(button);
            msg.send(sender);
        }

        for ( int i=0; i<message.size(); i++ ) {
            MessageComponent msg = new MessageComponent();
            msg.addText(pre);
            MessageParts buttonDelete = new MessageParts(
                    Messages.get("EditmodeLineDelete"), ChatColor.AQUA);
            buttonDelete.setClickEvent(
                    ClickEventType.RUN_COMMAND,
                    COMMAND + " message delete " + (i+1));
            buttonDelete.setHoverText(Messages.get("EditmodeLineDeleteToolTip"));
            msg.addParts(buttonDelete);
            msg.addText(" ");
            MessageParts buttonEdit = new MessageParts(
                    "[" + (i+1) + ":]", ChatColor.AQUA);
            buttonEdit.setClickEvent(
                    ClickEventType.SUGGEST_COMMAND,
                    COMMAND + " message " + (i+1) + " " + message.get(i));
            buttonEdit.setHoverText(Messages.get("EditmodeLineEditToolTip"));
            msg.addParts(buttonEdit);
            msg.addText(" ");
            msg.addText(message.get(i), ChatColor.WHITE);
            msg.send(sender);
        }

        if ( message.size() < MESSAGE_MAX_SIZE ) {
            int num = message.size() + MESSAGE_ADD_SIZE;
            if ( num > MESSAGE_MAX_SIZE ) {
                num = MESSAGE_MAX_SIZE;
            }
            MessageComponent msg = new MessageComponent();
            msg.addText(pre);
            MessageParts button = new MessageParts(
                    Messages.get("EditmodeLineAdd"), ChatColor.AQUA);
            button.setClickEvent(
                    ClickEventType.RUN_COMMAND,
                    COMMAND + " message " + num);
            msg.addParts(button);
            msg.send(sender);
        }

        if ( config.isEnableAttachment() ) {
            MessageComponent msg = new MessageComponent();
            msg.addText(pre);
            MessageParts button = new MessageParts(
                    Messages.get("EditmodeAttach"), ChatColor.AQUA);
            button.setClickEvent(
                    ClickEventType.RUN_COMMAND,
                    COMMAND + " attach");
            msg.addParts(button);
            msg.addText(" ");
            msg.addText(Messages.get("EditmodeAttachNum", "%num", attachments.size()));
            msg.send(sender);
        }

        String lineParts = Messages.get("EditmodeLastLineParts");
        MessageComponent last = new MessageComponent();
        last.addText(lineParts);
        MessageParts sendButton = new MessageParts(
                Messages.get("EditmodeSend"), ChatColor.AQUA);
        sendButton.setClickEvent(ClickEventType.RUN_COMMAND, COMMAND + " send");
        last.addParts(sendButton);
        last.addText(lineParts);
        MessageParts cancelButton = new MessageParts(
                Messages.get("EditmodeCancel"), ChatColor.AQUA);
        cancelButton.setClickEvent(ClickEventType.RUN_COMMAND, COMMAND + " cancel");
        last.addParts(cancelButton);
        last.addText(lineParts);
        last.send(sender);
    }

    /**
     * このメールのInbox用サマリー文字列を返す
     * @return サマリー
     */
    protected String getInboxSummary() {

        String fdate = getFormattedDate(date);
        String summary = String.format("%s (%s) %s",
                from.getName(), fdate, message.get(0));

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
                join(to), fdate, message.get(0));

        // 長すぎる場合は切る
        if ( summary.length() > SUMMARY_MAX_SIZE ) {
            summary = summary.substring(0, SUMMARY_MAX_SIZE) + "...";
        }

        return summary;
    }

    /**
     * MailSenderのリストを、コンマを使ってつなげる
     * @param list MailSenderのリスト
     * @return 繋がった文字列
     */
    private String join(List<MailSender> list) {

        StringBuffer buffer = new StringBuffer();
        for ( MailSender item : list ) {
            if ( buffer.length() > 0 ) {
                buffer.append(",");
            }
            buffer.append(item.getName());
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
     * 言語リソース設定に従ってフォーマットされた日時の文字列を取得します。
     * @param date フォーマットする日時
     * @return フォーマットされた文字列
     */
    private String getFormattedDate(Date date) {
        return new SimpleDateFormat(Messages.get("DateFormat")).format(date);
    }
}
