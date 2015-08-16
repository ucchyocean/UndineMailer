/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.bitbucket.ucchy.undine.bridge.VaultEcoBridge;
import org.bitbucket.ucchy.undine.command.GroupCommand;
import org.bitbucket.ucchy.undine.command.ListCommand;
import org.bitbucket.ucchy.undine.command.UndineCommand;
import org.bitbucket.ucchy.undine.group.GroupData;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bitbucket.ucchy.undine.sender.MailSenderBlock;
import org.bitbucket.ucchy.undine.sender.MailSenderConsole;
import org.bitbucket.ucchy.undine.sender.MailSenderPlayer;
import org.bitbucket.ucchy.undine.tellraw.ClickEventType;
import org.bitbucket.ucchy.undine.tellraw.MessageComponent;
import org.bitbucket.ucchy.undine.tellraw.MessageParts;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * メールデータマネージャ
 * @author ucchy
 */
public class MailManager {

    protected static final String MAILLIST_METAKEY = "UndineMailList";
    public static final String SENDTIME_METAKEY = "MailSendTime";
    private static final String COMMAND = UndineCommand.COMMAND;

    private static final int PAGE_SIZE = 10;
    private static final int MESSAGE_ADD_SIZE = 3;

    private ArrayList<MailData> mails;
    private HashMap<String, MailData> editmodeMails;
    private int nextIndex;
    private boolean isLoaded;

    private UndineMailer parent;

    /**
     * コンストラクタ
     */
    public MailManager(UndineMailer parent) {
        this.parent = parent;
        restoreEditmodeMail();
        reload(null);
    }

    /**
     * メールデータを再読込する
     * @param リロードが完了した時に、通知する先。通知が不要なら、nullでよい。
     */
    protected void reload(final CommandSender sender) {

        final long start = System.currentTimeMillis();

        new BukkitRunnable() {
            public void run() {

                isLoaded = false;
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

                UndineMailer.getInstance().getLogger().info("Async load mail data... Done. Time: "
                        + (System.currentTimeMillis() - start) + "ms, Data: " + mails.size() + ".");

                long upgradeStart = System.currentTimeMillis();

                int total = 0;
                for ( MailData mail : mails ) {
                    if ( mail.upgrade() ) {
                        saveMail(mail);
                        total++;
                    }
                }

                if ( total > 0 ) {
                    UndineMailer.getInstance().getLogger().info("Async upgrade mail data... Done.  Time: "
                            + (System.currentTimeMillis() - upgradeStart) + "ms, Data: " + total + ".");
                }

                isLoaded = true;

                if ( sender != null ) {
                    sender.sendMessage(Messages.get("InformationReload"));
                }
            }
        }.runTaskAsynchronously(UndineMailer.getInstance());
    }

    /**
     * メールデータがロード完了したかどうか。
     * UndineMailerは、保存されているメールデータをバックグラウンドで読み取ってロードするため、
     * ロードが完了していないうちは、メールリストの参照、メールの送信、リロードができないので
     * 注意してください。
     * @return ロード完了したかどうか
     */
    public boolean isLoaded() {
        return isLoaded;
    }

    /**
     * 指定されたインデクスのメールを取得する
     * @param index インデクス
     * @return メールデータ
     */
    public MailData getMail(int index) {

        if ( !isLoaded ) return null;
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

        // メールデータの本文が1行も無いときは、ここで1行追加を行う。
        if ( mail.getMessage().size() == 0 ) {
            mail.addMessage("");
        }

        // ロードが完了していないうちは、メールを送信できないようにする
        if ( !isLoaded ) {
            UndineMailer.getInstance().getLogger().warning(
                    "Because mailer has not yet been initialized, mailer dropped new mail.");
            UndineMailer.getInstance().getLogger().warning(mail.getInboxSummary());
            return;
        }

        // 統合宛先を設定する。
        ArrayList<MailSender> to_total = new ArrayList<MailSender>();
        for ( MailSender t : mail.getTo() ) {
            if ( !to_total.contains(t) ) {
                to_total.add(t);
            }
        }
        for ( GroupData group : mail.getToGroupsConv() ) {
            for ( MailSender t : group.getMembers() ) {
                if ( !to_total.contains(t) ) {
                    to_total.add(t);
                }
            }
        }
        mail.setToTotal(to_total);

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

        // 着払い料金が無効なら、着払い料金はクリアしておく
        if ( !parent.getUndineConfig().isEnableCODMoney() ) {
            mail.setCostMoney(0);
        }

        // 着払いアイテム無効なら、着払いアイテムはクリアしておく
        if ( !parent.getUndineConfig().isEnableCODItem() ) {
            mail.setCostItem(null);
        }

        // 保存する
        mails.add(mail);
        saveMail(mail);

        // 宛先の人がログイン中なら知らせる
        String msg = Messages.get("InformationYouGotMail",
                "%from", mail.getFrom().getName());

        if ( mail.isAllMail() ) {
            for ( Player player : Utility.getOnlinePlayers() ) {
                player.sendMessage(msg);
                String pre = Messages.get("ListVerticalParts");
                sendMailLine(MailSender.getMailSender(player),
                        pre, ChatColor.GOLD + mail.getInboxSummary(), mail);
            }
        } else {
            for ( MailSender to : mail.getToTotal() ) {
                if ( to.isOnline() ) {
                    to.sendMessage(msg);
                    String pre = Messages.get("ListVerticalParts");
                    sendMailLine(to, pre, ChatColor.GOLD + mail.getInboxSummary(), mail);
                }
            }
        }

        // 送った時刻を、メタデータに記録する
        long time = System.currentTimeMillis();
        mail.getFrom().setStringMetadata(SENDTIME_METAKEY, time + "");
    }

