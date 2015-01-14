/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.mc;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.bitbucket.ucchy.mc.tellraw.ClickEventType;
import org.bitbucket.ucchy.mc.tellraw.MessageComponent;
import org.bitbucket.ucchy.mc.tellraw.MessageParts;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * メールデータマネージャ
 * @author ucchy
 */
public class MailManager {

    private static final String COMMAND = "/magicmail";
    private static final int PAGE_SIZE = 10;

    private ArrayList<MailData> mails;
    private HashMap<CommandSender, MailData> editmodeMails;
    private int nextIndex;

    private MagicMail parent;

    /**
     * コンストラクタ
     */
    public MailManager(MagicMail parent) {
        this.parent = parent;
        this.editmodeMails = new HashMap<CommandSender, MailData>();
        reload();
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
    public void sendNewMail(String from, String to, String message) {

        ArrayList<String> toList = new ArrayList<String>();
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
    public void sendNewMail(String from, List<String> to, String message) {

        ArrayList<String> messageList = new ArrayList<String>();
        messageList.add(message);
        MailData mail = new MailData(to, from, messageList);
        sendNewMail(mail);
    }

    /**
     * 新しいメールを送信する
     * @param mail メール
     */
    public void sendNewMail(MailData mail) {

        // 宛先の重複を削除して再設定する
        ArrayList<String> to_copy = new ArrayList<String>();
        for ( String t : mail.getTo() ) {
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
        mail.makeAttachmentsOriginal();

        // 保存する
        mails.add(mail);
        saveMail(mail);

        // 宛先の人がログイン中なら知らせる
        String msg = Messages.get("InformationYouGotMail", "%from", mail.getFrom());
        for ( String name : mail.getTo() ) {
            OfflinePlayer player = Utility.getOfflinePlayer(name);
            if ( player != null && player.isOnline() ) {
                player.getPlayer().sendMessage(msg);
            }
        }

        // 送信したことを送信元に知らせる
        OfflinePlayer player = Utility.getOfflinePlayer(mail.getFrom());
        if ( player != null && player.isOnline() ) {
            msg = Messages.get("InformationYouSentMail");
            player.getPlayer().sendMessage(msg);
        }
    }

    /**
     * 受信したメールのリストを取得する
     * @param name 取得するプレイヤー名
     * @return メールのリスト
     */
    public ArrayList<MailData> getInboxMails(String name) {

        ArrayList<MailData> box = new ArrayList<MailData>();
        for ( MailData mail : mails ) {
            if ( mail.getTo().contains(name) ) {
                box.add(mail);
            }
        }
        sortNewer(box);
        return box;
    }

    /**
     * 受信したメールで未読のメールのリストを取得する
     * @param name 取得するプレイヤー名
     * @return メールのリスト
     */
    public ArrayList<MailData> getUnreadMails(String name) {

        ArrayList<MailData> box = new ArrayList<MailData>();
        for ( MailData mail : mails ) {
            if ( mail.getTo().contains(name) && !mail.isRead(name) ) {
                box.add(mail);
            }
        }
        sortNewer(box);
        return box;
    }

    /**
     * 送信したメールのリストを取得する
     * @param name 取得するプレイヤー名
     * @return メールのリスト
     */
    public ArrayList<MailData> getOutboxMails(String name) {

        ArrayList<MailData> box = new ArrayList<MailData>();
        for ( MailData mail : mails ) {
            if ( mail.getFrom().equals(name) ) {
                box.add(mail);
            }
        }
        sortNewer(box);
        return box;
    }

    /**
     * 指定されたメールを開いて確認する
     * @param sender 確認する人
     * @param mail メール
     */
    public void displayMail(CommandSender sender, MailData mail) {

        // 指定されたsenderの画面にメールを表示する
        for ( String line : mail.getDescription() ) {
            sender.sendMessage(line);
        }

        // 宛先に該当する人なら、既読を付ける
        if ( mail.getTo().contains(sender.getName()) ) {
            mail.setReadFlag(sender.getName());
            saveMail(mail);
        }
    }

    /**
     * 指定されたメールの添付ボックスを開いて確認する
     * @param player 確認する人
     * @param mail メール
     */
    public void displayAttachBox(Player player, MailData mail) {
        parent.getBoxManager().displayAttachmentBox(player, mail);
    }

    /**
     * 指定されたメールデータをMagicMailに保存する
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
     * @return 編集中メール（編集中でないならnull）
     */
    public MailData makeEditmodeMail(CommandSender sender) {
        if ( editmodeMails.containsKey(sender) ) {
            return editmodeMails.get(sender);
        }
        MailData mail = new MailData();
        mail.setFrom(sender.getName());
        editmodeMails.put(sender, mail);
        return mail;
    }

    /**
     * 編集中メールを取得する
     * @param sender 取得対象のsender
     * @return 編集中メール（編集中でないならnull）
     */
    public MailData getEditmodeMail(CommandSender sender) {
        if ( editmodeMails.containsKey(sender) ) {
            return editmodeMails.get(sender);
        }
        return null;
    }

    /**
     * 編集中メールを削除する
     * @param player 削除対象のプレイヤー
     */
    public void clearEditmodeMail(Player player) {
        editmodeMails.remove(player);
    }

    /**
     * メールデータのリストを、新しいメール順に並び替えする
     * @param list リスト
     */
    private void sortNewer(List<MailData> list) {
        Collections.sort(list, new Comparator<MailData>() {
            public int compare(MailData o1, MailData o2) {
                return o2.getDate().compareTo(o1.getDate());
            }
        });
    }

    /**
     * 指定されたsenderに、Inboxリストを表示する。
     * @param sender 表示対象
     * @param page 表示するページ
     */
    protected void displayInboxList(CommandSender sender, int page) {

        String pre = Messages.get("InboxLinePre");
        Player player = (sender instanceof Player) ? (Player)sender : null;

        ArrayList<MailData> mails = getInboxMails(sender.getName());
        int max = (int)((mails.size() - 1) / PAGE_SIZE) + 1;
        int unread = 0;
        for ( MailData m : mails ) {
            if ( !m.isRead(sender.getName()) ) {
                unread++;
            }
        }

        String fline = Messages.get("InboxFirstLine",
                new String[]{"%page", "%max", "%unread"},
                new String[]{page + "", max + "", unread + ""});
        sender.sendMessage(fline);

        for ( int i=0; i<10; i++ ) {

            int index = (page - 1) * 10 + i;
            if ( index < 0 || mails.size() <= index ) {
                continue;
            }

            MailData mail = mails.get(index);
            ChatColor color = mail.isRead(sender.getName()) ? ChatColor.GRAY : ChatColor.GOLD;

            if ( player == null ) {
                String msg = String.format("%s %s[%d]%s",
                        pre, color.toString(), index, mail.getInboxSummary());
                sender.sendMessage(msg);
            } else {
                sendMailLineTellraw(player, pre, color + mail.getInboxSummary(), mail);
            }
        }

        // 対象者がプレイヤーなら、ページャーを表示する
        if ( player != null ) {
            sendPagerTellraw(player, COMMAND + " inbox", page, max);
        }
    }

    /**
     * 指定されたsenderに、Outboxリストを表示する。
     * @param sender 表示対象
     * @param page 表示するページ
     */
    protected void displayOutboxList(CommandSender sender, int page) {

        String pre = Messages.get("OutboxLinePre");
        Player player = (sender instanceof Player) ? (Player)sender : null;

        ArrayList<MailData> mails = getOutboxMails(sender.getName());
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

            if ( player == null ) {
                String msg = String.format("%s %s[%d]%s",
                        pre, color.toString(), index, mail.getOutboxSummary());
                sender.sendMessage(msg);
            } else {
                sendMailLineTellraw(player, pre, color + mail.getOutboxSummary(), mail);
            }
        }

        // 対象者がプレイヤーなら、ページャーを表示する
        if ( player != null ) {
            sendPagerTellraw(player, COMMAND + " outbox", page, max);
        }
    }

    /**
     * tellrawを利用したメールサマリー表示を、対象プレイヤーに表示する
     * @param player プレイヤー
     * @param pre プレフィックス
     * @param summary サマリーの文字列
     * @param mail メールデータ
     */
    private void sendMailLineTellraw(
            Player player, String pre, String summary, MailData mail) {

        MessageComponent msg = new MessageComponent();

        msg.addText(pre);

        MessageParts button = new MessageParts(
                "[" + mail.getIndex() + "]", ChatColor.BLUE, ChatColor.UNDERLINE);
        button.setClickEvent(
                ClickEventType.RUN_COMMAND, COMMAND + " read " + mail.getIndex());
        msg.addParts(button);

        msg.addText(summary);

        msg.sendCommand(player);
    }

    /**
     * tellrawを利用したページャーを、対象プレイヤーに表示する
     * @param player プレイヤー
     * @param commandPre コマンドのプレフィックス
     * @param page 現在のページ
     * @param max 最終ページ
     */
    private void sendPagerTellraw(Player player, String commandPre, int page, int max) {

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
                    firstLabel, ChatColor.BLUE, ChatColor.UNDERLINE);
            firstButton.setClickEvent(ClickEventType.RUN_COMMAND, commandPre + " 1");
            firstButton.setHoverText(firstToolTip);
            msg.addParts(firstButton);

            msg.addText(" ");

            MessageParts prevButton = new MessageParts(
                    prevLabel, ChatColor.BLUE, ChatColor.UNDERLINE);
            prevButton.setClickEvent(ClickEventType.RUN_COMMAND, commandPre + " " + (page - 1));
            prevButton.setHoverText(prevToolTip);
            msg.addParts(prevButton);

        } else {
            msg.addText(firstLabel + " " + prevLabel, ChatColor.WHITE);

        }

        msg.addText(" (" + page + "/" + max + ") ");

        if ( page < max ) {
            MessageParts nextButton = new MessageParts(
                    nextLabel, ChatColor.BLUE, ChatColor.UNDERLINE);
            nextButton.setClickEvent(ClickEventType.RUN_COMMAND, commandPre + " " + (page + 1));
            nextButton.setHoverText(nextToolTip);
            msg.addParts(nextButton);

            msg.addText(" ");

            MessageParts lastButton = new MessageParts(
                    lastLabel, ChatColor.BLUE, ChatColor.UNDERLINE);
            lastButton.setClickEvent(ClickEventType.RUN_COMMAND, commandPre + " " + max);
            lastButton.setHoverText(lastToolTip);
            msg.addParts(lastButton);

        } else {
            msg.addText(nextLabel + " " + lastLabel, ChatColor.WHITE);
        }

        msg.addText(" " + parts);

        msg.sendCommand(player);
    }
}
