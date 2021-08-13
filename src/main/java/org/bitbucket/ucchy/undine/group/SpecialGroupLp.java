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
import org.bitbucket.ucchy.undine.sender.MailSenderDummy;

/**
 * LPからインポートされたグループ
 * @author ucchy
 */
public class SpecialGroupLp extends GroupData {

    public static final String NAME_PREFIX = "(lp)";

    /**
     * コンストラクタ
     * @param name グループ名
     * @param sendmode 送信権限
     */
    public SpecialGroupLp(String name, GroupPermissionMode sendmode) {
        super(NAME_PREFIX + name);
        setOwner(new MailSenderDummy("LuckPerms"));
        for ( String member : UndineMailer.getInstance().getLp().getGroupUsers(name) ) {
            MailSender sender = MailSender.getMailSenderFromString(member);
            if ( sender.isValidDestination() ) {
                addMember(sender);
            }
        }
        setSendMode(sendmode);
    }

    /**
     * グループのメンバーを取得する
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
     * @deprecated このメソッドは期待した結果とは違う結果を返します。
     */
    @Override
    @Deprecated
    public boolean isMember(MailSender sender) {
        return true; // 常にtrueを返す
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
}
