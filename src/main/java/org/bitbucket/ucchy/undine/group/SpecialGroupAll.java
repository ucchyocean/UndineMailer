/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine.group;

import java.io.File;
import java.util.ArrayList;

import org.bitbucket.ucchy.undine.UndineMailer;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bukkit.Bukkit;

/**
 * 特殊グループ all
 * @author ucchy
 */
public class SpecialGroupAll extends GroupData {

    public static final String NAME = "All";

    /**
     * コンストラクタ
     */
    public SpecialGroupAll() {
        super(NAME, MailSender.getMailSender(Bukkit.getConsoleSender()));
        setSendMode(UndineMailer.getInstance().getUndineConfig().getSpecialGroupAllSendMode());
        setModifyMode(GroupPermissionMode.NEVER);
        setDissolutionMode(GroupPermissionMode.NEVER);
    }

    /**
     * @see org.bitbucket.ucchy.undine.group.GroupData#getMembers()
     * @deprecated このメソッドは期待した結果とは違う結果を返します。
     */
    @Override
    @Deprecated
    public ArrayList<MailSender> getMembers() {
        return super.getMembers();
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
     * コンフィグセクションにグループを保存する
     * @param section コンフィグセクション
     * @see org.bitbucket.ucchy.undine.group.GroupData#saveToFile(java.io.File)
     */
    @Override
    protected void saveToFile(File file) {
        // do nothing.
    }
}
