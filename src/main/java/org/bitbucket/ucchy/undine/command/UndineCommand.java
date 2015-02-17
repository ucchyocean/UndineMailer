/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine.command;

import java.util.ArrayList;
import java.util.List;

import org.bitbucket.ucchy.undine.Messages;
import org.bitbucket.ucchy.undine.UndineMailer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

/**
 * Undineコマンドクラス
 * @author ucchy
 */
public class UndineCommand implements TabExecutor {

    public static final String COMMAND = "/umail";
    private static final String PERMISSION = "undine.";

    private ArrayList<SubCommand> commands;
    private ArrayList<String> commandNames;
    private UndineInboxCommand inboxCommand;
    private UndineHelpCommand helpCommand;

    /**
     * コンストラクタ
     * @param parent
     */
    public UndineCommand(UndineMailer parent) {

        inboxCommand = new UndineInboxCommand(parent);
        helpCommand = new UndineHelpCommand();

        commands = new ArrayList<SubCommand>();
        commands.add(inboxCommand);
        commands.add(new UndineOutboxCommand(parent));
        commands.add(new UndineTrashCommand(parent));
        commands.add(new UndineReadCommand(parent));
        commands.add(new UndineTextCommand(parent));
        commands.add(new UndineWriteCommand(parent));
        commands.add(new UndineToCommand(parent));
        commands.add(new UndineMessageCommand(parent));
        commands.add(new UndineAttachCommand(parent));
        commands.add(new UndineCostMoneyCommand(parent));
        commands.add(new UndineCostItemCommand(parent));
        commands.add(new UndineSendCommand(parent));
        commands.add(new UndineCancelCommand(parent));
        commands.add(helpCommand);
        commands.add(new UndineItemCommand());
        commands.add(new UndineReloadCommand(parent));

        commandNames = new ArrayList<String>();
        for ( SubCommand c : commands ) {
            commandNames.add(c.getCommandName());
        }
    }

    /**
     * コマンドが実行された時に呼び出されるメソッド
     * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // 引数なしの場合は、inboxコマンドを実行
        if ( args.length == 0 ) {
            inboxCommand.runCommand(sender, label, args);
            return true;
        }

        // 第1引数に指定されたコマンドを実行する
        for ( SubCommand c : commands ) {
            if ( c.getCommandName().equalsIgnoreCase(args[0]) ) {

                // パーミッションの確認
                String node = c.getPermissionNode();
                if ( !sender.hasPermission(node) ) {
                    sender.sendMessage(Messages.get("PermissionDeniedCommand"));
                    return true;
                }

                // 実行
                c.runCommand(sender, label, args);
                return true;
            }
        }

        // 第1引数が該当するコマンドが無いなら、ヘルプを表示する
        if ( !sender.hasPermission(helpCommand.getPermissionNode()) ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        helpCommand.runCommand(sender, label, args);
        return true;
    }

    /**
     * タブキーで補完された時に呼び出されるメソッド
     * @see org.bukkit.command.TabCompleter#onTabComplete(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if ( args.length == 1 ) {
            // コマンド名で補完する
            String arg = args[0].toLowerCase();
            ArrayList<String> coms = new ArrayList<String>();
            for ( String c : commandNames ) {
                if ( c.startsWith(arg)
                        && sender.hasPermission(PERMISSION + c) ) {
                    coms.add(c);
                }
            }
            return coms;

        } else if ( args.length >= 2 ) {
            // 該当のサブコマンドの補完を実行する。
            // 該当コマンドが無いなら、空配列を返して補完を実行しない。
            for ( SubCommand c : commands ) {
                if ( c.getCommandName().equalsIgnoreCase(args[0])
                        && sender.hasPermission(PERMISSION + c.getCommandName()) ) {
                    return c.tabComplete(sender, args);
                }
            }
            return new ArrayList<String>();
        }

        return null;
    }
}
