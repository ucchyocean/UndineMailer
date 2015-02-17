/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine.group;

import java.io.File;

import org.bitbucket.ucchy.undine.UndineMailer;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bitbucket.ucchy.undine.sender.MailSenderDummy;

/**
 * PEXからインポートされたグループ
 * @author ucchy
 */
public class SpecialGroupPex extends GroupData {

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
