/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.bitbucket.ucchy.undine.group.GroupData;
import org.bitbucket.ucchy.undine.group.SpecialGroupAll;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

/**
 * メールのデータ
 * @author ucchy
 */
public abstract class MailData implements Comparable<MailData>, Cloneable {

    public static final int MESSAGE_MAX_SIZE = 15;
    private static final int SUMMARY_MAX_SIZE = 45;

    protected int index = 0;
    
    /** メールが送信されたかどうか。Database モード用の変数であり、フラットファイルモードでは index == 0 のとき isSent == true と判定する */
    private boolean isSent;

    /**
     * 指定されたファイルへ保存する
     * @param file 保存先
     */
    public abstract void save();

    /**
     * 設定されている宛先を全て消去する
     */
    public abstract void deleteAllTo();

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
    public void setIndex(int index) {
        this.index = index;
    }

    /**
     * このメールの宛先を取得します。
     * @return 宛先
     */
    public abstract List<MailSender> getTo();

    /**
     * このメールの宛先を設定します。
     * @param line 宛先番号（0から始まることに注意）
     * @param to 宛先
     */
    public abstract void setTo(int line, MailSender to);

    /**
     * このメールの宛先を追加します。
     * @param to 宛先
     */
    public abstract void addTo(MailSender to);

    /**
     * このメールの宛先を追加します。
     * @param to 宛先
     */
    public abstract void addTo(List<MailSender> to);

    /**
     * このメールの指定された宛先を削除します。
     * @param line 宛先番号（0から始まることに注意）
     */
    public abstract void deleteTo(int line);

    /**
     * このメールの発信元を取得します。
     * @return 発信元
     */
    public abstract MailSender getFrom();

    /**
     * このメールの発信元を設定します。
     * @param from 発信元
     */
    public abstract void setFrom(MailSender from);

    /**
     * このメールのメッセージを取得します。
     * @return メッセージ
     */
    public abstract List<String> getMessage();

    /**
     * このメールのメッセージを設定します。
     * @param message メッセージ
     */
    public abstract void setMessage(List<String> message);

    /**
     * このメールのメッセージを設定します。
     * @param line 行番号（0から始まることに注意）
     * @param message メッセージ
     */
    public abstract void setMessage(int line, String message);

    /**
     * このメールのメッセージに、指定した内容を追加します。
     * @param message メッセージ
     */
    public abstract void addMessage(String message);

    /**
     * このメールのメッセージの、指定された行番号を削除します。
     * @param line 宛先番号（0から始まることに注意）
     */
    public abstract void deleteMessage(int line);

    /**
     * 宛先グループを取得します。
     * @return 宛先グループ
     */
    public abstract List<String> getToGroups();

    /**
     * 宛先グループを取得します。
     * @return 宛先グループ
     */
    public List<GroupData> getToGroupsConv() {
        return UndineMailer.getInstance().getGroupManager().getGroups(getToGroups());
    }

    /**
     * このメールの宛先グループを設定します。
     * @param line 宛先番号（0から始まることに注意）
     * @param group グループ
     */
    public abstract void setToGroup(int line, String group);

    /**
     * このメールの宛先グループに、新しいグループを追加します。
     * 
     * @param group グループ名
     */
    public void addToGroup(String group) {
        setToGroup(Integer.MAX_VALUE, group);
    }

    /**
     * このメールの宛先グループの、指定された行番号を削除します。
     * @param line 宛先番号（0から始まることに注意）
     */
    public abstract void deleteToGroup(int line);

    /**
     * 統合宛先を設定する
     * @param total 統合宛先
     */
    protected abstract void setToTotal(List<MailSender> total);

    /**
     * 統合宛先（宛先＋宛先グループの和集合）を取得する。未送信メールの場合はnullになる。
     * @return 統合宛先
     */
    public abstract List<MailSender> getToTotal();

    /**
     * このメールに添付されたアイテムを取得します。
     * @return 添付アイテム
     */
    public abstract List<ItemStack> getAttachments();

    /**
     * このメールの添付アイテムを設定します。
     * @param attachments 添付アイテム
     */
    public abstract void setAttachments(List<ItemStack> attachments);

