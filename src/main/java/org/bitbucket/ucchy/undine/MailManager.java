/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bitbucket.ucchy.undine.tellraw.ClickEventType;
import org.bitbucket.ucchy.undine.tellraw.MessageComponent;
import org.bitbucket.ucchy.undine.tellraw.MessageParts;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * メールデータマネージャ
 * @author ucchy
 */
public class MailManager {

    private static final String COMMAND = UndineMailer.COMMAND;

    private static final int PAGE_SIZE = 10;

    private ArrayList<MailData> mails;
    private HashMap<String, MailData> editmodeMails;
    private int nextIndex;

    private UndineMailer parent;

    /**
     * コンストラクタ
     */
    public MailManager(UndineMailer parent) {
        this.parent = parent;
        reload();
        restoreEditmodeMail();
    }

    /**
     * メールデータを再読込する
     */
    public void reload() {

        mails = new ArrayList<MailData>();
        nextIndex = 1;

        File folder = parent.getMailFolder();
        File[] files = folder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".yml");
            }
        });

        for ( File file : files ) {
            MailData data = MailData.load(file);
            mails.add(data);

            if ( nextIndex <= data.getIndex() ) {
                nextIndex = data.getIndex() + 1;
            }
        }
    }

    /**
     * 指定されたインデクスのメールを取得する
     * @param index インデクス
     * @return メールデータ
     */
    public MailData getMail(int index) {

        for ( MailData m : mails ) {
            if ( m.getIndex() == index ) {
                return m;
            }
        }
        return null;
    }

    /**
     * 新しいテキストメールを送信する
     * @param from 送り元
     * @param to 宛先
     * @param message メッセージ
     */
    public void sendNewMail(MailSender from, MailSender to, String message) {

        ArrayList<MailSender> toList = new ArrayList<MailSender>();
        toList.add(to);
        ArrayList<String> messageList = new ArrayList<String>();
        messageList.add(message);
        MailData mail = new MailData(toList, from, messageList);
        sendNewMail(mail);
    }

    /**
     * 新しいテキストメールを送信する
     * @param from 送り元
     * @param to 宛先
     * @param message メッセージ
     */
    public void sendNewMail(MailSender from, List<MailSender> to, String message) {

        ArrayList<String> messageList = new ArrayList<String>();
        messageList.add(message);
        MailData mail = new MailData(to, from, messageList);
        sendNewMail(mail);
    }

    /**
     * 新しいテキストメールを送信する
     * @param from 送り元
     * @param to 宛先
     * @param message メッセージ
     */
    public void sendNewMail(MailSender from, List<MailSender> to, List<String> message) {

        MailData mail = new MailData(to, from, message);
        sendNewMail(mail);
    }

    /**
     * 新しいメールを送信する
     * @param mail メール
     */
    public void sendNewMail(MailData mail) {

        // 宛先の重複を削除して再設定する
        ArrayList<MailSender> to_copy = new ArrayList<MailSender>();
        for ( MailSender t : mail.getTo() ) {
            if ( !to_copy.contains(t) ) {
                to_copy.add(t);
            }
        }
        mail.setTo(to_copy);

        // インデクスを設定する
        mail.setIndex(nextIndex);
        nextIndex++;

        // 送信時間を設定する
        mail.setDate(new Date());

        // オリジナルの添付ファイルを記録する
        mail.makeAttachmentsOriginal();

        // 添付が無いなら、着払い設定はクリアしておく
        if ( mail.getAttachments().size() == 0 ) {
            mail.setCostMoney(0);
            mail.setCostItem(null);
        }

        // 着払いアイテムが設定されているなら、着払い料金はクリアしておく
        if ( mail.getCostItem() != null ) {
            mail.setCostMoney(0);
        }

        // 保存する
        mails.add(mail);
        saveMail(mail);

        // 宛先の人がログイン中なら知らせる
        String msg = Messages.get("InformationYouGotMail",
                "%from", mail.getFrom().getName());
        for ( MailSender to : mail.getTo() ) {
            if ( to.isOnline() ) {
                to.sendMessage(msg);
                String pre = Messages.get("InboxLinePre");
                sendMailLine(to, pre, ChatColor.GOLD + mail.getInboxSummary(), mail);
            }
        }

        // 送信したことを送信元に知らせる
        if ( mail.getFrom() != null && mail.getFrom().isOnline() ) {
            msg = Messages.get("InformationYouSentMail");
            mail.getFrom().sendMessage(msg);
        }
    }

    /**
     * 受信したメールのリストを取得する
     * @param sender 取得する対象
     * @return メールのリスト
     */
    public ArrayList<MailData> getInboxMails(MailSender sender) {

        ArrayList<MailData> box = new ArrayList<MailData>();
        for ( MailData mail : mails ) {
            if ( mail.getTo().contains(sender) ) {
                box.add(mail);
            }
        }
        sortNewer(box);
        return box;
    }

    /**
     * 受信したメールで未読のリストを取得する
     * @param sender 取得する対象
     * @return メールのリスト
     */
    public ArrayList<MailData> getUnreadMails(MailSender sender) {

        ArrayList<MailData> box = new ArrayList<MailData>();
        for ( MailData mail : mails ) {
            if ( mail.getTo().contains(sender) && !mail.isRead(sender) ) {
                box.add(mail);
            }
        }
        sortNewer(box);
        return box;
    }

    /**
     * 送信したメールのリストを取得する
     * @param sender 取得する対象
     * @return メールのリスト
     */
    public ArrayList<MailData> getOutboxMails(MailSender sender) {

        ArrayList<MailData> box = new ArrayList<MailData>();
        for ( MailData mail : mails ) {
            if ( mail.getFrom().equals(sender) ) {
                box.add(mail);
            }
        }
        sortNewer(box);
        return box;
    }

    /**
     * 指定されたメールを開いて確認する
     * @param sender 確認する対象
     * @param mail メール
     */
    public void displayMail(MailSender sender, MailData mail) {

        // 指定されたsenderの画面にメールを表示する
        mail.displayDescription(sender);

        // 添付ボックスがからっぽになっているなら、既読を付ける
        if ( mail.getAttachments().size() == 0 ) {
            mail.setReadFlag(sender);
            saveMail(mail);
        }
    }

    /**
     * 指定されたメールデータをUndineに保存する
     * @param mail メールデータ
     */
    public void saveMail(MailData mail) {

        // 編集中で未送信のメールは保存できません。
        if ( mail.getIndex() == 0 ) {
            return;
        }

        String filename = String.format("%1$08d.yml", mail.getIndex());
        File folder = parent.getMailFolder();
        File file = new File(folder, filename);
        mail.save(file);
    }

    /**
     * 編集中メールを作成して返す
     * @param sender 取得対象のsender
     * @return 編集中メール
     */
    public MailData makeEditmodeMail(MailSender sender) {
        String id = sender.toString();
        if ( editmodeMails.containsKey(id) ) {
            return editmodeMails.get(id);
        }
        MailData mail = new MailData();
        mail.setFrom(sender);
        editmodeMails.put(id, mail);
        return mail;
    }

    /**
     * 編集中メールを取得する
     * @param sender 取得対象のsender
     * @return 編集中メール（編集中でないならnull）
     */
    public MailData getEditmodeMail(MailSender sender) {
        String id = sender.toString();
        if ( editmodeMails.containsKey(id) ) {
            return editmodeMails.get(id);
        }
        return null;
    }

    /**
     * 編集中メールを削除する
     * @param sender 削除対象のsender
     */
    public void clearEditmodeMail(MailSender sender) {
        editmodeMails.remove(sender.toString());
    }

    /**
     * 指定されたメールを送信するのにかかる金額を返す
     * @param mail メール
     * @return 送信にかかる金額
     */
    protected int getSendFee(MailData mail) {
        if ( parent.getVaultEco() == null ) return 0;
        UndineConfig config = parent.getUndineConfig();
        if ( !config.isEnableSendFee() ) return 0;
        int total = 0;
        total += mail.getTo().size() * config.getSendFee();
        total += mail.getAttachments().size() * config.getAttachFee();
        return total;
    }

    /**
     * 指定されたsenderに、Inboxリストを表示する。
     * @param sender 表示対象のsender
     * @param page 表示するページ
     */
    protected void displayInboxList(MailSender sender, int page) {

        String pre = Messages.get("InboxLinePre");

        ArrayList<MailData> mails = getInboxMails(sender);
        int max = (int)((mails.size() - 1) / PAGE_SIZE) + 1;
        int unread = 0;
        for ( MailData m : mails ) {
            if ( !m.isRead(sender) ) {
                unread++;
            }
        }

        String fline = Messages.get("InboxFirstLine", "%unread", unread + "");
        sender.sendMessage(fline);

        for ( int i=0; i<10; i++ ) {

            int index = (page - 1) * 10 + i;
            if ( index < 0 || mails.size() <= index ) {
                continue;
            }

            MailData mail = mails.get(index);
            ChatColor color = mail.isRead(sender) ? ChatColor.GRAY : ChatColor.GOLD;

            sendMailLine(sender, pre, color + mail.getInboxSummary(), mail);
        }

        sendPager(sender, COMMAND + " inbox", page, max);
    }

    /**
     * 指定されたsenderに、Outboxリストを表示する。
     * @param sender 表示対象のsender
     * @param page 表示するページ
     */
    protected void displayOutboxList(MailSender sender, int page) {

        String pre = Messages.get("OutboxLinePre");

        ArrayList<MailData> mails = getOutboxMails(sender);
        int max = (int)((mails.size() - 1) / PAGE_SIZE) + 1;

        String fline = Messages.get("OutboxFirstLine",
                new String[]{"%page", "%max"},
                new String[]{page + "", max + ""});
        sender.sendMessage(fline);

        for ( int i=0; i<10; i++ ) {

            int index = (page - 1) * 10 + i;
            if ( index < 0 || mails.size() <= index ) {
                continue;
            }

            MailData mail = mails.get(index);
            ChatColor color = ChatColor.GRAY;

            sendMailLine(sender, pre, color + mail.getOutboxSummary(), mail);
        }

        sendPager(sender, COMMAND + " outbox", page, max);
    }

    /**
     * 指定されたsenderに、サーバー参加時の未読メール一覧を表示する。
     * @param sender 表示対象
     */
    protected void displayUnreadOnJoin(MailSender sender) {

        List<MailData> unread = getUnreadMails(sender);

        if ( unread.size() == 0 ) {
            return;
        }

        // 未読のメールを表示する
        sender.sendMessage(Messages.get(
                "InformationPlayerJoin", "%unread", unread.size()));

        // 最大5件まで、メールのサマリーを表示する
        String pre = Messages.get("InboxLinePre");
        for ( int i=0; i<5; i++ ) {
            if ( i >= unread.size() ) {
                break;
            }
            MailData mail = unread.get(i);
            sendMailLine(sender, pre, ChatColor.GOLD + mail.getInboxSummary(), mail);
        }
    }

    /**
     * 編集中メールをeditmails.ymlへ保存する
     */
    protected void storeEditmodeMail() {

        YamlConfiguration config = new YamlConfiguration();
        for ( String name : editmodeMails.keySet() ) {
            ConfigurationSection section = config.createSection(name);
            editmodeMails.get(name).saveToConfigSection(section);
        }

        try {
            File file = new File(parent.getDataFolder(), "editmails.yml");
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * editmails.ymlから編集中メールを復帰する
     */
    protected void restoreEditmodeMail() {

        editmodeMails = new HashMap<String, MailData>();

        File file = new File(parent.getDataFolder(), "editmails.yml");
        if ( !file.exists() ) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for ( String name : config.getKeys(false) ) {
            MailData mail = MailData.loadFromConfigSection(
                    config.getConfigurationSection(name));
            editmodeMails.put(name, mail);
        }

        // 復帰元ファイルを削除しておく
        file.delete();
    }

    /**
     * メールデータのリストを、新しいメール順に並び替えする
     * @param list リスト
     */
    private static void sortNewer(List<MailData> list) {
        Collections.sort(list, new Comparator<MailData>() {
            public int compare(MailData o1, MailData o2) {
                return o2.getDate().compareTo(o1.getDate());
            }
        });
    }

    /**
     * メールサマリー表示を対象プレイヤーに表示する
     * @param sender 表示対象
     * @param pre プレフィックス
     * @param summary サマリーの文字列
     * @param mail メールデータ
     */
    private void sendMailLine(
            MailSender sender, String pre, String summary, MailData mail) {

        MessageComponent msg = new MessageComponent();

        msg.addText(pre);

        MessageParts button = new MessageParts(
                "[" + mail.getIndex() + "]", ChatColor.AQUA);
        button.setClickEvent(
                ClickEventType.RUN_COMMAND, COMMAND + " read " + mail.getIndex());
        button.addHoverText(Messages.get("SummaryOpenThisMailToolTip"));
        msg.addParts(button);

        msg.addText((mail.getAttachments().size() > 0) ? "*" : " ");

        msg.addText(summary);

        msg.send(sender);
    }

    /**
     * ページャーを対象プレイヤーに表示する
     * @param sender 表示対象
     * @param commandPre コマンドのプレフィックス
     * @param page 現在のページ
     * @param max 最終ページ
     */
    private void sendPager(MailSender sender, String commandPre, int page, int max) {

        String firstLabel = Messages.get("FirstPage");
        String prevLabel = Messages.get("PrevPage");
        String nextLabel = Messages.get("NextPage");
        String lastLabel = Messages.get("LastPage");
        String firstToolTip = Messages.get("FirstPageToolTip");
        String prevToolTip = Messages.get("PrevPageToolTip");
        String nextToolTip = Messages.get("NextPageToolTip");
        String lastToolTip = Messages.get("LastPageToolTip");
        String parts = Messages.get("PagerParts");

        MessageComponent msg = new MessageComponent();

        msg.addText(parts + " ");

        if ( page > 1 ) {
            MessageParts firstButton = new MessageParts(
                    firstLabel, ChatColor.AQUA);
            firstButton.setClickEvent(ClickEventType.RUN_COMMAND, commandPre + " 1");
            firstButton.addHoverText(firstToolTip);
            msg.addParts(firstButton);

            msg.addText(" ");

            MessageParts prevButton = new MessageParts(
                    prevLabel, ChatColor.AQUA);
            prevButton.setClickEvent(ClickEventType.RUN_COMMAND, commandPre + " " + (page - 1));
            prevButton.addHoverText(prevToolTip);
            msg.addParts(prevButton);

        } else {
            msg.addText(firstLabel + " " + prevLabel, ChatColor.WHITE);

        }

        msg.addText(" (" + page + "/" + max + ") ");

        if ( page < max ) {
            MessageParts nextButton = new MessageParts(
                    nextLabel, ChatColor.AQUA);
            nextButton.setClickEvent(ClickEventType.RUN_COMMAND, commandPre + " " + (page + 1));
            nextButton.addHoverText(nextToolTip);
            msg.addParts(nextButton);

            msg.addText(" ");

            MessageParts lastButton = new MessageParts(
                    lastLabel, ChatColor.AQUA);
            lastButton.setClickEvent(ClickEventType.RUN_COMMAND, commandPre + " " + max);
            lastButton.addHoverText(lastToolTip);
            msg.addParts(lastButton);

        } else {
            msg.addText(nextLabel + " " + lastLabel, ChatColor.WHITE);
        }

        msg.addText(" " + parts);

        msg.send(sender);
    }
}
