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
import java.util.Date;
import java.util.HashMap;

import org.bitbucket.ucchy.undine.group.GroupData;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * フラットファイルを用いたメールデータマネージャ
 * @author ucchy
 */
public class MailManagerFlatFile extends MailManager {

    private ArrayList<MailDataFlatFile> mails;
    private HashMap<String, MailDataFlatFile> editmodeMails;
    private int nextIndex;
    private boolean isLoaded;

    /**
     * コンストラクタ
     */
    public MailManagerFlatFile(UndineMailer parent) {
        super(parent);
    }

    /**
     * メールデータを再読込する
     * 
     * @param リロードが完了した時に、通知する先。通知が不要なら、nullでよい。
     */
    @Override
    protected void reload(final CommandSender sender) {

        final long start = System.currentTimeMillis();

        new BukkitRunnable() {
            public void run() {

                isLoaded = false;
                mails = new ArrayList<MailDataFlatFile>();
                nextIndex = 1;

                File folder = parent.getMailFolder();
                File[] files = folder.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".yml");
                    }
                });

                if (files != null) {
                    for (File file : files) {
                        MailDataFlatFile data = MailDataFlatFile.load(file);
                        mails.add(data);

                        if (nextIndex <= data.getIndex()) {
                            nextIndex = data.getIndex() + 1;
                        }
                    }
                }

                UndineMailer.getInstance().getLogger().info("Async load mail data... Done. Time: "
                        + (System.currentTimeMillis() - start) + "ms, Data: " + mails.size() + ".");

                long upgradeStart = System.currentTimeMillis();

                int total = 0;
                for (MailData mail : mails) {
                    if (mail.upgrade()) {
                        saveMail(mail);
                        total++;
                    }
                }

                if (total > 0) {
                    UndineMailer.getInstance().getLogger().info("Async upgrade mail data... Done.  Time: "
                            + (System.currentTimeMillis() - upgradeStart) + "ms, Data: " + total + ".");
                }

                isLoaded = true;

                if (sender != null) {
                    sender.sendMessage(Messages.get("InformationReload"));
                }
            }
        }.runTaskAsynchronously(UndineMailer.getInstance());
    }

    /**
     * メールデータがロード完了したかどうか。 UndineMailerは、保存されているメールデータをバックグラウンドで読み取ってロードするため、
     * ロードが完了していないうちは、メールリストの参照、メールの送信、リロードができないので 注意してください。
     * 
     * @return ロード完了したかどうか
     */
    @Override
    public boolean isLoaded() {
        return isLoaded;
    }

    /**
     * 指定されたインデクスのメールを取得する
     * 
     * @param index インデクス
     * @return メールデータ
     */
    @Override
    public MailData getMail(int index) {

        if (!isLoaded)
            return null;
        for (MailData m : mails) {
            if (m.getIndex() == index) {
                return m;
            }
        }
        return null;
    }

    /**
     * 新しいメールを送信する
     * 
     * @param mail メール
     */
    @Override
    public void sendNewMail(MailData mail) {
        if (!(mail instanceof MailDataFlatFile)) {
            return;
        }
        MailDataFlatFile fileMail = (MailDataFlatFile) mail;

        // メールデータの本文が1行も無いときは、ここで1行追加を行う。
        if (fileMail.getMessage().size() == 0) {
            fileMail.addMessage("");
        }

        // ロードが完了していないうちは、メールを送信できないようにする
        if (!isLoaded) {
            UndineMailer.getInstance().getLogger()
                    .warning("Because mailer has not yet been initialized, mailer dropped new mail.");
            UndineMailer.getInstance().getLogger().warning(fileMail.getInboxSummary());
            return;
        }

        // 統合宛先を設定する。
        ArrayList<MailSender> to_total = new ArrayList<MailSender>();
        for (MailSender t : fileMail.getTo()) {
            if (!to_total.contains(t)) {
                to_total.add(t);
            }
        }
        for (GroupData group : fileMail.getToGroupsConv()) {
            for (MailSender t : group.getMembers()) {
                if (!to_total.contains(t)) {
                    to_total.add(t);
                }
            }
        }
        fileMail.setToTotal(to_total);

        // インデクスを設定する
        fileMail.setIndex(nextIndex);
        nextIndex++;

        // 送信時間を設定する
        fileMail.setDate(new Date());

        // 送信地点を設定する
        fileMail.setLocation(fileMail.getFrom().getLocation());

        // オリジナルの添付ファイルを記録する
        fileMail.makeAttachmentsOriginal();

        // 添付が無いなら、着払い設定はクリアしておく
        if (fileMail.getAttachments().size() == 0) {
            fileMail.setCostMoney(0);
            fileMail.setCostItem(null);
        }

        // 着払いアイテムが設定されているなら、着払い料金はクリアしておく
        if (fileMail.getCostItem() != null) {
            fileMail.setCostMoney(0);
        }

        // 着払い料金が無効なら、着払い料金はクリアしておく
        if (!parent.getUndineConfig().isEnableCODMoney()) {
            fileMail.setCostMoney(0);
        }

        // 着払いアイテム無効なら、着払いアイテムはクリアしておく
        if (!parent.getUndineConfig().isEnableCODItem()) {
            fileMail.setCostItem(null);
        }

        // 編集中メールだったなら編集中ではなくしておく
        editmodeMails.values().remove(fileMail);

        // 保存する
        mails.add(fileMail);
        saveMail(fileMail);

        // 宛先の人がログイン中なら知らせる
        String msg = Messages.get("InformationYouGotMail", "%from", fileMail.getFrom().getName());

        if (fileMail.isAllMail()) {
            for (Player player : Utility.getOnlinePlayers()) {
                player.sendMessage(msg);
                String pre = Messages.get("ListVerticalParts");
                sendMailLine(MailSender.getMailSender(player), pre, ChatColor.GOLD + fileMail.getInboxSummary(), fileMail);
            }
        } else {
            for (MailSender to : fileMail.getToTotal()) {
                if (to.isOnline()) {
                    to.sendMessage(msg);
                    String pre = Messages.get("ListVerticalParts");
                    sendMailLine(to, pre, ChatColor.GOLD + fileMail.getInboxSummary(), fileMail);
                }
            }
        }

        // 送った時刻を、メタデータに記録する
        long time = System.currentTimeMillis();
        fileMail.getFrom().setStringMetadata(SENDTIME_METAKEY, time + "");
    }

    /**
     * 受信したメールのリストを取得する
     * 
     * @param sender 取得する対象
     * @return メールのリスト
     */
    @Override
    public ArrayList<MailData> getInboxMails(MailSender sender) {

        if (!isLoaded) {
            return null;
        }

        ArrayList<MailData> box = new ArrayList<MailData>();
        for (MailData mail : mails) {
            if (mail.isAllMail() || (mail.getToTotal() != null && mail.getToTotal().contains(sender))
                    || mail.getTo().contains(sender)) {
                if (!mail.isSetTrash(sender)) {
                    box.add(mail);
                }
            }
        }
        sortNewer(box);
        return box;
    }

    /**
     * 受信したメールで未読のリストを取得する
     * 
     * @param sender 取得する対象
     * @return メールのリスト
     */
    @Override
    public ArrayList<MailData> getUnreadMails(MailSender sender) {

        if (!isLoaded) {
            return null;
        }

        ArrayList<MailData> box = new ArrayList<MailData>();
        for (MailData mail : mails) {
            if (mail.isAllMail() || (mail.getToTotal() != null && mail.getToTotal().contains(sender))
                    || mail.getTo().contains(sender)) {
                if (!mail.isRead(sender) && !mail.isSetTrash(sender)) {
                    box.add(mail);
                }
            }
        }
        sortNewer(box);
        return box;
    }

    /**
     * 送信したメールのリストを取得する
     * 
     * @param sender 取得する対象
     * @return メールのリスト
     */
    @Override
    public ArrayList<MailData> getOutboxMails(MailSender sender) {

        if (!isLoaded) {
            return null;
        }

        ArrayList<MailData> box = new ArrayList<MailData>();
        for (MailData mail : mails) {
            if (mail.getFrom().equals(sender) && !mail.isSetTrash(sender)) {
                box.add(mail);
            }
        }
        sortNewer(box);
        return box;
    }

    /**
     * 関連メールのリストを取得する
     * 
     * @param sender 取得する対象
     * @return メールのリスト
     */
    @Override
    public ArrayList<MailData> getRelatedMails(MailSender sender) {

        if (!isLoaded) {
            return null;
        }

        ArrayList<MailData> box = new ArrayList<MailData>();
        for (MailData mail : mails) {
            if (mail.isRelatedWith(sender) && mail.isRead(sender) && !mail.isSetTrash(sender)) {
                box.add(mail);
            }
        }
        sortNewer(box);
        return box;
    }

    /**
     * ゴミ箱フォルダのメールリストを取得する
     * 
     * @param sender 取得する対象
     * @return メールのリスト
     */
    @Override
    public ArrayList<MailData> getTrashboxMails(MailSender sender) {

        if (!isLoaded) {
            return null;
        }

        ArrayList<MailData> box = new ArrayList<MailData>();
        for (MailData mail : mails) {
            if (mail.isRelatedWith(sender) && mail.isSetTrash(sender)) {
                box.add(mail);
            }
        }
        sortNewer(box);
        return box;
    }

    /**
     * 指定されたメールデータをUndineに保存する
     * 
     * @param mail メールデータ
     */
    @Override
    public void saveMail(MailData mail) {
        mail.save();
    }

    /**
     * 指定されたインデクスのメールを削除する
     * 
     * @param index インデクス
     */
    @Override
    public void deleteMail(int index) {

        if (isLoaded) {
            MailData mail = getMail(index);
            if (mail != null) {
                mails.remove(mail);
            }
        }

        String filename = String.format("%1$08d.yml", index);
        File folder = parent.getMailFolder();
        File file = new File(folder, filename);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 古いメールを削除する
     */
    @Override
    protected void cleanup() {

        if (!isLoaded) {
            return;
        }

        ArrayList<Integer> queue = new ArrayList<Integer>();
        int period = parent.getUndineConfig().getMailStorageTermDays();
        Date now = new Date();

        for (MailData mail : mails) {
            int days = (int) ((now.getTime() - mail.getDate().getTime()) / (1000 * 60 * 60 * 24));
            if (days > period) {
                queue.add(mail.getIndex());
            }
        }

        for (int index : queue) {
            deleteMail(index);
        }
    }

    /**
     * 編集中メールを作成して返す
     * 
     * @param sender 取得対象のsender
     * @return 編集中メール
     */
    @Override
    public MailData makeEditmodeMail(MailSender sender) {
        String id = sender.toString();
        if (editmodeMails.containsKey(id)) {
            return editmodeMails.get(id);
        }
        MailDataFlatFile mail = new MailDataFlatFile();
        mail.setFrom(sender);
        editmodeMails.put(id, mail);
        return mail;
    }

    /**
     * 編集中メールを取得する
     * 
     * @param sender 取得対象のsender
     * @return 編集中メール（編集中でないならnull）
     */
    @Override
    public MailData getEditmodeMail(MailSender sender) {
        String id = sender.toString();
        if (editmodeMails.containsKey(id)) {
            return editmodeMails.get(id);
        }
        return null;
    }

    /**
     * 編集中メールを削除する
     * 
     * @param sender 削除対象のsender
     */
    @Override
    public void clearEditmodeMail(MailSender sender) {
        editmodeMails.remove(sender.toString());
    }

    /**
     * 編集中メールをeditmails.ymlへ保存する
     */
    @Override
    protected void storeEditmodeMail() {

        YamlConfiguration config = new YamlConfiguration();
        for (String name : editmodeMails.keySet()) {
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
    @Override
    protected void restoreEditmodeMail() {

        editmodeMails = new HashMap<String, MailDataFlatFile>();

        File file = new File(parent.getDataFolder(), "editmails.yml");
        if (!file.exists())
            return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String name : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(name);
            if (section != null) {
                MailDataFlatFile mail = MailDataFlatFile.loadFromConfigSection(section);
                editmodeMails.put(name, mail);
            }
        }

        // 復帰元ファイルを削除しておく
        file.delete();
    }

    /**
     * 指定したsenderが使用中の添付ボックスの個数を返す
     * @param sender
     * @return 使用中添付ボックスの個数
     */
    @Override
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
}
