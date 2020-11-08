/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine.group;

import org.bitbucket.ucchy.undine.UndineMailer;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bitbucket.ucchy.undine.sender.MailSenderDummy;

/**
 * PEXからインポートされたグループ
 * @author ucchy
 */
public class SpecialGroupPex extends SpecialGroup {

    public static final String NAME_PREFIX = "(pex)";

    /**
     * コンストラクタ
     * @param name グループ名
     * @param sendmode 送信権限
     */
    public SpecialGroupPex(String name, GroupPermissionMode sendmode) {
        super(NAME_PREFIX + name);
        setOwner(new MailSenderDummy("PermissionsEx"));
        for ( String member : UndineMailer.getInstance().getPex().getGroupUsers(name) ) {
            MailSender sender = MailSender.getMailSenderFromString(member);
            if ( sender.isValidDestination() ) {
                addMember(sender);
            }
        }
        setSendMode(sendmode);
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
}
