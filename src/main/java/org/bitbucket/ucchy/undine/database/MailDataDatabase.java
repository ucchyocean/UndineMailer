/*
 * @author     LazyGon
 * @license    LGPLv3
 * @copyright  Copyright OKOCRAFT 2020
 */
package org.bitbucket.ucchy.undine.database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.bitbucket.ucchy.undine.MailData;
import org.bitbucket.ucchy.undine.UndineMailer;
import org.bitbucket.ucchy.undine.group.SpecialGroupAll;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

/**
 * データベース管理のメールのデータ。
 * 
 * @author LazyGon
 */
public class MailDataDatabase extends MailData {

    private final Database database;

    private boolean isSent = false;

    /**
     * コンストラクタ
     * 
     * @param database
     */
    MailDataDatabase(Database database, int index, boolean isSent) {
        this.database = database;
        if (isSent) {
            markAsSentMail(index);
        } else {
            setIndex(index);
        }
    }

    /**
     * データベースへ保存する
     */
    @Override
    public void save() {
        // DO nothing. 常に同期されているため、不要。
    }

    @Override
    public boolean isSent() {
        return isSent;
    }

    public void markAsSentMail(int newIndex) {
        this.isSent = true;
        setIndex(newIndex);
    }

    /**
     * このオブジェクトの複製を作成して返す。
     * 
     * @see java.lang.Object#clone()
     */
    @Override
    public MailDataDatabase clone() {
        return new MailDataDatabase(database, index, isSent);
    }

    /**
     * 設定されている宛先を全て消去する
     */
    @Override
    public void deleteAllTo() {
        if (isSent()) {
            database.mailRecipientsTable.clearRecipient(index);
            database.mailRecipientGroupsTable.clearGroup(index);
        } else {
            database.draftMailRecipientsTable.clearRecipient(index);
            database.draftMailRecipientGroupsTable.clearGroup(index);
        }
    }

    /**
     * このメールの宛先を取得します。
     * 
     * @return 宛先
     */
    @Override
    public List<MailSender> getTo() {
        List<Integer> recipientIds;
        if (isSent()) {
            recipientIds = database.mailRecipientsTable.getRecipients(index);
        } else {
            recipientIds = database.draftMailRecipientsTable.getRecipients(index);
        }
        return new ArrayList<>(database.mailSenderTable.getByIds(recipientIds).values());
    }

    /**
     * このメールの宛先を設定します。宛先番号は常に名前の並び順に整理されます。
     * 
     * @param line 宛先番号（0から始まることに注意）
     * @param to   宛先
     */
    @Override
    public void setTo(int line, MailSender to) {
        if (isSent()) {
            ArrayList<MailSender> recipients = new ArrayList<>(
                    database.mailSenderTable.getByIds(database.mailRecipientsTable.getRecipients(index)).values());
            recipients.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
            if (recipients.size() > line) {
                database.mailRecipientsTable.removeRecipient(index,
                        database.mailSenderTable.getId(recipients.get(line)));
            }
            database.mailRecipientsTable.addRecipient(index, database.mailSenderTable.getId(to));

            // 全体グループを除去しておく。
            database.mailRecipientGroupsTable.removeGroup(index, SpecialGroupAll.NAME);
        } else {
            ArrayList<MailSender> recipients = new ArrayList<>(database.mailSenderTable
                    .getByIds(database.draftMailRecipientsTable.getRecipients(index)).values());
            recipients.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
            if (recipients.size() > line) {
                database.draftMailRecipientsTable.removeRecipient(index,
                        database.mailSenderTable.getId(recipients.get(line)));
            }
            database.draftMailRecipientsTable.addRecipient(index, database.mailSenderTable.getId(to));

            // 全体グループを除去しておく。
            database.draftMailRecipientGroupsTable.removeGroup(index, SpecialGroupAll.NAME);
        }
    }

    /**
     * このメールの宛先を追加します。
     * 
     * @param to 宛先
     */
    @Override
    public void addTo(MailSender to) {
        if (isSent()) {
            database.mailRecipientsTable.addRecipient(index, database.mailSenderTable.getId(to));

            // 全体グループは除去しておく。
            database.mailRecipientGroupsTable.removeGroup(index,
                    UndineMailer.getInstance().getGroupManager().getGroupAll().getName());
        } else {
            database.draftMailRecipientsTable.addRecipient(index, database.mailSenderTable.getId(to));

            // 全体グループは除去しておく。
            database.draftMailRecipientGroupsTable.removeGroup(index,
                    UndineMailer.getInstance().getGroupManager().getGroupAll().getName());
        }
    }

