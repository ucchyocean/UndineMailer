/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine.group;

import java.util.ArrayList;

import org.bitbucket.ucchy.undine.Messages;
import org.bitbucket.ucchy.undine.UndineMailer;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bitbucket.ucchy.undine.sender.MailSenderPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

/**
 * 特殊グループ AllConnected
 * @author ucchy
 */
public class SpecialGroupAllConnected extends SpecialGroup {

    public static final String NAME = "AllConnected";

    /**
     * コンストラクタ
     */
    protected SpecialGroupAllConnected() {
        super(NAME);
        setOwner(MailSender.getMailSender(Bukkit.getConsoleSender()));
        setSendMode(UndineMailer.getInstance().getUndineConfig().getSpecialGroupAllConnectedSendMode());
        setModifyMode(GroupPermissionMode.NEVER);
        setDissolutionMode(GroupPermissionMode.NEVER);
    }

    /**
     * グループのメンバーを取得する
     * @see org.bitbucket.ucchy.undine.group.GroupData#getMembers()
     */
    @Override
    public ArrayList<MailSender> getMembers() {
        ArrayList<MailSender> members = new ArrayList<MailSender>();
        for ( String uuid : UndineMailer.getInstance().getPlayerUuids() ) {
            members.add(new MailSenderPlayer("$" + uuid));
        }
        return members;
    }

    /**
     * 指定されたsenderが、グループのメンバーかどうかを返す
     * @param sender
     * @return メンバーかどうか
     * @see org.bitbucket.ucchy.undine.group.GroupData#isMember(org.bitbucket.ucchy.undine.sender.MailSender)
     */
    @Override
    public boolean isMember(MailSender sender) {
        if ( !(sender instanceof MailSenderPlayer) ) {
            return false;
        }
        return ((MailSenderPlayer)sender).isUuidCached();
    }

    /**
     * ホバー用のテキストを作成して返す
     * @return ホバー用のテキスト
     * @see org.bitbucket.ucchy.undine.group.GroupData#getHoverText()
     */
    @Override
    public String getHoverText() {
        return ChatColor.GOLD + Messages.get("GroupSpecialAllConnectedMembers");
    }
}