    /**
     * 指定されたアイテムを添付アイテムに追加します。
     * @param item アイテム
     */
    public abstract void addAttachment(ItemStack item);

    /**
     * このメールを読んだ人のリストを取得します。
     * @return 読んだ人のリスト
     */
    public abstract List<MailSender> getReadFlags();

    /**
     * このメールに削除フラグを付けた人のリストを取得します。
     * @return 削除フラグをつけている人のリスト
     */
    public abstract List<MailSender> getTrashFlags();

    /**
     * このメールの添付アイテムを受け取るのに必要な金額を取得します。
     * @return 受け取り金額
     */
    public abstract double getCostMoney();

    /**
     * このメールの添付アイテムを受け取るのに必要な金額を設定します。
     * @param feeMoney 受け取り金額
     */
    public abstract void setCostMoney(double feeMoney);

    /**
     * このメールの添付アイテムを受け取るのに必要な引き換えアイテムを取得します。
     * @return 引き換えアイテム
     */
    public abstract ItemStack getCostItem();

    /**
     * このメールの添付アイテムを受け取るのに必要な引き換えアイテムを設定します。
     * @param feeItem 引き換えアイテム
     */
    public abstract void setCostItem(ItemStack feeItem);

    /**
     * このメールの送信時間を取得します。
     * @return 送信時間
     */
    public abstract Date getDate();

    /**
     * このメールの送信時間を設定します（メール送信時に自動で割り当てられます）。
     * @param date 送信時間
     */
    protected abstract void setDate(Date date);

    /**
     * このメールが送信された地点を取得します。
     * @return 送信地点
     */
    public abstract Location getLocation();

    /**
     * このメールの送信地点を設定します（メール送信時に自動で割り当てられます）。
     * @param location 送信地点
     */
    public abstract void setLocation(Location location);

    /**
     * メール送信時の添付アイテムを取得します。
     * @return メール送信時の添付アイテム
     */
    public abstract List<ItemStack> getAttachmentsOriginal();

    /**
     * attachmentOriginalに、添付ファイルのコピーを行います
     * （メール送信時に自動で行われます）。
     */
    protected abstract void makeAttachmentsOriginal();

    /**
     * 指定したプレイヤーが、このメールを読んだかどうかを返します。
     * @param player プレイヤー
     * @return 読んだかどうか
     */
    public abstract boolean isRead(MailSender player);

    /**
     * 指定した名前のsenderの既読マークを付ける
     * @param sender sender
     */
    public abstract void setReadFlag(MailSender sender);

    /**
     * 指定した人が、このメールに削除マークをつけているかどうかを返します。
     * @param sender
     * @return 削除マークをつけているかどうか
     */
    public abstract boolean isSetTrash(MailSender sender);

    /**
     * 指定した人の削除マークを付ける
     * @param sender
     */
    public abstract void setTrashFlag(MailSender sender);

    /**
     * 指定した人の削除マークを消す
     * @param sender
     */
    public abstract void removeTrashFlag(MailSender sender);

    /**
     * 指定された名前のプレイヤーは、このメールの関係者かどうかを返す。
     * @param sender sender
     * @return 指定された名前がtoまたはfromに含まれるかどうか
     */
    public abstract boolean isRelatedWith(MailSender sender);

    /**
     * 指定された名前のプレイヤーは、このメールの受信者かどうかを返す。
     * @param sender sender
     * @return 指定された名前がtoに含まれるかどうか
     */
    public abstract boolean isRecipient(MailSender sender);

    /**
     * このメールは編集中モードなのかどうかを返す
     * @return 編集中かどうか
     */
    public boolean isSent() {
        return isSent;
    }

    /**
     * このメールを送信済みとしてマークする。
     */
    public void setSent() {
        this.isSent = true;
    }

    /**
     * このメールの添付アイテムがオープンされたのかどうかを返す
     * @return 添付アイテムがオープンされたのかどうか
     */
    public abstract boolean isAttachmentsOpened();

    /**
     * このメールの添付アイテムをオープンされたとして記録する。
     * 受信者が添付ボックスを一度でも開いた事がある状態なら、
     * 送信者は添付アイテムをキャンセルすることができなくなる。
     */
    public abstract void setOpenAttachments();