    /**
     * このメールの宛先を複数追加します。
     * 
     * @param to 宛先
     */
    @Override
    public void addTo(List<MailSender> to) {
        if (to.isEmpty()) {
            return;
        }
        if (isSent()) {
            database.mailRecipientsTable.addRecipients(index, database.mailSenderTable.getIds(to));

            // 全体グループは除去しておく。
            database.mailRecipientGroupsTable.removeGroup(index,
                    UndineMailer.getInstance().getGroupManager().getGroupAll().getName());
        } else {
            database.draftMailRecipientsTable.addRecipients(index, database.mailSenderTable.getIds(to));

            // 全体グループは除去しておく。
            database.draftMailRecipientGroupsTable.removeGroup(index,
                    UndineMailer.getInstance().getGroupManager().getGroupAll().getName());
        }
    }

    /**
     * このメールの指定された宛先を削除します。
     * 
     * @param line 宛先番号（0から始まることに注意）
     */
    @Override
    public void deleteTo(int line) {
        if (isSent()) {
            ArrayList<MailSender> recipients = new ArrayList<>(
                    database.mailSenderTable.getByIds(database.mailRecipientsTable.getRecipients(index)).values());
            recipients.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
            if (recipients.size() > line) {
                database.mailRecipientsTable.removeRecipient(index,
                        database.mailSenderTable.getId(recipients.get(line)));
            }
        } else {
            ArrayList<MailSender> recipients = new ArrayList<>(database.mailSenderTable
                    .getByIds(database.draftMailRecipientsTable.getRecipients(index)).values());
            recipients.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
            if (recipients.size() > line) {
                database.draftMailRecipientsTable.removeRecipient(index,
                        database.mailSenderTable.getId(recipients.get(line)));
            }
        }
    }

    /**
     * このメールの発信元を取得します。
     * 
     * @return 発信元
     */
    @Override
    public MailSender getFrom() {
        if (isSent()) {
            return database.mailDataTable.getSenderById(index);
        } else {
            return database.draftMailDataTable.getSenderById(index);
        }
    }

    /**
     * このメールの発信元を設定します。
     * 
     * @param from 発信元
     */
    @Override
    public void setFrom(MailSender from) {
        if (isSent()) {
            database.mailDataTable.setSender(index, database.mailSenderTable.getId(from));
        } else {
            database.draftMailDataTable.setSender(index, database.mailSenderTable.getId(from));
        }
    }