    /**
     * 受信したメールのリストを取得する
     * @param sender 取得する対象
     * @return メールのリスト
     */
    public ArrayList<MailData> getInboxMails(MailSender sender) {

        if ( !isLoaded ) {
            return null;
        }

        ArrayList<MailData> box = new ArrayList<MailData>();
        for ( MailData mail : mails ) {
            if ( mail.isAllMail()
                    || (mail.getToTotal() != null && mail.getToTotal().contains(sender))
                    || mail.getTo().contains(sender) ) {
                if ( !mail.isSetTrash(sender) ) {
                    box.add(mail);
                }
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

        if ( !isLoaded ) {
            return null;
        }

        ArrayList<MailData> box = new ArrayList<MailData>();
        for ( MailData mail : mails ) {
            if ( mail.isAllMail()
                    || (mail.getToTotal() != null && mail.getToTotal().contains(sender))
                    || mail.getTo().contains(sender) ) {
                if ( !mail.isRead(sender) && !mail.isSetTrash(sender) ) {
                    box.add(mail);
                }
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

        if ( !isLoaded ) {
            return null;
        }

        ArrayList<MailData> box = new ArrayList<MailData>();
        for ( MailData mail : mails ) {
            if ( mail.getFrom().equals(sender) && !mail.isSetTrash(sender) ) {
                box.add(mail);
            }
        }
        sortNewer(box);
        return box;
    }

    /**
     * 関連メールのリストを取得する
     * @param sender 取得する対象
     * @return メールのリスト
     */
    public ArrayList<MailData> getRelatedMails(MailSender sender) {

        if ( !isLoaded ) {
            return null;
        }

        ArrayList<MailData> box = new ArrayList<MailData>();
        for ( MailData mail : mails ) {
            if ( mail.isRelatedWith(sender) && mail.isRead(sender)
                    && !mail.isSetTrash(sender) ) {
                box.add(mail);
            }
        }
        sortNewer(box);
        return box;
    }

    /**
     * ゴミ箱フォルダのメールリストを取得する
     * @param sender 取得する対象
     * @return メールのリスト
     */
    public ArrayList<MailData> getTrashboxMails(MailSender sender) {

        if ( !isLoaded ) {
            return null;
        }

        ArrayList<MailData> box = new ArrayList<MailData>();
        for ( MailData mail : mails ) {
            if ( mail.isRelatedWith(sender) && mail.isSetTrash(sender) ) {
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

        // ロード中の場合は、メールを表示できません
        if ( !isLoaded ) {
            return;
        }

        // 指定されたsenderの画面にメールを表示する
        displayMailDescription(sender, mail);

        // 添付ボックスがからっぽになっているか、キャンセルされているなら、既読を付ける
        if ( mail.getAttachments().size() == 0 || mail.isAttachmentsCancelled() ) {
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
     * 指定されたインデクスのメールを削除する
     * @param index インデクス
     */
    public void deleteMail(int index) {

        if ( isLoaded ) {
            MailData mail = getMail(index);
            if ( mail != null ) {
                mails.remove(mail);
            }
        }

        String filename = String.format("%1$08d.yml", index);
        File folder = parent.getMailFolder();
        File file = new File(folder, filename);
        if ( file.exists() ) {
            file.delete();
        }
    }

    /**
     * 古いメールを削除する
     */
    protected void cleanup() {

        if ( !isLoaded ) {
            return;
        }

        ArrayList<Integer> queue = new ArrayList<Integer>();
        int period = parent.getUndineConfig().getMailStorageTermDays();
        Date now = new Date();

        for ( MailData mail : mails ) {
            int days = (int)((now.getTime() - mail.getDate().getTime()) / (1000*60*60*24));
            if ( days > period ) {
                queue.add(mail.getIndex());
            }
        }

        for ( int index : queue ) {
            deleteMail(index);
        }
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
     * 指定されたsenderに、Inboxリストを表示する。
     * @param sender 表示対象のsender
     * @param page 表示するページ
     */
    public void displayInboxList(MailSender sender, int page) {

        // ロード中の場合は、リストを表示しないようにする
        if ( !isLoaded ) {
            return;
        }

        // 空行を挿入する
        for ( int i=0; i<parent.getUndineConfig().getUiEmptyLines(); i++ ) {
            sender.sendMessage("");
        }

        String parts = Messages.get("ListHorizontalParts");
        String pre = Messages.get("ListVerticalParts");

        ArrayList<MailData> mails = getInboxMails(sender);
        int max = (int)((mails.size() - 1) / PAGE_SIZE) + 1;
        int unread = 0;
        for ( MailData m : mails ) {
            if ( !m.isRead(sender) ) {
                unread++;
            }
        }

        String title = Messages.get("InboxTitle", "%unread", unread);
        sender.sendMessage(parts + parts + " " + title + " " + parts + parts);

        for ( int i=0; i<10; i++ ) {

            int index = (page - 1) * 10 + i;
            if ( index < 0 || mails.size() <= index ) {
                continue;
            }

            MailData mail = mails.get(index);
            ChatColor color = mail.isRead(sender) ? ChatColor.GRAY : ChatColor.GOLD;

            sendMailLine(sender, pre, color + mail.getInboxSummary(), mail);
        }

        sendPager(sender, UndineCommand.COMMAND + " inbox", page, max);

        // 表示した人にメタデータを設定する
        sender.setStringMetadata(MAILLIST_METAKEY, "inbox");
    }

    /**
     * 指定されたsenderに、Outboxリストを表示する。
     * @param sender 表示対象のsender
     * @param page 表示するページ
     */
    public void displayOutboxList(MailSender sender, int page) {

        // ロード中の場合は、リストを表示しないようにする
        if ( !isLoaded ) {
            return;
        }

        // 空行を挿入する
        for ( int i=0; i<parent.getUndineConfig().getUiEmptyLines(); i++ ) {
            sender.sendMessage("");
        }

        String parts = Messages.get("ListHorizontalParts");
        String pre = Messages.get("ListVerticalParts");

        ArrayList<MailData> mails = getOutboxMails(sender);
        int max = (int)((mails.size() - 1) / PAGE_SIZE) + 1;

        String title = Messages.get("OutboxTitle");
        sender.sendMessage(parts + parts + " " + title + " " + parts + parts);

        for ( int i=0; i<PAGE_SIZE; i++ ) {

            int index = (page - 1) * PAGE_SIZE + i;
            if ( index < 0 || mails.size() <= index ) {
                continue;
            }

            MailData mail = mails.get(index);
            ChatColor color = ChatColor.GRAY;

            sendMailLine(sender, pre, color + mail.getOutboxSummary(), mail);
        }

        sendPager(sender, UndineCommand.COMMAND + " outbox", page, max);

        // 表示した人にメタデータを設定する
        sender.setStringMetadata(MAILLIST_METAKEY, "outbox");
    }

    /**
     * 指定されたsenderに、サーバー参加時の未読メール一覧を表示する。
     * @param sender 表示対象
     */
    protected void displayUnreadOnJoin(MailSender sender) {

        // ロード中の場合は、リストを表示しないようにする
        if ( !isLoaded ) {
            return;
        }

        List<MailData> unread = getUnreadMails(sender);

        if ( unread.size() == 0 ) {
            return;
        }

        // 未読のメールを表示する
        sender.sendMessage(Messages.get(
                "InformationPlayerJoin", "%unread", unread.size()));

        // 最大5件まで、メールのサマリーを表示する
        String pre = Messages.get("ListVerticalParts");
        for ( int i=0; i<5; i++ ) {
            if ( i >= unread.size() ) {
                break;
            }
            MailData mail = unread.get(i);
            sendMailLine(sender, pre, ChatColor.GOLD + mail.getInboxSummary(), mail);
        }

        // 表示した人にメタデータを設定する
        sender.setStringMetadata(MAILLIST_METAKEY, "unread");
    }

    /**
     * 指定されたsenderに、Trashboxリストを表示する。
     * @param sender 表示対象のsender
     * @param page 表示するページ
     */
    public void displayTrashboxList(MailSender sender, int page) {

        // ロード中の場合は、リストを表示しないようにする
        if ( !isLoaded ) {
            return;
        }

        // 空行を挿入する
        for ( int i=0; i<parent.getUndineConfig().getUiEmptyLines(); i++ ) {
            sender.sendMessage("");
        }

        String parts = Messages.get("ListHorizontalParts");
        String pre = Messages.get("ListVerticalParts");

        ArrayList<MailData> mails = getTrashboxMails(sender);
        int max = (int)((mails.size() - 1) / PAGE_SIZE) + 1;

        String title = Messages.get("TrashboxTitle");
        sender.sendMessage(parts + parts + " " + title + " " + parts + parts);

        for ( int i=0; i<PAGE_SIZE; i++ ) {

            int index = (page - 1) * PAGE_SIZE + i;
            if ( index < 0 || mails.size() <= index ) {
                continue;
            }

            MailData mail = mails.get(index);
            ChatColor color = ChatColor.GRAY;

            sendMailLine(sender, pre, color + mail.getInboxSummary(), mail);
        }

        sendPager(sender, UndineCommand.COMMAND + " trash", page, max);

        // 表示した人にメタデータを設定する
        sender.setStringMetadata(MAILLIST_METAKEY, "trash");
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
     * 指定したsenderが使用中の添付ボックスの個数を返す
     * @param sender
     * @return 使用中添付ボックスの個数
     */
    public int getAttachBoxUsageCount(MailSender sender) {

        // ロード中の場合は、Integer最大値を返す
        if ( !isLoaded ) {
            return Integer.MAX_VALUE;
        }

        int count = 0;
        for ( MailData mail : mails ) {
            if ( mail.getFrom().equals(sender) && mail.getAttachments().size() > 0 ) {
                count++;
            }
        }
        return count;
    }

    /**
     * メールの詳細情報を表示する
     * @param sender 表示先
     * @param index 表示するメールのインデクス
     */
    public void displayMailDescription(MailSender sender, int index) {
        MailData mail = getMail(index);
        if ( mail == null ) return;
        displayMailDescription(sender, mail);
    }

    /**
     * メールの詳細情報を表示する
     * @param sender 表示先
     * @param mail 表示するメール
     */
    public void displayMailDescription(MailSender sender, MailData mail) {

        // 空行を挿入する
        int lines = UndineMailer.getInstance().getUndineConfig().getUiEmptyLines();
        for ( int i=0; i<lines; i++ ) {
            sender.sendMessage("");
        }

        String num = mail.isEditmode() ? Messages.get("Editmode") : mail.getIndex() + "";
        String fdate = mail.isEditmode() ? null : getFormattedDate(mail.getDate());

        String parts = Messages.get("DetailHorizontalParts");
        String pre = Messages.get("DetailVerticalParts");

        String title = Messages.get("MailDetailTitle", "%number", num);
        sender.sendMessage(parts + parts + " " + title + " " + parts + parts);

        String todesc = joinToAndGroup(mail);
        String tonext = "";
        if ( todesc.length() > 25 ) { // 宛先が長すぎる場合は、次の行に折り返す
            tonext = todesc.substring(25);
            todesc = todesc.substring(0, 25);
        }
        sender.sendMessage(pre + Messages.get("MailDetailFromToLine",
                new String[]{"%from", "%to"},
                new String[]{mail.getFrom().getName(), todesc}));
        if ( tonext.length() > 0 ) {
            sender.sendMessage(pre + "  " + ChatColor.WHITE + tonext);
        }

        if ( fdate != null ) {
            sender.sendMessage(pre + Messages.get("MailDetailDateLine", "%date", fdate));
        }
        sender.sendMessage(pre + Messages.get("MailDetailMessageLine"));
        for ( String m : mail.getMessage() ) {
            sender.sendMessage(pre + "  " + ChatColor.WHITE + Utility.replaceColorCode(m));
        }

        if ( mail.getAttachments().size() > 0 ) {

            MessageComponent msg = new MessageComponent();
            msg.addText(pre + Messages.get("MailDetailAttachmentsLine"));
            msg.addText(" ");

            if ( !mail.isEditmode() ) {

                if ( (!mail.isAttachmentsCancelled() && mail.isRecipient(sender))
                        || (mail.isAttachmentsCancelled() && mail.getFrom().equals(sender)) ) {
                    // 未キャンセルで受信者の場合、または、
                    // キャンセル済みで送信者の場合、オープンボタンを置く

                    MessageParts button = new MessageParts(
                            Messages.get("MailDetailAttachmentBox"), ChatColor.AQUA);
                    button.setClickEvent(ClickEventType.RUN_COMMAND,
                            COMMAND + " attach " + mail.getIndex());
                    msg.addParts(button);

                } else if ( !mail.isAttachmentsCancelled() && !mail.isAttachmentsOpened()
                        && mail.getFrom().equals(sender) ) {
                    // 未キャンセルで送信者の場合、
                    // 受信者がボックスを一度も開いていないなら、キャンセルボタンを置く

                    MessageParts button = new MessageParts(
                            Messages.get("MailDetailAttachmentBoxCancel"),
                            ChatColor.AQUA);
                    button.setClickEvent(ClickEventType.RUN_COMMAND,
                            COMMAND + " attach " + mail.getIndex() + " cancel");
                    button.addHoverText(Messages.get("MailDetailAttachmentBoxCancelToolTip"));
                    msg.addParts(button);

                } else if ( mail.isAttachmentsCancelled() && !mail.getFrom().equals(sender) ) {
                    // キャンセル済みで受信者の場合、キャンセルされた旨のラベルを出す

                    if ( mail.isAttachmentsRefused() ) {
                        msg.addText(Messages.get("MailDetailAttachmentBoxRefused"));
                        if ( mail.getAttachmentsRefusedReason() != null ) {
                            msg.addText("\n" + pre + "  " + ChatColor.WHITE
                                    + mail.getAttachmentsRefusedReason());
                        }
                    } else {
                        msg.addText(Messages.get("MailDetailAttachmentBoxCancelled"));
                    }
                }
            }

            msg.send(sender);

            for ( ItemStack i : mail.getAttachments() ) {
                sender.sendMessage(pre + "  " + ChatColor.WHITE + getItemDesc(i, true));
            }

            if ( mail.getCostMoney() > 0 || mail.getCostItem() != null ) {
                String costDesc = mail.getCostMoney() + "";
                VaultEcoBridge eco = UndineMailer.getInstance().getVaultEco();
                if ( eco != null ) {
                    costDesc = eco.format(mail.getCostMoney());
                }

                msg = new MessageComponent();
                if ( mail.getCostMoney() > 0 ) {
                    msg.addText(pre + Messages.get(
                            "MailDetailAttachCostMoneyLine", "%fee", costDesc));
                } else {
                    msg.addText(pre + Messages.get(
                            "MailDetailAttachCostItemLine", "%item",
                            getItemDesc(mail.getCostItem(), true)));
                }
                if ( mail.getTo().contains(sender) ) {
                    msg.addText(" ");
                    MessageParts refuseButton = new MessageParts(
                            Messages.get("MailDetailAttachmentBoxRefuse"),
                            ChatColor.AQUA);
                    refuseButton.setClickEvent(
                            ClickEventType.SUGGEST_COMMAND,
                            UndineCommand.COMMAND + " attach " + mail.getIndex() + " refuse ");
                    refuseButton.addHoverText(
                            Messages.get("MailDetailAttachmentBoxRefuseToolTip"));
                    msg.addParts(refuseButton);
                }
                msg.send(sender);
            }

        } else if ( mail.isAttachmentsCancelled() ) {
            // キャンセル済みの場合、キャンセルされた旨のラベルを出す

            if ( mail.isAttachmentsRefused() ) {
                sender.sendMessage(pre + Messages.get("MailDetailAttachmentsLine") + " "
                        + ChatColor.WHITE + Messages.get("MailDetailAttachmentBoxRefused"));
                if ( mail.getAttachmentsRefusedReason() != null ) {
                    sender.sendMessage(pre + "  " + ChatColor.WHITE
                            + mail.getAttachmentsRefusedReason());
                }
            } else {
                sender.sendMessage(pre + Messages.get("MailDetailAttachmentsLine") + " "
                        + ChatColor.WHITE + Messages.get("MailDetailAttachmentBoxCancelled"));
            }
        }

        if ( !mail.isEditmode() && mail.getAttachmentsOriginal() != null
                && mail.getAttachmentsOriginal().size() > 0 && mail.getFrom().equals(sender) ) {
            // 添付アイテムオリジナルがあり、表示先が送信者なら、元の添付アイテムを表示する。

            sender.sendMessage(pre + Messages.get("MailDetailAttachmentsOriginalLine"));

            for ( ItemStack i : mail.getAttachmentsOriginal() ) {
                sender.sendMessage(pre + "  " + ChatColor.WHITE + getItemDesc(i, true));
            }

        }

        if ( sender instanceof MailSenderPlayer
                && mail.isRelatedWith(sender) && !mail.isEditmode() ) {

            if ( mail.isSetTrash(sender) ) {
                // ゴミ箱に入っているメールなら、Restoreボタンを表示する

                MessageComponent msg = new MessageComponent();
                msg.addText(pre);

                MessageParts button = new MessageParts(
                        Messages.get("MailDetailTrashRestore"), ChatColor.AQUA);
                button.setClickEvent(ClickEventType.RUN_COMMAND,
                        COMMAND + " trash restore " + mail.getIndex());
                msg.addParts(button);

                msg.send(sender);

            } else {
                // 既に添付が1つもないメールなら、Deleteボタンを表示する
                // 開いているのが受信者なら、Replyボタンを表示する

                boolean attachNothing = (mail.getAttachments().size() == 0);
                boolean isRecipient = mail.isRecipient(sender)
                        && !(mail.getFrom() instanceof MailSenderConsole);

                if ( attachNothing || isRecipient ) {

                    MessageComponent msg = new MessageComponent();
                    msg.addText(pre);

                    if ( attachNothing ) {
                        MessageParts button = new MessageParts(
                                Messages.get("MailDetailTrash"), ChatColor.AQUA);
                        button.setClickEvent(ClickEventType.RUN_COMMAND,
                                COMMAND + " trash set " + mail.getIndex());
                        msg.addParts(button);
                    }

                    if ( attachNothing && isRecipient ) {
                        msg.addText(" ");
                    }

                    if ( isRecipient ) {
                        MessageParts button = new MessageParts(
                                Messages.get("MailDetailReply"), ChatColor.AQUA);
                        button.setClickEvent(ClickEventType.RUN_COMMAND,
                                COMMAND + " write " + mail.getFrom().getName());
                        msg.addParts(button);
                    }

                    msg.send(sender);
                }
            }
        }

        sendMailDescriptionPager(sender, mail.getIndex());
    }

    /**
     * メールの編集画面を表示する
     * @param sender 表示するsender
     */
    public void displayEditmode(MailSender sender) {

        // senderがコマブロなら何もしない
        if ( sender instanceof MailSenderBlock ) {
            return;
        }

        // 編集中メールの作成
        MailData mail = makeEditmodeMail(sender);

        // senderがコンソールなら、詳細表示画面にリダイレクトする
        if ( sender instanceof MailSenderConsole ) {
            displayMailDescription(sender, mail);
            return;
        }

        // メッセージが3行に満たない場合は、この時点で空行を足しておく
        while ( mail.getMessage().size() < MESSAGE_ADD_SIZE ) {
            mail.addMessage("");
        }

        // 空行を挿入する
        int lines = UndineMailer.getInstance().getUndineConfig().getUiEmptyLines();
        for ( int i=0; i<lines; i++ ) {
            sender.sendMessage("");
        }

        String parts = Messages.get("DetailHorizontalParts");
        String pre = Messages.get("DetailVerticalParts");

        String title = Messages.get("EditmodeTitle");
        sender.sendMessage(parts + parts + " " + title + " " + parts + parts);

        for ( int i=0; i<mail.getTo().size(); i++ ) {
            MessageComponent msg = new MessageComponent();
            msg.addText(pre);
            MessageParts buttonDelete = new MessageParts(
                    Messages.get("EditmodeToDelete"), ChatColor.AQUA);
            buttonDelete.setClickEvent(
                    ClickEventType.RUN_COMMAND,
                    COMMAND + " to delete " + (i+1));
            buttonDelete.addHoverText(Messages.get("EditmodeToDeleteToolTip"));
            msg.addParts(buttonDelete);
            msg.addText(" ");
            MessageParts button = new MessageParts(
                    Messages.get("EditmodeTo"), ChatColor.AQUA);
            button.setClickEvent(
                    ClickEventType.SUGGEST_COMMAND,
                    COMMAND + " to " + (i+1) + " " + mail.getTo().get(i).getName());
            button.addHoverText(Messages.get("EditmodeToToolTip"));
            msg.addParts(button);
            msg.addText(" ");
            msg.addText(mail.getTo().get(i).getName(), ChatColor.WHITE);
            msg.send(sender);
        }

        UndineConfig config = UndineMailer.getInstance().getUndineConfig();

        if ( mail.getTo().size() < config.getMaxDestination() ) {
            MessageComponent msg = new MessageComponent();
            msg.addText(pre);

            if ( !config.isEnablePlayerList() ) {
                MessageParts button = new MessageParts(
                        Messages.get("EditmodeToAdd"), ChatColor.AQUA);
                button.setClickEvent(
                        ClickEventType.SUGGEST_COMMAND,
                        COMMAND + " to " + (mail.getTo().size()+1) + " ");
                button.addHoverText(Messages.get("EditmodeToAddToolTip"));
                msg.addParts(button);

            } else {
                MessageParts buttonAddress = new MessageParts(
                        Messages.get("EditmodeToAddress"), ChatColor.AQUA);
                buttonAddress.setClickEvent(
                        ClickEventType.RUN_COMMAND,
                        ListCommand.COMMAND_INDEX + " " + COMMAND
                            + " to " + (mail.getTo().size()+1));
                msg.addParts(buttonAddress);

            }

            msg.send(sender);
        }

        for ( int i=0; i<mail.getToGroups().size(); i++ ) {
            GroupData group = parent.getGroupManager().getGroup(mail.getToGroups().get(i));

            MessageComponent msg = new MessageComponent();
            msg.addText(pre);
            MessageParts buttonDelete = new MessageParts(
                    Messages.get("EditmodeToDelete"), ChatColor.AQUA);
            buttonDelete.setClickEvent(
                    ClickEventType.RUN_COMMAND,
                    COMMAND + " to group delete " + (i+1));
            buttonDelete.addHoverText(Messages.get("EditmodeToDeleteToolTip"));
            msg.addParts(buttonDelete);
            msg.addText(" " + ChatColor.WHITE + Messages.get("EditmodeToGroup") + " ",
                    ChatColor.WHITE);
            MessageParts groupName = new MessageParts(mail.getToGroups().get(i), ChatColor.WHITE);
            if ( group != null ) {
                groupName.addHoverText(group.getHoverText());
            }
            msg.addParts(groupName);
            msg.send(sender);
        }

        if ( mail.getToGroups().size() < config.getMaxDestinationGroup() ) {
            MessageComponent msg = new MessageComponent();
            msg.addText(pre);
            MessageParts button = new MessageParts(
                    Messages.get("EditmodeToGroupAdd"), ChatColor.AQUA);
            button.setClickEvent(
                    ClickEventType.RUN_COMMAND,
                    GroupCommand.COMMAND + " list 1 "
                            + COMMAND + " to group " + (mail.getToGroups().size()+1));
            msg.addParts(button);

            msg.send(sender);
        }

        for ( int i=0; i<mail.getMessage().size(); i++ ) {
            MessageComponent msg = new MessageComponent();
            msg.addText(pre);
            MessageParts buttonDelete = new MessageParts(
                    Messages.get("EditmodeLineDelete"), ChatColor.AQUA);
            buttonDelete.setClickEvent(
                    ClickEventType.RUN_COMMAND,
                    COMMAND + " message delete " + (i+1));
            buttonDelete.addHoverText(Messages.get("EditmodeLineDeleteToolTip"));
            msg.addParts(buttonDelete);
            msg.addText(" ");
            MessageParts buttonEdit = new MessageParts(
                    "[M" + (i+1) + "]", ChatColor.AQUA);
            buttonEdit.setClickEvent(
                    ClickEventType.SUGGEST_COMMAND,
                    COMMAND + " message " + (i+1) + " " + mail.getMessage().get(i));
            buttonEdit.addHoverText(Messages.get("EditmodeLineEditToolTip"));
            msg.addParts(buttonEdit);
            msg.addText(" " + Utility.replaceColorCode(mail.getMessage().get(i)), ChatColor.WHITE);
            msg.send(sender);
        }

        if ( mail.getMessage().size() < MailData.MESSAGE_MAX_SIZE ) {
            int num = mail.getMessage().size() + MESSAGE_ADD_SIZE;
            if ( num > MailData.MESSAGE_MAX_SIZE ) {
                num = MailData.MESSAGE_MAX_SIZE;
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
            msg.addText(Messages.get("EditmodeAttachNum", "%num", mail.getAttachments().size()));
            msg.send(sender);

            boolean isEnableCODMoney = (UndineMailer.getInstance().getVaultEco() != null)
                    && config.isEnableCODMoney();
            boolean isEnableCODItem = config.isEnableCODItem();

            if ( mail.getAttachments().size() > 0 && (isEnableCODMoney || isEnableCODItem) ) {

                MessageComponent msgfee = new MessageComponent();
                msgfee.addText(pre);

                if ( mail.getCostMoney() == 0 && mail.getCostItem() == null ) {

                    if ( isEnableCODMoney ) {
                        MessageParts buttonFee = new MessageParts(
                                Messages.get("EditmodeCostMoney"), ChatColor.AQUA);
                        buttonFee.setClickEvent(
                                ClickEventType.SUGGEST_COMMAND, COMMAND + " costmoney ");
                        buttonFee.addHoverText(Messages.get("EditmodeCostMoneyToolTip"));
                        msgfee.addParts(buttonFee);
                    }

                    if ( isEnableCODMoney && isEnableCODItem ) {
                        msgfee.addText(" ");
                    }

                    if ( isEnableCODItem ) {
                        MessageParts buttonItem = new MessageParts(
                                Messages.get("EditmodeCostItem"), ChatColor.AQUA);
                        buttonItem.setClickEvent(
                                ClickEventType.SUGGEST_COMMAND, COMMAND + " costitem ");
                        buttonItem.addHoverText(Messages.get("EditmodeCostItemToolTip"));
                        msgfee.addParts(buttonItem);
                    }

                } else if ( mail.getCostMoney() > 0 ) {

                    String costDesc = mail.getCostMoney() + "";
                    VaultEcoBridge eco = UndineMailer.getInstance().getVaultEco();
                    if ( eco != null ) {
                        costDesc = eco.format(mail.getCostMoney());
                    }

                    MessageParts buttonDelete = new MessageParts(
                            Messages.get("EditmodeCostMoneyRemove"), ChatColor.AQUA);
                    buttonDelete.setClickEvent(
                            ClickEventType.RUN_COMMAND,
                            COMMAND + " costmoney 0");
                    buttonDelete.addHoverText(Messages.get("EditmodeCostMoneyRemoveToolTip"));
                    msgfee.addParts(buttonDelete);
                    MessageParts buttonFee = new MessageParts(
                            Messages.get("EditmodeCostMoneyData", "%fee", costDesc),
                            ChatColor.AQUA);
                    buttonFee.setClickEvent(
                            ClickEventType.SUGGEST_COMMAND,
                            COMMAND + " costmoney " + mail.getCostMoney());
                    msgfee.addParts(buttonFee);

                } else {

                    String desc = getItemDesc(mail.getCostItem(), true);

                    MessageParts buttonDelete = new MessageParts(
                            Messages.get("EditmodeCostItemRemove"), ChatColor.AQUA);
                    buttonDelete.setClickEvent(
                            ClickEventType.RUN_COMMAND,
                            COMMAND + " costitem remove");
                    buttonDelete.addHoverText(Messages.get("EditmodeCostItemRemoveToolTip"));
                    msgfee.addParts(buttonDelete);
                    MessageParts buttonItem = new MessageParts(
                            Messages.get("EditmodeCostItemData", "%item", desc),
                            ChatColor.AQUA);
                    buttonItem.setClickEvent(
                            ClickEventType.SUGGEST_COMMAND,
                            COMMAND + " costitem " + getItemDesc(mail.getCostItem(), false));
                    msgfee.addParts(buttonItem);

                }

                msgfee.send(sender);
            }
        }

        MessageComponent last = new MessageComponent();
        last.addText(parts);
        MessageParts sendButton = new MessageParts(
                Messages.get("EditmodeSend"), ChatColor.AQUA);
        sendButton.setClickEvent(ClickEventType.RUN_COMMAND, COMMAND + " send");
        last.addParts(sendButton);
        last.addText(parts);
        MessageParts cancelButton = new MessageParts(
                Messages.get("EditmodeCancel"), ChatColor.AQUA);
        cancelButton.setClickEvent(ClickEventType.RUN_COMMAND, COMMAND + " cancel");
        last.addParts(cancelButton);
        last.addText(parts);
        last.send(sender);
    }

    /**
     * メール詳細画面のページャーを対象プレイヤーに表示する
     * @param sender 表示対象
     * @param index 表示しようとしているメールのインデクス
     */
    private void sendMailDescriptionPager(MailSender sender, int index) {

        // メタデータが無いなら、ページャーを表示しない
        String meta = sender.getStringMetadata(MailManager.MAILLIST_METAKEY);
        if ( meta == null ||
                (!meta.equals("inbox") && !meta.equals("outbox")
                        && !meta.equals("unread") && !meta.equals("trash")) ) {
            sender.sendMessage(Messages.get("DetailLastLine"));
            return;
        }

        // リストの取得
        ArrayList<MailData> list;
        if ( meta.equals("inbox") ) {
            list = UndineMailer.getInstance().getMailManager().getInboxMails(sender);
        } else if ( meta.equals("outbox") ) {
            list = UndineMailer.getInstance().getMailManager().getOutboxMails(sender);
        } else if ( meta.equals("trash") ) {
            list = UndineMailer.getInstance().getMailManager().getTrashboxMails(sender);
        } else {
            list = UndineMailer.getInstance().getMailManager().getUnreadMails(sender);
        }

        // ページ番号の取得
        int page = getIndexOfMailList(index, list);

        // 該当のメールがリストに含まれていないなら、ページャーを表示しない
        if ( page == -1 ) {
            sender.sendMessage(Messages.get("DetailLastLine"));
            return;
        }

        String firstLabel = Messages.get("FirstPage");
        String prevLabel = Messages.get("PrevPage");
        String nextLabel = Messages.get("NextPage");
        String lastLabel = Messages.get("LastPage");
        String firstToolTip = Messages.get("FirstMailToolTip");
        String prevToolTip = Messages.get("PrevMailToolTip");
        String nextToolTip = Messages.get("NextMailToolTip");
        String lastToolTip = Messages.get("LastMailToolTip");
        String parts = Messages.get("DetailHorizontalParts");

        MessageComponent msg = new MessageComponent();

        msg.addText(parts + " ");

        if ( !meta.equals("unread") ) {
            String returnCommand;
            if ( meta.equals("outbox") ) {
                returnCommand = COMMAND + " outbox";
            } else if ( meta.equals("trash") ) {
                returnCommand = COMMAND + " trash";
            } else {
                returnCommand = COMMAND + " inbox";
            }
            MessageParts returnButton = new MessageParts(
                    Messages.get("Return"), ChatColor.AQUA);
            returnButton.setClickEvent(ClickEventType.RUN_COMMAND, returnCommand);
            returnButton.addHoverText(Messages.get("ReturnListToolTip"));
            msg.addParts(returnButton);

            msg.addText(" ");
        }

        if ( page > 0 ) {
            int first = list.get(0).getIndex();
            int prev = list.get(page - 1).getIndex();

            MessageParts firstButton = new MessageParts(
                    firstLabel, ChatColor.AQUA);
            firstButton.setClickEvent(ClickEventType.RUN_COMMAND,
                    COMMAND + " read " + first);
            firstButton.addHoverText(firstToolTip);
            msg.addParts(firstButton);

            msg.addText(" ");

            MessageParts prevButton = new MessageParts(
                    prevLabel, ChatColor.AQUA);
            prevButton.setClickEvent(ClickEventType.RUN_COMMAND,
                    COMMAND + " read " + prev);
            prevButton.addHoverText(prevToolTip);
            msg.addParts(prevButton);

        } else {
            msg.addText(firstLabel + " " + prevLabel, ChatColor.WHITE);

        }

        msg.addText(" (" + (page + 1) + "/" + list.size() + ") ");

        if ( page < (list.size() - 1) ) {
            int next = list.get(page + 1).getIndex();
            int last = list.get(list.size() - 1).getIndex();

            MessageParts nextButton = new MessageParts(
                    nextLabel, ChatColor.AQUA);
            nextButton.setClickEvent(ClickEventType.RUN_COMMAND,
                    COMMAND + " read " + next);
            nextButton.addHoverText(nextToolTip);
            msg.addParts(nextButton);

            msg.addText(" ");

            MessageParts lastButton = new MessageParts(
                    lastLabel, ChatColor.AQUA);
            lastButton.setClickEvent(ClickEventType.RUN_COMMAND,
                    COMMAND + " read " + last);
            lastButton.addHoverText(lastToolTip);
            msg.addParts(lastButton);

        } else {
            msg.addText(nextLabel + " " + lastLabel, ChatColor.WHITE);
        }

        msg.addText(" " + parts);

        msg.send(sender);
    }

    /**
     * 指定されたインデクスのメールが、リストの何番目にあるかを返す
     * @param index インデクス
     * @param list リスト
     * @return 何番目にあるか。含まれていないなら-1が返されることに注意
     */
    private int getIndexOfMailList(int index, ArrayList<MailData> list) {
        for ( int i=0; i<list.size(); i++ ) {
            if ( list.get(i).getIndex() == index ) {
                return i;
            }
        }
        return -1;
    }

    /**
     * アイテムを簡単な文字列表現にして返す
     * @param item アイテム
     * @param forDescription
     * @return 文字列表現
     */
    private String getItemDesc(ItemStack item, boolean forDescription) {
        String desc = item.getDurability() == 0 ? item.getType().toString() :
                item.getType().toString() + ":" + item.getDurability();
        if ( item.getAmount() == 1 ) return desc;
        if ( forDescription ) return desc + " * " + item.getAmount();
        return desc + " " + item.getAmount();
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
                ClickEventType.RUN_COMMAND,
                UndineCommand.COMMAND + " read " + mail.getIndex());
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
        String parts = Messages.get("ListHorizontalParts");

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

    /**
     * 言語リソース設定に従ってフォーマットされた日時の文字列を取得します。
     * @param date フォーマットする日時
     * @return フォーマットされた文字列
     */
    private String getFormattedDate(Date date) {
        return new SimpleDateFormat(Messages.get("DateFormat")).format(date);
    }

    /**
     * 宛先のリストを、コンマを使ってつなげる
     * @param mail メール
     * @return 繋がった文字列
     */
    private String joinToAndGroup(MailData mail) {

        StringBuffer buffer = new StringBuffer();
        for ( MailSender item : mail.getTo() ) {
            if ( buffer.length() > 0 ) {
                buffer.append(", ");
            }
            buffer.append(item.getName());
        }
        for ( String group : mail.getToGroups() ) {
            if ( buffer.length() > 0 ) {
                buffer.append(", ");
            }
            buffer.append(group);
        }
        return buffer.toString();
    }
}
