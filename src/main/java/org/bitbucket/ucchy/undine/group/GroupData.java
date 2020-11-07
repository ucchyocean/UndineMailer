/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine.group;

import java.util.ArrayList;

import org.bitbucket.ucchy.undine.UndineMailer;
import org.bitbucket.ucchy.undine.database.GroupDataDatabase;
import org.bitbucket.ucchy.undine.database.Database.DatabaseType;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bitbucket.ucchy.undine.sender.MailSenderPlayer;
import org.bukkit.ChatColor;

/**
 * メールグループ
 * @author ucchy
 */
public abstract class GroupData {

    protected final UndineMailer parent;

    protected String name;

    public GroupData(UndineMailer parent, String name) {
        this.parent = parent;
        this.name = name;
    }

    public abstract void save();

    /**
     * グループにメンバーを追加する
     * @param member メンバー
     */
    public abstract void addMember(MailSender member);

    /**
     * グループからメンバーを削除する
     * @param member メンバー
     */
    public abstract void removeMember(MailSender member);

    /**
     * グループ名を取得する
     * @return グループ名
     */
    public String getName() {
        return name;
    }

    /**
     * グループのオーナーを取得する
     * @return オーナー
     */
    public abstract MailSender getOwner();

    /**
     * グループのオーナーを設定する
     * @param owner
     */
    public abstract void setOwner(MailSender owner);

    /**
     * グループのメンバーを取得する
     * @return メンバー
     */
    public abstract ArrayList<MailSender> getMembers();

    /**
     * 指定されたsenderが、グループのメンバーかどうかを返す
     * @param sender
     * @return メンバーかどうか
     */
    public abstract boolean isMember(MailSender sender);

    /**
     * 指定されたsenderがオーナーかどうかを返す
     * @param sender
     * @return オーナーかどうか
     */
    public abstract boolean isOwner(MailSender sender);

    /**
     * 送信権限モードを取得する
     * @return sendMode
     */
    public abstract GroupPermissionMode getSendMode();

    /**
     * 送信権限モードを設定する
     * @param sendMode sendMode
     */
    public abstract void setSendMode(GroupPermissionMode sendMode);

    /**
     * 指定されたsenderは、送信権限を持っているかどうかを調べる
     * @param sender
     * @return 送信権限を持っているかどうか
     */
    public boolean canSend(MailSender sender) {
        return permissionCheck(sender,
                getSendMode(), "undine.group.send-all");
    }

    /**
     * 変更権限モードを取得する
     * @return modifyMode
     */
    public abstract GroupPermissionMode getModifyMode();

    /**
     * 変更権限モードを設定する
     * @param modifyMode modifyMode
     */
    public abstract void setModifyMode(GroupPermissionMode modifyMode);

    /**
     * 指定されたsenderは、変更権限を持っているかどうかを調べる
     * @param sender
     * @return 変更権限を持っているかどうか
     */
    public boolean canModify(MailSender sender) {
        return permissionCheck(sender,
                getModifyMode(), "undine.group.modify-all");
    }

    /**
     * 解散権限モードを取得する
     * @return dissolutionMode
     */
    public abstract GroupPermissionMode getDissolutionMode();

    /**
     * 解散権限モードを設定する
     * @param dissolutionMode dissolutionMode
     */
    public abstract void setDissolutionMode(GroupPermissionMode dissolutionMode);

    /**
     * 指定されたsenderは、解散権限を持っているかどうかを調べる
     * @param sender
     * @return 解散権限を持っているかどうか
     */
    public boolean canBreakup(MailSender sender) {
        return permissionCheck(sender,
                getDissolutionMode(), "undine.group.dissolution-all");
    }

    /**
     * ホバー用のテキストを作成して返す
     * @return ホバー用のテキスト
     */
    public String getHoverText() {
        MailSender owner = getOwner(); 
        StringBuffer hover = new StringBuffer();
        hover.append(ChatColor.GOLD + owner.getName() + ChatColor.WHITE);
        ArrayList<MailSender> members = new ArrayList<MailSender>(getMembers());
        members.remove(owner);
        for ( int j=0; j<5; j++ ) {
            if ( members.size() <= j ) {
                break;
            }
            hover.append("\n" + members.get(j).getName());
        }
        if ( members.size() - 5 > 0 ) {
            hover.append("\n ... and " + (members.size() - 5) + " more ...");
        }
        return hover.toString();
    }

    /**
     * パーミッションのチェックを行う
     * @param sender
     * @param mode
     * @param specialNode
     * @return パーミッションの有無
     */
    private boolean permissionCheck(
            MailSender sender, GroupPermissionMode mode, String specialNode) {
        if ( mode == GroupPermissionMode.NEVER ) return false;
        if ( sender.hasPermission(specialNode) ) {
            return true;
        }
        switch ( mode ) {
        case EVERYONE:
            return true;
        case MEMBER:
            return isMember(sender);
        case OWNER:
            return getOwner().equals(sender);
        case OP:
            return sender.isOp();
        default:
            return false;
        }
    }

    /**
     * データのアップグレードを行う。
     * @return アップグレードを実行したかどうか
     */
    protected boolean upgrade() {
        boolean upgraded = false;
        MailSender owner = getOwner();
        if ( owner instanceof MailSenderPlayer ) {
            if ( ((MailSenderPlayer) owner).upgrade() ) {
                upgraded = true;
            }
        }
        ArrayList<MailSender> members = getMembers();
        for ( MailSender ms : members ) {
            if ( ms instanceof MailSenderPlayer ) {
                if ( ((MailSenderPlayer) ms).upgrade() ) {
                    upgraded = true;
                }
            }
        }
        return upgraded;
    }

    public static GroupData create(UndineMailer parent, String name, MailSender owner) {
        GroupData groupData;
        if (parent.getUndineConfig().getDatabaseType() == DatabaseType.FLAT_FILE) {
            groupData = new GroupDataFlatFile(parent, name, owner);
            parent.getGroupManager().saveGroupData(groupData);
        } else {
            groupData = new GroupDataDatabase(parent, name, owner);
        }
        return groupData;
    }
}
