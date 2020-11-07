package org.bitbucket.ucchy.undine.database;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bitbucket.ucchy.undine.MailData;
import org.bitbucket.ucchy.undine.MailManager;
import org.bitbucket.ucchy.undine.Messages;
import org.bitbucket.ucchy.undine.UndineMailer;
import org.bitbucket.ucchy.undine.Utility;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MailManagerDatabase extends MailManager {

    private final Database database;

    /**
     * コンストラクタ
     */
    public MailManagerDatabase(UndineMailer parent) {
        super(parent);
        this.database = parent.getDatabase();
    }

    /**
     * 逐一データベースからメールデータを取得するため、リロードしない。
     * 
     * @param sender 通知先。実際は使われない。
     */
    @Override
    protected void reload(final CommandSender sender) {
    }

    /**
     * メールデータがロード完了したかどうか。 
     * @return 常にtrue
     */
    @Override
    public boolean isLoaded() {
        return true;
    }

    /**
     * 指定されたインデクスのメールを取得する
     * 
     * @param index インデクス
     * @return メールデータ
     */
    @Override
    public MailDataDatabase getMail(int index) {
        if (database.mailDataTable.exists(index)) {
            return new MailDataDatabase(database, index, true);
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
        if (!(mail instanceof MailDataDatabase) || mail.isSent()) {
            return;
        }
        MailDataDatabase dbMail = (MailDataDatabase) mail;

        // メールデータの本文が1行も無いときは、ここで1行追加を行う。
        if (dbMail.getMessage().size() == 0) {
            dbMail.addMessage("");
        }

        // メールを送信する。
        dbMail.markAsSentMail(database.mailDataTable.newMail(dbMail.getIndex()));
        
        // 送信時間を設定する
        dbMail.setDate(new Date());

        // 送信地点を設定する
        dbMail.setLocation(dbMail.getFrom().getLocation());

        // オリジナルの添付ファイルを記録する
        dbMail.makeAttachmentsOriginal();

        // 添付が無いなら、着払い設定はクリアしておく
        if (dbMail.getAttachments().size() == 0) {
            dbMail.setCostMoney(0);
            dbMail.setCostItem(null);
        }

        // 着払いアイテムが設定されているなら、着払い料金はクリアしておく
        if (dbMail.getCostItem() != null) {
            dbMail.setCostMoney(0);
        }

        // 着払い料金が無効なら、着払い料金はクリアしておく
        if (!parent.getUndineConfig().isEnableCODMoney()) {
            dbMail.setCostMoney(0);
        }

        // 着払いアイテム無効なら、着払いアイテムはクリアしておく
        if (!parent.getUndineConfig().isEnableCODItem()) {
            dbMail.setCostItem(null);
        }

        // 宛先の人がログイン中なら知らせる
        String msg = Messages.get("InformationYouGotMail", "%from", dbMail.getFrom().getName());

        if (dbMail.isAllMail()) {
            for (Player player : Utility.getOnlinePlayers()) {
                player.sendMessage(msg);
                String pre = Messages.get("ListVerticalParts");
                sendMailLine(MailSender.getMailSender(player), pre, ChatColor.GOLD + dbMail.getInboxSummary(), dbMail);
            }
        } else {
            for (MailSender to : dbMail.getToTotal()) {
                if (to.isOnline()) {
                    to.sendMessage(msg);
                    String pre = Messages.get("ListVerticalParts");
                    sendMailLine(to, pre, ChatColor.GOLD + dbMail.getInboxSummary(), dbMail);
                }
            }
        }

        // 送った時刻を、メタデータに記録する
        long time = System.currentTimeMillis();
        dbMail.getFrom().setStringMetadata(SENDTIME_METAKEY, time + "");
    }

    /**
     * 受信したメールのリストを取得する
     * 
     * @param sender 取得する対象
     * @return メールのリスト
     */
    @Override
    public ArrayList<MailData> getInboxMails(MailSender sender) {
        ArrayList<MailData> box = new ArrayList<MailData>();
        int senderId = database.mailSenderTable.getId(sender);
        List<Integer> mails = database.mailDataTable.getIdsByRecipient(senderId);
        database.mailRecipientsTable.isTrashAll(mails, senderId).forEach((id, isTrash) -> {
            if (isTrash) {
                mails.remove(id);
            }
        });
        
        for (int id : mails) {
            box.add(new MailDataDatabase(database, id, true));
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
        ArrayList<MailData> box = new ArrayList<MailData>();
        int senderId = database.mailSenderTable.getId(sender);
        List<Integer> mails = database.mailDataTable.getIdsByRecipient(senderId);
        database.mailRecipientsTable.isReadAll(mails, senderId).forEach((id, isRead) -> {
            if (isRead) {
                mails.remove(id);
            }
        });
        
        for (int id : mails) {
            box.add(new MailDataDatabase(database, id, true));
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
        ArrayList<MailData> box = new ArrayList<MailData>();
        int senderId = database.mailSenderTable.getId(sender);
        List<Integer> mails = database.mailDataTable.getIdsBySenderId(senderId);
        database.mailRecipientsTable.isTrashAll(mails, senderId).forEach((id, isTrash) -> {
            if (isTrash) {
                mails.remove(id);
            }
        });
        for (int id : mails) {
            box.add(new MailDataDatabase(database, id, true));
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
        ArrayList<MailData> box = new ArrayList<MailData>();
        int senderId = database.mailSenderTable.getId(sender);
        List<Integer> mails = database.mailDataTable.getIdsByRecipient(senderId);
        mails.addAll(database.mailDataTable.getIdsBySenderId(senderId));
        database.mailRecipientsTable.isTrashAll(mails, senderId).forEach((id, isTrash) -> {
            if (isTrash) {
                mails.remove(id);
            }
        });
        database.mailRecipientsTable.isReadAll(mails, senderId).forEach((id, isRead) -> {
            if (!isRead) {
                mails.remove(id);
            }
        });

        for (int id : mails) {
            box.add(new MailDataDatabase(database, id, true));
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
        ArrayList<MailData> box = new ArrayList<MailData>();
        int senderId = database.mailSenderTable.getId(sender);
        List<Integer> mails = database.mailDataTable.getIdsByRecipient(senderId);
        mails.addAll(database.mailDataTable.getIdsBySenderId(senderId));
        database.mailRecipientsTable.isTrashAll(mails, senderId).forEach((id, isTrash) -> {
            if (!isTrash) {
                mails.remove(id);
            }
        });

        for (int id : mails) {
            box.add(new MailDataDatabase(database, id, true));
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
        // Do nothing. 常に同期されているため、不要。
    }

    /**
     * 指定されたインデクスのメールを削除する
     * 
     * @param index インデクス
     */
    @Override
    public void deleteMail(int index) {
        database.mailDataTable.removeMail(index);
    }

    /**
     * 古いメールを削除する
     */
    @Override
    protected void cleanup() {
        database.mailDataTable.removeMailsOlderThan(parent.getUndineConfig().getMailStorageTermDays());
    }

    /**
     * 編集中メールを作成して返す
     * 
     * @param sender 取得対象のsender
     * @return 編集中メール
     */
    @Override
    public MailData makeEditmodeMail(MailSender sender) {
        MailData mail = getEditmodeMail(sender);
        if (mail == null) {
            mail = new MailDataDatabase(
                database,
                database.draftMailDataTable.newMail(database.mailSenderTable.getId(sender)),
                false
            );
        }
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
        List<Integer> editmodeMails = database.draftMailDataTable.getIdsBySenderId(database.mailSenderTable.getId(sender));
        return editmodeMails.isEmpty() ? null : new MailDataDatabase(database, editmodeMails.get(0), false);
    }

    /**
     * 編集中メールを削除する
     * 
     * @param sender 削除対象のsender
     */
    @Override
    public void clearEditmodeMail(MailSender sender) {
        database.draftMailDataTable.removeMail(database.draftMailDataTable.getIdsBySenderId(database.mailSenderTable.getId(sender)));
    }

    /**
     * 編集中メールを保存する
     */
    @Override
    protected void storeEditmodeMail() {
        // Do nothing. 常時同期されているため、不要。
    }

    /**
     * editmails.ymlから編集中メールを復帰する
     */
    @Override
    protected void restoreEditmodeMail() {
        // Do nothing. 常時同期されているため、不要。
    }

    /**
     * 指定したsenderが使用中の添付ボックスの個数を返す
     * @param sender
     * @return 使用中添付ボックスの個数
     */
    @Override
    public int getAttachBoxUsageCount(MailSender sender) {
        return database.mailAttachmentBoxTable.getAttachBoxUsageCount(database.mailSenderTable.getId(sender));
    }
}
