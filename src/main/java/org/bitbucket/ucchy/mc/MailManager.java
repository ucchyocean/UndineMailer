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

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * メールデータマネージャ
 * @author ucchy
 */
public class MailManager {

    private static final int PAGE_SIZE = 10;

    private ArrayList<MailData> mails;
    private HashMap<Player, MailData> editmodeMails;
    private int nextIndex;

    private MailCraft parent;

    /**
     * コンストラクタ
     */
    public MailManager(MailCraft parent) {
        this.parent = parent;
        this.editmodeMails = new HashMap<Player, MailData>();
        reload();
    }

    /**
     * 全てのデータを再読込する
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
     * 指定されたメールデータを保存する
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
     * 編集中メールを取得する
     * @param player 取得対象のプレイヤー
     * @return 編集中メール
     */
    public MailData getEditmodeMail(Player player) {
        if ( editmodeMails.containsKey(player) ) {
            return editmodeMails.get(player);
        }
        MailData mail = new MailData();
        mail.setFrom(player.getName());
        editmodeMails.put(player, mail);
        return mail;
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
                String msg = String.format("%s %s[%d] %s",
                        pre, color.toString(), index, mail.getInboxSummary());
                sender.sendMessage(msg);
            } else {
                sendMailLineTellraw(player, pre, color + mail.getInboxSummary(), mail);
            }
        }

        // 対象者がプレイヤーなら、ページャーを表示する
        if ( player != null ) {
            String parts = Messages.get("PagerParts");

            String prevCommand = null;
            if ( page > 1 ) {
                prevCommand = "/mailcraft inbox " + (page - 1);
            }
            String nextCommand = null;
            if ( max > page ) {
                nextCommand = "/mailcraft inbox " + (page + 1);
            }
            sendPagerTellraw(player, parts, prevCommand, nextCommand);
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
                String msg = String.format("%s %s[%d] %s",
                        pre, color.toString(), index, mail.getOutboxSummary());
                sender.sendMessage(msg);
            } else {
                sendMailLineTellraw(player, pre, color + mail.getOutboxSummary(), mail);
            }
        }

        // 対象者がプレイヤーなら、ページャーを表示する
        if ( player != null ) {
            String parts = Messages.get("PagerParts");

            String prevCommand = null;
            if ( page > 1 ) {
                prevCommand = "/mailcraft outbox " + (page - 1);
            }
            String nextCommand = null;
            if ( max > page ) {
                nextCommand = "/mailcraft outbox " + (page + 1);
            }
            sendPagerTellraw(player, parts, prevCommand, nextCommand);
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

        StringBuffer buffer = new StringBuffer();
        buffer.append("tellraw ");
        buffer.append(player.getName() + " ");
        buffer.append("{\"text\":\"\",\"extra\":[");
        buffer.append("{\"text\":\"" + pre + "\"},");
        buffer.append("{\"text\":\"[" + mail.getIndex() + "]\","
                + "\"color\":\"blue\",\"underlined\":\"true\","
                + "\"clickEvent\":{\"action\":\"run_command\",\"value\":\""
                + "/mailcraft read " + mail.getIndex() + "\"}},");
        buffer.append("{\"text\":\"" + summary + "\"}");
        buffer.append("]}");

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), buffer.toString());
    }

    /**
     * tellrawを利用したページャーを、対象プレイヤーに表示する
     * @param player プレイヤー
     * @param parts パーツ
     * @param prevCommand 前のページボタンに割り当てるコマンド
     * @param nextCommand 次のページボタンに割り当てるコマンド
     */
    private void sendPagerTellraw(
            Player player, String parts, String prevCommand, String nextCommand) {

        String labelPrev = Messages.get("PrevPage");
        String labelNext = Messages.get("NextPage");

        StringBuffer buffer = new StringBuffer();
        buffer.append("tellraw ");
        buffer.append(player.getName() + " ");
        buffer.append("{\"text\":\"\",\"extra\":[");
        buffer.append("{\"text\":\"" + parts + "\"},");
        if ( prevCommand != null ) {
            buffer.append("{\"text\":\"" + labelPrev + "\","
                    + "\"color\":\"blue\",\"underlined\":\"true\","
                    + "\"clickEvent\":{\"action\":\"run_command\",\"value\":\""
                    + prevCommand + "\"}},");
        } else {
            buffer.append("{\"text\":\"" + labelPrev + "\"},");
        }
        buffer.append("{\"text\":\"" + parts + "\"},");
        if ( nextCommand != null ) {
            buffer.append("{\"text\":\"" + labelNext + "\","
                    + "\"color\":\"blue\",\"underlined\":\"true\","
                    + "\"clickEvent\":{\"action\":\"run_command\",\"value\":\""
                    + nextCommand + "\"}},");
        } else {
            buffer.append("{\"text\":\"" + labelNext + "\"},");
        }
        buffer.append("{\"text\":\"" + parts + "\"}");
        buffer.append("]}");

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), buffer.toString());
    }
}
