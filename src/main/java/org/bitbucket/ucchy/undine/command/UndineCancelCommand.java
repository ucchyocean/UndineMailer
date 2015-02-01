/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine.command;

import java.util.List;

import org.bitbucket.ucchy.undine.MailData;
import org.bitbucket.ucchy.undine.MailManager;
import org.bitbucket.ucchy.undine.Messages;
import org.bitbucket.ucchy.undine.UndineMailer;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * undine cancel コマンド
 * @author ucchy
 */
public class UndineCancelCommand implements SubCommand {

    private static final String NAME = "cancel";
    private static final String NODE = "undine." + NAME;

    private UndineMailer parent;
    private MailManager manager;

    /**
     * コンストラクタ
     * @param parent
     */
    public UndineCancelCommand(UndineMailer parent) {
        this.parent = parent;
        this.manager = parent.getMailManager();
    }

    /**
     * コマンドを取得します。
     * @return コマンド
     * @see org.bitbucket.ucchy.undine.command.SubCommand#getCommandName()
     */
    @Override
    public String getCommandName() {
        return NAME;
    }

    /**
     * パーミッションノードを取得します。
     * @return パーミッションノード
     * @see org.bitbucket.ucchy.undine.command.SubCommand#getPermissionNode()
     */
    @Override
    public String getPermissionNode() {
        return NODE;
    }

    /**
     * コマンドを実行します。
     * @param sender コマンド実行者
     * @param args 実行時の引数
     * @see org.bitbucket.ucchy.undine.command.SubCommand#runCommand(org.bukkit.command.CommandSender, java.lang.String[])
     */
    @Override
    public void runCommand(CommandSender sender, String[] args) {

        MailData mail = manager.getEditmodeMail(MailSender.getMailSender(sender));

        // 編集中でないならエラーを表示して終了
        if ( mail == null ) {
            sender.sendMessage(Messages.get("ErrorNotInEditmode"));
            return;
        }

        // 添付ファイルが残っているなら、エラーを表示して終了
        if ( mail.getAttachments().size() > 0 ) {
            sender.sendMessage(Messages.get("ErrorItemAttachedYet"));
            return;
        }

        // キャンセル
        manager.clearEditmodeMail(MailSender.getMailSender(sender));

        if ( sender instanceof Player ) {
            parent.getBoxManager().clearEditmodeBox((Player)sender);
        }

        sender.sendMessage(Messages.get("InformationEditCancelled"));
    }

    /**
     * TABキー補完を実行します。
     * @param sender コマンド実行者
     * @param args 補完時の引数
     * @return 補完候補
     * @see org.bitbucket.ucchy.undine.command.SubCommand#tabComplete(org.bukkit.command.CommandSender, java.lang.String[])
     */
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return null;
    }
}
