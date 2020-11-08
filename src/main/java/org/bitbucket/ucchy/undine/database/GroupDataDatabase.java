package org.bitbucket.ucchy.undine.database;

import java.util.ArrayList;

import org.bitbucket.ucchy.undine.UndineMailer;
import org.bitbucket.ucchy.undine.group.GroupData;
import org.bitbucket.ucchy.undine.group.GroupPermissionMode;
import org.bitbucket.ucchy.undine.sender.MailSender;

/**
 * データベース管理のメールグループ
 * @author LazyGon
 */
public class GroupDataDatabase extends GroupData {

    private GroupDataTable groupDataTable;
    private GroupMembersTable groupMembersTable;
    private MailSenderTable mailSenderTable;

    public GroupDataDatabase(UndineMailer parent, String name) {
        super(parent, name);
        this.groupDataTable = parent.getDatabase().groupDataTable;
        this.groupMembersTable = parent.getDatabase().groupMembersTable;
        this.mailSenderTable = parent.getDatabase().mailSenderTable;

        if (!this.groupDataTable.exists(name)) {
            throw new IllegalArgumentException("The group " + name + " is not registered on database yet. Specify owner for constructor to register group.");
        }
    }

    /**
     * コンストラクタ
     */
    public GroupDataDatabase(UndineMailer parent, String name, MailSender owner) {
        super(parent, name);
        this.groupDataTable = parent.getDatabase().groupDataTable;
        this.groupMembersTable = parent.getDatabase().groupMembersTable;
        this.mailSenderTable = parent.getDatabase().mailSenderTable;
        
        this.groupDataTable.add(name, owner);
        addMember(owner);
        setSendMode(parent.getUndineConfig().getSendModeDefault());
        setModifyMode(parent.getUndineConfig().getModifyModeDefault());
        setDissolutionMode(parent.getUndineConfig().getDissolutionModeDefault());
    }

    @Override
    public void save() {
        // Do nothing. データは常にデータベースと同期されているため、保存は不要。
    }

    /**
     * グループにメンバーを追加する
     * 
     * @param member メンバー
     */
    @Override
    public void addMember(MailSender member) {
        groupMembersTable.addMember(name, mailSenderTable.getId(member));
    }

    /**
     * グループからメンバーを削除する
     * 
     * @param member メンバー
     */
    @Override
    public void removeMember(MailSender member) {
        groupMembersTable.removeMember(name, mailSenderTable.getId(member));
    }

    /**
     * グループのオーナーを取得する
     * 
     * @return オーナー
     */
    @Override
    public MailSender getOwner() {
        return mailSenderTable.getById(groupDataTable.getOwnerId(name));
    }

    /**
     * グループのオーナーを設定する
     * 
     * @param owner
     */
    @Override
    public void setOwner(MailSender owner) {
        groupDataTable.setOwner(name, mailSenderTable.getId(owner));
    }

    /**
     * グループのメンバーを取得する
     * 
     * @return メンバー
     */
    @Override
    public ArrayList<MailSender> getMembers() {
        return new ArrayList<>(mailSenderTable.getByIds(groupMembersTable.getMemberIdsOf(name)).values());
    }

    /**
     * 指定されたsenderが、グループのメンバーかどうかを返す
     * 
     * @param sender
     * @return メンバーかどうか
     */
    @Override
    public boolean isMember(MailSender sender) {
        return groupMembersTable.isMember(name, mailSenderTable.getId(sender));
    }

    /**
     * 指定されたsenderがオーナーかどうかを返す
     * 
     * @param sender
     * @return オーナーかどうか
     */
    @Override
    public boolean isOwner(MailSender sender) {
        return groupDataTable.getOwnerId(name) == mailSenderTable.getId(sender);
    }

    /**
     * 送信権限モードを取得する
     * 
     * @return sendMode
     */
    @Override
    public GroupPermissionMode getSendMode() {
        return groupDataTable.getSendMode(name);
    }

    /**
     * 送信権限モードを設定する
     * 
     * @param sendMode sendMode
     */
    @Override
    public void setSendMode(GroupPermissionMode sendMode) {
        groupDataTable.setSendMode(name, sendMode);
    }

    /**
     * 変更権限モードを取得する
     * 
     * @return modifyMode
     */
    @Override
    public GroupPermissionMode getModifyMode() {
        return groupDataTable.getModifyMode(name);
    }

    /**
     * 変更権限モードを設定する
     * 
     * @param modifyMode modifyMode
     */
    @Override
    public void setModifyMode(GroupPermissionMode modifyMode) {
        groupDataTable.setModifyMode(name, modifyMode);
    }

    /**
     * 解散権限モードを取得する
     * 
     * @return dissolutionMode
     */
    @Override
    public GroupPermissionMode getDissolutionMode() {
        return groupDataTable.getDissolutionMode(name);
    }

    /**
     * 解散権限モードを設定する
     * 
     * @param dissolutionMode dissolutionMode
     */
    @Override
    public void setDissolutionMode(GroupPermissionMode dissolutionMode) {
        groupDataTable.setDissolutionMode(name, dissolutionMode);
    }

    /**
     * データのアップグレードを行う。データベースへの書き込みと読み出しのタイミングで常にチェックしているため、不要。
     * @return アップグレードを実行したかどうか
     */
    @Override
    protected boolean upgrade() {
        return false;
    }
}