    /**
     * このメールのメッセージを取得します。
     * 
     * @return メッセージ
     */
    @Override
    public List<String> getMessage() {
        if (isSent()) {
            String message = database.mailDataTable.getMessage(index);
            return message == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(message.split("\r\n|\r|\n", -1)));
        } else {
            String message = database.draftMailDataTable.getMessage(index);
            return message == null ? new ArrayList<>() : new ArrayList<>(Arrays.asList(message.split("\r\n|\r|\n", -1)));
        }
    }

    /**
     * このメールのメッセージを設定します。
     * 
     * @param message メッセージ
     */
    @Override
    public void setMessage(List<String> message) {
        if (message.isEmpty()) {
            if (isSent()) {
                database.mailDataTable.setMessage(index, null);
            } else {
                database.draftMailDataTable.setMessage(index, null);
            }
            return;
        }

        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 0; i < message.size(); i++) {
            messageBuilder.append(message.get(i));
            if (i == message.size() - 1) {
                break;
            }
            messageBuilder.append("\n");
        }
        if (isSent()) {
            database.mailDataTable.setMessage(index, messageBuilder.toString());
        } else {
            database.draftMailDataTable.setMessage(index, messageBuilder.toString());
        }
    }

    /**
     * このメールのメッセージを設定します。
     * 
     * @param line    行番号（0から始まることに注意）
     * @param message メッセージ
     */
    @Override
    public void setMessage(int line, String message) {
        List<String> lines = getMessage();

        while (lines.size() <= line) {
            lines.add("");
        }
        lines.set(line, message);
        setMessage(lines);
    }

    /**
     * このメールのメッセージに、指定した内容を追加します。
     * 
     * @param message メッセージ
     */
    @Override
    public void addMessage(String message) {
        List<String> messages = getMessage();
        String[] lines = message.split("\r\n|\r|\n", -1);
        for (String line : lines) {
            messages.add(line);
        }
        setMessage(messages);
    }

    /**
     * このメールのメッセージの、指定された行番号を削除します。
     * 
     * @param line 宛先番号（0から始まることに注意）
     */
    @Override
    public void deleteMessage(int line) {
        List<String> message = getMessage();
        if (message.size() > line && line >= 0) {
            message.remove(line);
        }
        setMessage(message);
    }

    /**
     * 宛先グループを取得します。
     * 
     * @return 宛先グループ
     */
    @Override
    public List<String> getToGroups() {
        if (isSent()) {
            return database.mailRecipientGroupsTable.getGroups(index);
        } else {
            return database.draftMailRecipientGroupsTable.getGroups(index);
        }
    }

    /**
     * このメールの宛先グループを設定します。
     * 
     * @param line  宛先番号（0から始まることに注意）
     * @param group グループ
     */
    @Override
    public void setToGroup(int line, String group) {
        if (isSent()) {
            // 追加するグループが全体グループなら、
            // 他の宛先を全て削除する
            if (SpecialGroupAll.NAME.equals(group)) {
                database.mailRecipientsTable.clearRecipient(index);
                database.mailRecipientGroupsTable.clearGroup(index);
                database.mailRecipientGroupsTable.addGroup(index, group);
                return;
            }

            List<String> groups = database.mailRecipientGroupsTable.getGroups(index);
            groups.sort(String::compareToIgnoreCase);
            if (groups.size() <= line) {
                database.mailRecipientGroupsTable.addGroup(index, group);
            } else if (groups.size() > line) {
                database.mailRecipientGroupsTable.removeGroup(index, groups.get(line));
                database.mailRecipientGroupsTable.addGroup(index, group);
            }

            // 全体グループが含まれていたなら、全体グループを除去する
            if (groups.contains(SpecialGroupAll.NAME)) {
                database.mailRecipientGroupsTable.removeGroup(index, SpecialGroupAll.NAME);
            }
        } else {
            // 追加するグループが全体グループなら、
            // 他の宛先を全て削除する
            if (SpecialGroupAll.NAME.equals(group)) {
                database.draftMailRecipientsTable.clearRecipient(index);
                database.draftMailRecipientGroupsTable.clearGroup(index);
                database.draftMailRecipientGroupsTable.addGroup(index, group);
                return;
            }

            List<String> groups = database.draftMailRecipientGroupsTable.getGroups(index);
            groups.sort(String::compareToIgnoreCase);
            if (groups.size() <= line) {
                database.draftMailRecipientGroupsTable.addGroup(index, group);
            } else if (groups.size() > line) {
                database.draftMailRecipientGroupsTable.removeGroup(index, groups.get(line));
                database.draftMailRecipientGroupsTable.addGroup(index, group);
            }

            // 全体グループが含まれていたなら、全体グループを除去する
            if (groups.contains(SpecialGroupAll.NAME)) {
                database.draftMailRecipientGroupsTable.removeGroup(index, SpecialGroupAll.NAME);
            }

        }
    }

    /**
     * このメールの宛先グループの、指定された行番号を削除します。
     * 
     * @param line 宛先番号（0から始まることに注意）
     */
    @Override
    public void deleteToGroup(int line) {
        if (isSent()) {
            List<String> groups = database.mailRecipientGroupsTable.getGroups(index);
            groups.sort(String::compareToIgnoreCase);
            if (groups.size() > line && line >= 0) {
                String removal = groups.get(line);
                database.mailRecipientGroupsTable.removeGroup(index, removal);
            }
        } else {
            List<String> groups = database.draftMailRecipientGroupsTable.getGroups(index);
            groups.sort(String::compareToIgnoreCase);
            if (groups.size() > line && line >= 0) {
                String removal = groups.get(line);
                database.draftMailRecipientGroupsTable.removeGroup(index, removal);
            }
        }
    }

    /**
     * 統合宛先を設定する
     * 
     * @param total 統合宛先
     */
    @Override
    protected void setToTotal(List<MailSender> total) {
        // DO noting. 統合宛先は常にtoGroupとtoから計算される。
    }

    /**
     * 統合宛先（宛先＋宛先グループの和集合）を取得する。
     * 
     * @return 統合宛先
     */
    @Override
    public List<MailSender> getToTotal() {
        List<MailSender> toTotal = new ArrayList<>(
                database.mailSenderTable.getByIds(database.groupMembersTable.getMemberIdsOf(getToGroups())).values());
        toTotal.addAll(getTo());
        return toTotal;
    }

    /**
     * このメールに添付されたアイテムを取得します。
     * 
     * @return 添付アイテム
     */
    @Override
    public List<ItemStack> getAttachments() {
        if (isSent()) {
            return database.mailAttachmentBoxTable.getAttachmentBoxOf(index);
        } else {
            return database.draftMailAttachmentBoxTable.getAttachmentBoxOf(index);
        }
    }

    /**
     * このメールの添付アイテムを設定します。
     * 
     * @param attachments 添付アイテム
     */
    @Override
    public void setAttachments(List<ItemStack> attachments) {
        if (isSent()) {
            database.mailAttachmentBoxTable.setAttachmentBox(index, attachments);
        } else {
            database.draftMailAttachmentBoxTable.setAttachmentBox(index, attachments);
        }
    }

    /**
     * 指定されたアイテムを添付アイテムに追加します。
     * 
     * @param item アイテム
     */
    @Override
    public void addAttachment(ItemStack item) {
        List<ItemStack> attachments = getAttachments();
        attachments.add(item);
        setAttachments(attachments);
    }

    /**
     * このメールを読んだ人のリストを取得します。
     * 
     * @return 読んだ人のリスト
     */
    @Override
    public List<MailSender> getReadFlags() {
        if (!isSent()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(
                database.mailSenderTable.getByIds(database.mailRecipientsTable.getWhoRead(index)).values());
    }

    /**
     * このメールに削除フラグを付けた人のリストを取得します。
     * 
     * @return 削除フラグをつけている人のリスト
     */
    @Override
    public List<MailSender> getTrashFlags() {
        if (!isSent()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(
                database.mailSenderTable.getByIds(database.mailRecipientsTable.getWhoTrash(index)).values());
    }

    /**
     * このメールの添付アイテムを受け取るのに必要な金額を取得します。
     * 
     * @return 受け取り金額
     */
    @Override
    public double getCostMoney() {
        if (isSent()) {
            return database.mailDataTable.getCostMoney(index);
        } else {
            return database.draftMailDataTable.getCostMoney(index);
        }
    }

    /**
     * このメールの添付アイテムを受け取るのに必要な金額を設定します。
     * 
     * @param feeMoney 受け取り金額
     */
    @Override
    public void setCostMoney(double feeMoney) {
        if (isSent()) {
            database.mailDataTable.setCostMoney(index, feeMoney);
        } else {
            database.draftMailDataTable.setCostMoney(index, feeMoney);
        }
    }

    /**
     * このメールの添付アイテムを受け取るのに必要な引き換えアイテムを取得します。
     * 
     * @return 引き換えアイテム
     */
    @Override
    public ItemStack getCostItem() {
        if (!isSent()) {
            return database.mailDataTable.getCostItem(index);
        } else {
            return database.draftMailDataTable.getCostItem(index);
        }
    }

    /**
     * このメールの添付アイテムを受け取るのに必要な引き換えアイテムを設定します。
     * 
     * @param feeItem 引き換えアイテム
     */
    @Override
    public void setCostItem(ItemStack feeItem) {
        if (!isSent()) {
            database.mailDataTable.setCostItem(index, feeItem);
        } else {
            database.draftMailDataTable.setCostItem(index, feeItem);
        }
    }

    /**
     * このメールの送信時間を取得します。
     * 
     * @return 送信時間
     */
    @Override
    public Date getDate() {
        if (isSent()) {
            return database.mailDataTable.getDate(index);
        }
        return null;
    }

    /**
     * このメールの送信時間を設定します（メール送信時に自動で割り当てられます）。
     * 
     * @param date 送信時間
     */
    @Override
    protected void setDate(Date date) {
        if (isSent()) {
            database.mailDataTable.setDate(index, date);
        }
    }

    /**
     * このメールが送信された地点を取得します。
     * 
     * @return 送信地点
     */
    @Override
    public Location getLocation() {
        if (isSent()) {
            return database.mailDataTable.getLocation(index);
        }
        return null;
    }

    /**
     * このメールの送信地点を設定します（メール送信時に自動で割り当てられます）。
     * 
     * @param location 送信地点
     */
    @Override
    public void setLocation(Location location) {
        if (isSent()) {
            database.mailDataTable.setLocation(index, location);
        }
    }

    /**
     * メール送信時の添付アイテムを取得します。
     * 
     * @return メール送信時の添付アイテム
     */
    @Override
    public List<ItemStack> getAttachmentsOriginal() {
        if (isSent()) {
            return database.mailAttachmentBoxSnapshotTable.getAttachmentBoxOf(index);
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * attachmentOriginalに、添付ファイルのコピーを行います （メール送信時に自動で行われます）。
     */
    @Override
    protected void makeAttachmentsOriginal() {
        database.mailAttachmentBoxSnapshotTable.setAttachmentBox(index, getAttachments());
    }

    /**
     * 指定したプレイヤーが、このメールを読んだかどうかを返します。
     * 
     * @param sender プレイヤー
     * @return 読んだかどうか
     */
    @Override
    public boolean isRead(MailSender sender) {
        if (!isSent()) {
            return false;
        }
        return database.mailRecipientsTable.isRead(index, database.mailSenderTable.getId(sender));
    }

    /**
     * 指定した名前のsenderの既読マークを付ける
     * 
     * @param sender sender
     */
    @Override
    public void setReadFlag(MailSender sender) {
        if (isSent()) {
            database.mailRecipientsTable.setRead(index, database.mailSenderTable.getId(sender), true);
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
        if (isSent()) {
            return database.mailRecipientsTable.isTrash(index, database.mailSenderTable.getId(sender));
        }
        return false;
    }

    /**
     * 指定した人の削除マークを付ける
     * 
     * @param sender
     */
    @Override
    public void setTrashFlag(MailSender sender) {
        if (isSent()) {
            database.mailRecipientsTable.setTrash(index, database.mailSenderTable.getId(sender), true);
        }
    }

    /**
     * 指定した人の削除マークを消す
     * 
     * @param sender
     */
    @Override
    public void removeTrashFlag(MailSender sender) {
        if (isSent()) {
            database.mailRecipientsTable.setTrash(index, database.mailSenderTable.getId(sender), false);
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
        return getFrom().equals(sender) || isAllMail() || getToTotal().contains(sender);
    }

    /**
     * 指定された名前のプレイヤーは、このメールの受信者かどうかを返す。
     * 
     * @param sender sender
     * @return 指定された名前がtoに含まれるかどうか
     */
    @Override
    public boolean isRecipient(MailSender sender) {
        return isAllMail() || getToTotal().contains(sender);
    }

    /**
     * このメールの添付アイテムがオープンされたのかどうかを返す
     * 
     * @return 添付アイテムがオープンされたのかどうか
     */
    @Override
    public boolean isAttachmentsOpened() {
        return database.mailDataTable.isAttachmentOpened(index);
    }

    /**
     * このメールの添付アイテムをオープンされたとして記録する。 受信者が添付ボックスを一度でも開いた事がある状態なら、
     * 送信者は添付アイテムをキャンセルすることができなくなる。
     */
    @Override
    public void setOpenAttachments() {
        database.mailDataTable.setAttachmentOpened(index, true);
    }

    /**
     * このメールの添付アイテムがキャンセルされたのかどうかを返す
     * 
     * @return 添付アイテムがキャンセルされたのかどうか
     */
    @Override
    public boolean isAttachmentsCancelled() {
        return database.mailDataTable.isAttachmentCancelled(index);
    }

    /**
     * このメールの添付アイテムをキャンセルする。 添付アイテムがキャンセルされると、受信者はボックスを開けなくなり、
     * 逆に送信者がボックスを開くことができるようになる。
     */
    @Override
    public void cancelAttachments() {
        database.mailDataTable.setAttachmentCancelled(index, true);
        setCostItem(null);
        setCostMoney(0);
    }

    /**
     * このメールの添付アイテムが拒否されたのかどうかを返す
     * 
     * @return 添付アイテムが拒否されたのかどうか
     */
    @Override
    public boolean isAttachmentsRefused() {
        return database.mailDataTable.isAttachmentRefused(index);
    }

    /**
     * 受取拒否の理由を取得します。
     * 
     * @return 受取拒否の理由（設定されていない場合はnullになることに注意すること）
     */
    @Override
    public String getAttachmentsRefusedReason() {
        return database.mailDataTable.getAttachmentsRefusedReason(index);
    }

    /**
     * このメールの添付アイテムを拒否する。 添付アイテムが拒否されると、受信者はボックスを開けなくなり、 逆に送信者がボックスを開くことができるようになる。
     * 
     * @param attachmentsRefusedReason 拒否理由
     */
    @Override
    public void refuseAttachments(String attachmentsRefusedReason) {
        cancelAttachments();
        database.mailDataTable.setAttachmentRefused(index, true);
        if (attachmentsRefusedReason != null && attachmentsRefusedReason.length() > 0) {
            database.mailDataTable.setAttachmentsRefusedReason(index, attachmentsRefusedReason);
        }
    }

    /**
     * データのアップグレードを行う。
     * 
     * @return アップグレードを実行したかどうか
     */
    @Override
    protected boolean upgrade() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof MailDataDatabase)) {
            return false;
        }
        MailDataDatabase mailDataDatabase = (MailDataDatabase) o;
        return super.equals(o) && isSent == mailDataDatabase.isSent;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), isSent);
    }
}
