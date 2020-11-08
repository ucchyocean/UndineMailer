/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine.group;

import java.util.ArrayList;

import org.bitbucket.ucchy.undine.UndineMailer;
import org.bitbucket.ucchy.undine.sender.MailSender;

/**
 * メールグループ
 * @author ucchy
 */
public abstract class SpecialGroup extends GroupData {

    private MailSender owner;
    private ArrayList<MailSender> members;
    private GroupPermissionMode sendMode;
    private GroupPermissionMode modifyMode;
    private GroupPermissionMode dissolutionMode;
    
    /**
     * コンストラクタ(継承クラス用)
     * 
     * @param name グループ名
     */
    protected SpecialGroup(String name) {
        super(null, name);
        members = new ArrayList<MailSender>();
        sendMode = UndineMailer.getInstance().getUndineConfig().getSendModeDefault();
        modifyMode = UndineMailer.getInstance().getUndineConfig().getModifyModeDefault();
        dissolutionMode = UndineMailer.getInstance().getUndineConfig().getDissolutionModeDefault();
    }

    /**
     * グループにメンバーを追加する
     * 
     * @param member メンバー
     */
    @Override
    public void addMember(MailSender member) {
        if (!members.contains(member)) {
            members.add(member);
        }
    }

    /**
     * グループからメンバーを削除する
     * 
     * @param member メンバー
     */
    @Override
    public void removeMember(MailSender member) {
        if (members.contains(member)) {
            members.remove(member);
        }
    }

    /**
     * グループのオーナーを取得する
     * 
     * @return オーナー
     */
    @Override
    public MailSender getOwner() {
        return owner;
    }

    /**
     * グループのオーナーを設定する
     * 
     * @param owner
     */
    @Override
    public void setOwner(MailSender owner) {
        this.owner = owner;
    }

    /**
     * グループのメンバーを取得する
     * 
     * @return メンバー
     */
    @Override
    public ArrayList<MailSender> getMembers() {
        return members;
    }

    /**
     * 指定されたsenderがオーナーかどうかを返す
     * 
     * @param sender
     * @return オーナーかどうか
     */
    @Override
    public boolean isOwner(MailSender sender) {
        return owner.equals(sender);
    }

    /**
     * 送信権限モードを取得する
     * 
     * @return sendMode
     */
    @Override
    public GroupPermissionMode getSendMode() {
        return sendMode;
    }

    /**
     * 送信権限モードを設定する
     * 
     * @param sendMode sendMode
     */
    @Override
    public void setSendMode(GroupPermissionMode sendMode) {
        this.sendMode = sendMode;
    }

    /**
     * 変更権限モードを取得する
     * 
     * @return modifyMode
     */
    @Override
    public GroupPermissionMode getModifyMode() {
        return modifyMode;
    }

    /**
     * 変更権限モードを設定する
     * 
     * @param modifyMode modifyMode
     */
    @Override
    public void setModifyMode(GroupPermissionMode modifyMode) {
        this.modifyMode = modifyMode;
    }

    /**
     * 解散権限モードを取得する
     * 
     * @return dissolutionMode
     */
    @Override
    public GroupPermissionMode getDissolutionMode() {
        return dissolutionMode;
    }

    /**
     * 解散権限モードを設定する
     * 
     * @param dissolutionMode dissolutionMode
     */
    @Override
    public void setDissolutionMode(GroupPermissionMode dissolutionMode) {
        this.dissolutionMode = dissolutionMode;
    }

    /**
     * ファイルにグループを保存する
     * @see org.bitbucket.ucchy.undine.group.GroupData#save()
     * @deprecated このメソッドは実際は何も実行されません。
     */
    @Deprecated
    @Override
    public void save() {
        // Do nothing.
    }

    /**
     * データのアップグレードを行う。
     * @return アップグレードを実行したかどうか
     */
    protected boolean upgrade() {
        return false;
    }
}
