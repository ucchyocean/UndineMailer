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
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

/**
 * 特殊グループ all
 * @author ucchy
 */
public class SpecialGroupAll extends SpecialGroup {

    public static final String NAME = "All";

    /**
     * コンストラクタ
     */
    public SpecialGroupAll() {
        super(NAME);
        setOwner(MailSender.getMailSender(Bukkit.getConsoleSender()));
        setSendMode(UndineMailer.getInstance().getUndineConfig().getSpecialGroupAllSendMode());
        super.setModifyMode(GroupPermissionMode.NEVER);
        super.setDissolutionMode(GroupPermissionMode.NEVER);
    }

    @Override
    public void setDissolutionMode(GroupPermissionMode dissolutionMode) {
        // Do nothing.
    }

    @Override
    public void setModifyMode(GroupPermissionMode modifyMode) {
        // Do nothing.
    }

    /**
     * グループのメンバーを取得する
     * @see org.bitbucket.ucchy.undine.group.GroupData#getMembers()
     * @deprecated このメソッドは期待した結果とは違う結果を返します。
     */
    @Override
    @Deprecated
    public ArrayList<MailSender> getMembers() {
        return new ArrayList<>();
    }

    /**
     * 指定されたsenderが、グループのメンバーかどうかを返す
     * @param sender
     * @return メンバーかどうか
     * @see org.bitbucket.ucchy.undine.group.GroupData#isMember(org.bitbucket.ucchy.undine.sender.MailSender)
     */
    @Override
    public boolean isMember(MailSender sender) {
        return true; // 常にtrueを返す
    }

    /**
     * ホバー用のテキストを作成して返す
     * @return ホバー用のテキスト
     * @see org.bitbucket.ucchy.undine.group.GroupData#getHoverText()
     */
    @Override
    public String getHoverText() {
        return ChatColor.GOLD + Messages.get("GroupSpecialAllMembers");
    }
}