    /**
     * このメールの添付アイテムがキャンセルされたのかどうかを返す
     * @return 添付アイテムがキャンセルされたのかどうか
     */
    public abstract boolean isAttachmentsCancelled();

    /**
     * このメールの添付アイテムをキャンセルする。
     * 添付アイテムがキャンセルされると、受信者はボックスを開けなくなり、
     * 逆に送信者がボックスを開くことができるようになる。
     */
    public abstract void cancelAttachments();

    /**
     * このメールの添付アイテムが拒否されたのかどうかを返す
     * @return 添付アイテムが拒否されたのかどうか
     */
    public abstract boolean isAttachmentsRefused();

    /**
     * 受取拒否の理由を取得します。
     * @return 受取拒否の理由（設定されていない場合はnullになることに注意すること）
     */
    public abstract String getAttachmentsRefusedReason();

    /**
     * このメールの添付アイテムを拒否する。
     * 添付アイテムが拒否されると、受信者はボックスを開けなくなり、
     * 逆に送信者がボックスを開くことができるようになる。
     * @param attachmentsRefusedReason 拒否理由
     */
    public abstract void refuseAttachments(String attachmentsRefusedReason);

    
    /**
     * このオブジェクトの複製を作成して返す。
     * 
     * @see java.lang.Object#clone()
     */
    @Override
    public abstract MailData clone();

    /**
     * このメールが全体メールなのかどうかを返します。
     * @return 全体メールかどうか
     */
    public boolean isAllMail() {
        return getToGroups().contains(SpecialGroupAll.NAME);
    }

    /**
     * インスタンス同士の比較を行う。このメソッドを実装しておくことで、
     * Java8でのHashMapのキー挿入における高速化が期待できる（らしい）。
     * @param other 他のインスタンス
     * @return 比較結果
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(MailData other) {
        if ( this.index > other.index ) return 1;
        if ( this.index < other.index ) return -1;
        return 0;
    }

    /**
     * このメールのInbox用サマリー文字列を返す。
     * "送信者 (送信日時) 1行目の内容"
     * @return サマリー
     */
    public String getInboxSummary() {

        String fdate = getFormattedDate(getDate());
        String summary = String.format("%s (%s) %s",
                getFrom().getName(), fdate, Utility.removeColorCode(getMessage().get(0)));

        // 長すぎる場合は切る
        if ( summary.length() > SUMMARY_MAX_SIZE + 2 ) {
            summary = summary.substring(0, SUMMARY_MAX_SIZE) + "..";
        }

        return summary;
    }

    /**
     * このメールのOutbox用サマリー文字列を返す。
     * "受信者 (送信日時) 1行目の内容"
     * @return サマリー
     */
    public String getOutboxSummary() {

        String fdate = getFormattedDate(getDate());
        String todesc = joinToAndGroup();
        if ( todesc.length() > 15 ) { // 長すぎる場合は切る
            todesc = todesc.substring(0, 15);
        }
        String summary = String.format("%s (%s) %s",
                todesc, fdate, Utility.removeColorCode(getMessage().get(0)));

        // 長すぎる場合は切る
        if ( summary.length() > SUMMARY_MAX_SIZE + 2 ) {
            summary = summary.substring(0, SUMMARY_MAX_SIZE) + "..";
        }

        return summary;
    }

    /**
     * 宛先のリストを、コンマを使ってつなげる
     * @return 繋がった文字列
     */
    private String joinToAndGroup() {

        StringBuffer buffer = new StringBuffer();
        for ( MailSender item : getTo() ) {
            if ( buffer.length() > 0 ) {
                buffer.append(", ");
            }
            buffer.append(item.getName());
        }
        for ( String group : getToGroups() ) {
            if ( buffer.length() > 0 ) {
                buffer.append(", ");
            }
            buffer.append(group);
        }
        return buffer.toString();
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
     * データのアップグレードを行う。
     * @return アップグレードを実行したかどうか
     */
    protected abstract boolean upgrade();

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof MailDataFlatFile)) {
            return false;
        }
        MailDataFlatFile mailDataDatabase = (MailDataFlatFile) o;
        return index == mailDataDatabase.index;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(index);
    }
}
