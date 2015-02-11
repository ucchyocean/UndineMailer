/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine.command;

import java.util.List;

import org.bitbucket.ucchy.undine.MailManager;
import org.bitbucket.ucchy.undine.Messages;
import org.bitbucket.ucchy.undine.UndineMailer;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bukkit.command.CommandSender;

/**
 * undine outbox コマンド
 * @author ucchy
 */
public class UndineOutboxCommand implements SubCommand {

    private static final String NAME = "outbox";
    private static final String NODE = "undine." + NAME;

    private MailManager manager;

    /**
     * コンストラクタ
     * @param parent
     */
    public UndineOutboxCommand(UndineMailer parent) {
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

        int page = 1;
        if ( args.length >= 2 && args[1].matches("[0-9]{1,9}") ) {
            page = Integer.parseInt(args[1]);
        }

        manager.displayOutboxList(
                MailSender.getMailSender(sender), page);
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
