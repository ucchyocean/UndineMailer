/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine.group;

import java.io.File;
import java.util.ArrayList;

import org.bitbucket.ucchy.undine.Messages;
import org.bitbucket.ucchy.undine.UndineMailer;
import org.bitbucket.ucchy.undine.Utility;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bitbucket.ucchy.undine.sender.MailSenderPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * 特殊グループ AllLogin
 * @author ucchy
 */
public class SpecialGroupAllLogin extends GroupData {

    public static final String NAME = "AllLogin";

    /**
     * コンストラクタ
     */
    protected SpecialGroupAllLogin() {
        super(NAME);
        setOwner(MailSender.getMailSender(Bukkit.getConsoleSender()));
        setSendMode(UndineMailer.getInstance().getUndineConfig().getSpecialGroupAllLoginSendMode());
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
        for ( Player player : Utility.getOnlinePlayers() ) {
            members.add(MailSender.getMailSender(player));
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
        return (sender instanceof MailSenderPlayer && sender.isOnline());
    }

    /**
     * ファイルにグループを保存する
     * @param file ファイル
     * @see org.bitbucket.ucchy.undine.group.GroupData#saveToFile(java.io.File)
     * @deprecated このメソッドは実際は何も実行されません。
     */
    @Override
    @Deprecated
    protected void saveToFile(File file) {
        // do nothing.
    }

    /**
     * ホバー用のテキストを作成して返す
     * @return ホバー用のテキスト
     * @see org.bitbucket.ucchy.undine.group.GroupData#getHoverText()
     */
    @Override
    public String getHoverText() {
        return ChatColor.GOLD + Messages.get("GroupSpecialAllLoginMembers");
    }
}
