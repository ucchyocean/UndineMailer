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

/**
 *
 * @author ucchy
 */
public class UndineTrashCommand implements SubCommand {

    private static final String NAME = "trash";
    private static final String NODE = "undine." + NAME;

    private MailManager manager;

    /**
     * コンストラクタ
     * @param parent
     */
    public UndineTrashCommand(UndineMailer parent) {
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

        // MailManagerのロードが完了していないなら、エラーを表示して終了
        if ( !manager.isLoaded() ) {
            sender.sendMessage(Messages.get("ErrorCannotListInitializingYet"));
            return;
        }

        MailSender ms = MailSender.getMailSender(sender);

        if ( args.length >= 3 && args[1].equalsIgnoreCase("set") ) {
            // メールをゴミ箱に移動するコマンドの実行  /mail trash set <index>

            // 指定されたパラメータが数字(正の整数)でない場合はエラーを表示して終了
            if ( !args[2].matches("[0-9]{1,9}") ) {
                sender.sendMessage(Messages.get("ErrorInvalidIndex", "%index", args[2]));
                return;
            }

            int index = Integer.parseInt(args[2]);
            MailData mail = manager.getMail(index);

            // メールが見つからない場合はエラーを表示して終了
            if ( mail == null ) {
                sender.sendMessage(Messages.get("ErrorInvalidIndex", "%index", args[2]));
                return;
            }

            // 他人のメールだった場合はエラーを表示して終了
            if ( !mail.isRelatedWith(ms) ) {
                sender.sendMessage(Messages.get("ErrorNoneReadPermission"));
                return;
            }

            // 既に捨てたメールならエラーを表示して終了
            if ( mail.isSetTrash(ms) ) {
                sender.sendMessage(Messages.get("ErrorAlreadyTrashed", "%index", mail.getIndex()));
                return;
            }

            // 未読のメールならエラーを表示して終了
            if ( !mail.isRead(ms) ) {
                sender.sendMessage(Messages.get("ErrorCannotDropBecauseUnread", "%index", mail.getIndex()));
                return;
            }

            // 添付アイテムが残っているメールならエラーを表示して終了
            if ( mail.getAttachments().size() > 0 ) {
                sender.sendMessage(Messages.get("ErrorCannotDropBecauseAttached", "%index", mail.getIndex()));
                return;
            }

            // ゴミフラグを設定
            mail.setTrashFlag(ms);
            manager.saveMail(mail);

            sender.sendMessage(Messages.get("InformationTrashed", "%index", mail.getIndex()));
            return;

        } else if ( args.length >= 3 && args[1].equalsIgnoreCase("restore") ) {
            // メールをゴミ箱から戻すコマンドの実行  /mail trash restore <index>

            // 指定されたパラメータが数字(正の整数)でない場合はエラーを表示して終了
            if ( !args[2].matches("[0-9]{1,9}") ) {
                sender.sendMessage(Messages.get("ErrorInvalidIndex", "%index", args[2]));
                return;
            }

            int index = Integer.parseInt(args[2]);
            MailData mail = manager.getMail(index);

            // メールが見つからない場合はエラーを表示して終了
            if ( mail == null ) {
                sender.sendMessage(Messages.get("ErrorInvalidIndex", "%index", args[2]));
                return;
            }

            // 他人のメールだった場合はエラーを表示して終了
            if ( !mail.isRelatedWith(ms) ) {
                sender.sendMessage(Messages.get("ErrorNoneReadPermission"));
                return;
            }

            // 捨てられていないメールならエラーを表示して終了
            if ( !mail.isSetTrash(ms) ) {
                sender.sendMessage(Messages.get("ErrorNotTrashed", "%index", mail.getIndex()));
                return;
            }

            // ゴミフラグを除去
            mail.removeTrashFlag(ms);
            manager.saveMail(mail);

            sender.sendMessage(Messages.get("InformationTrashRestored", "%index", mail.getIndex()));
            return;

        }

        // 以下、ゴミ箱フォルダのメールリスト表示コマンドの実行  /mail trash [page]

        int page = 1;
        if ( args.length >= 2 && args[1].matches("[0-9]{1,9}") ) {
            page = Integer.parseInt(args[1]);
        }

        manager.displayTrashboxList(ms, page);
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
