/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine.group;

import java.util.List;

import org.bitbucket.ucchy.undine.Messages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

/**
 * groupコマンド
 * @author ucchy
 */
public class GroupCommand implements TabExecutor {

    protected static final String COMMAND = "/ugroup";
    private static final String PERMISSION = "undine.group";
    private static final int PAGE_SIZE = 10;

    /**
     * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // 引数なしの場合は、listを開く
        if ( args.length == 0 ) {
            return doListCommand(sender, command, label, new String[]{"list"});
        }

        // 後は、そのままそれぞれのサブコマンドを実行するようにする。
        if ( args[0].equalsIgnoreCase("create") ) {
            return doCreateCommand(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("delete") ) {
            return doDeleteCommand(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("list") ) {
            return doListCommand(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("detail") ) {
            return doDetailCommand(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("add") ) {
            return doAddCommand(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("remove") ) {
            return doRemoveCommand(sender, command, label, args);
        }

        return false;
    }

    private boolean doCreateCommand(CommandSender sender, Command command2, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".create") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        // TODO 自動生成されたメソッド・スタブ
        return false;
    }

    private boolean doDeleteCommand(CommandSender sender, Command command2, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".delete") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        // TODO 自動生成されたメソッド・スタブ
        return false;
    }

    private boolean doListCommand(CommandSender sender, Command command2, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".list") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        // TODO 自動生成されたメソッド・スタブ
        return false;
    }

    private boolean doDetailCommand(CommandSender sender, Command command2, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".detail") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        // TODO 自動生成されたメソッド・スタブ
        return false;
    }

    private boolean doAddCommand(CommandSender sender, Command command2, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".add") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        // TODO 自動生成されたメソッド・スタブ
        return false;
    }

    private boolean doRemoveCommand(CommandSender sender, Command command2, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".remove") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        // TODO 自動生成されたメソッド・スタブ
        return false;
    }

    /**
     * @see org.bukkit.command.TabCompleter#onTabComplete(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public List<String> onTabComplete(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
        // TODO 自動生成されたメソッド・スタブ
        return null;
    }
}
