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
 * undine message コマンド
 * @author ucchy
 */
public class UndineMessageCommand implements SubCommand {

    private static final String NAME = "message";
    private static final String NODE = "undine." + NAME;

    private MailManager manager;

    /**
     * コンストラクタ
     * @param parent
     */
    public UndineMessageCommand(UndineMailer parent) {
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

        // パラメータが足りない場合はエラーを表示して終了
        if ( args.length < 2 ) {
            sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "Index Number"));
            return;
        }

        if ( args[1].matches("[0-9]{1,2}") ) {
            // 2番めの引数に数値が来た場合は、追加/再設定
            int line = Integer.parseInt(args[1]) - 1;
            if ( line < 0 ) {
                line = 0;
            } else if ( line >= MailData.MESSAGE_MAX_SIZE ) {
                line = MailData.MESSAGE_MAX_SIZE - 1;
            }

            StringBuffer message = new StringBuffer();
            for ( int i=2; i<args.length; i++ ) {
                if ( message.length() > 0 ) {
                    message.append(" ");
                }
                message.append(args[i]);
            }

            mail.setMessage(line, message.toString());

        } else if ( args[1].equalsIgnoreCase("delete")
                && args.length >= 3
                && args[2].matches("[0-9]{1,2}") ) {
            // 2番めの引数にdeleteが来た場合は、削除
            int line = Integer.parseInt(args[2]) - 1;
            if ( line < 0 ) {
                line = 0;
            } else if ( line >= mail.getMessage().size() ) {
                line = mail.getMessage().size() - 1;
            }
            mail.deleteMessage(line);

        }

        // 編集画面を表示する。
        manager.displayEditmode(MailSender.getMailSender(sender));
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
