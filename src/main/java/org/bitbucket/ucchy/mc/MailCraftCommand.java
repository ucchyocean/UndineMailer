/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.mc;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

/**
 * MailCraftコマンドクラス
 * @author ucchy
 */
public class MailCraftCommand implements TabExecutor {

    private static final String PERMISSION = "mailcraft.command";

    private MailCraft parent;
    private MailManager manager;
    private MailCraftConfig config;

    public MailCraftCommand(MailCraft parent) {
        this.parent = parent;
        manager = parent.getMailManager();
        config = parent.getMailCraftConfig();
    }

    /**
     * コマンドが実行された時に呼び出されるメソッド
     * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // 引数なしの場合は、inboxコマンドを実行
        if ( args.length == 0 ) {
            return doInboxCommand(sender, command, label, new String[]{"inbox"});
        }

        // 後は、そのままそれぞれのサブコマンドを実行するようにする。
        if ( args[0].equalsIgnoreCase("inbox") ) {
            return doInboxCommand(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("outbox") ) {
            return doOutboxCommand(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("read") ) {
            return doReadCommand(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("text") ) {
            return doTextCommand(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("write") ) {
            return doWriteCommand(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("to") ) {
            return doToCommand(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("message") ) {
            return doMessageCommand(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("attach") ) {
            return doAttachCommand(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("send") ) {
            return doSendCommand(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("reload") ) {
            return doReloadCommand(sender, command, label, args);
        }

        return false;
    }

    /**
     * タブキーで補完された時に呼び出されるメソッド
     * @see org.bukkit.command.TabCompleter#onTabComplete(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        return null;
    }

    private boolean doInboxCommand(CommandSender sender, Command command, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".inbox") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        int page = 1;
        if ( args.length >= 2 && args[1].matches("[0-9]{1,9}") ) {
            page = Integer.parseInt(args[1]);
        }

        parent.getMailManager().displayInboxList(sender, page);

        return true;
    }

    private boolean doOutboxCommand(CommandSender sender, Command command, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".outbox") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        int page = 1;
        if ( args.length >= 2 && args[1].matches("[0-9]{1,9}") ) {
            page = Integer.parseInt(args[1]);
        }

        parent.getMailManager().displayOutboxList(sender, page);

        return true;
    }

    private boolean doReadCommand(CommandSender sender, Command command, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".read") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        // パラメータが足りない場合はエラーを表示して終了
        if ( args.length < 2 ) {
            sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "MailIndex"));
            return true;
        }

        // 指定されたパラメータが数字(正の整数)でない場合はエラーを表示して終了
        if ( !args[1].matches("[0-9]{1,9}") ) {
            sender.sendMessage(Messages.get("ErrorInvalidIndex", "%index", args[1]));
            return true;
        }

        int index = Integer.parseInt(args[1]);
        MailData mail = manager.getMail(index);

        // メールが見つからない場合はエラーを表示して終了
        if ( mail == null ) {
            sender.sendMessage(Messages.get("ErrorInvalidIndex", "%index", args[1]));
            return true;
        }

        // 他人のメールだった場合はエラーを表示して終了
        if ( !mail.isRelatedWith(sender.getName()) &&
                !sender.hasPermission(PERMISSION + ".read-all") ) {
            sender.sendMessage(Messages.get("ErrorNoneReadPermission"));
            return true;
        }

        // 該当のメールを表示
        manager.displayMail(sender, mail);

        return true;
    }

    private boolean doTextCommand(CommandSender sender, Command command, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".create") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        // パラメータが足りない場合はエラーを表示して終了
        if ( args.length < 2 ) {
            sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "To"));
            return true;
        }

        if ( args.length < 3 ) {
            sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "Message"));
            return true;
        }

        String[] dests = args[1].split(",");
        ArrayList<String> targets = new ArrayList<String>();

        // 宛先が全て見つからない場合はエラーを表示
        for ( String d : dests ) {
            OfflinePlayer player = Utility.getOfflinePlayer(d);
            if ( player == null || !player.hasPlayedBefore() ) {
                sender.sendMessage(Messages.get("ErrorNotFoundDestination", "%dest", d));
            } else {
                targets.add(d);
            }
        }

        // 自分自身が指定不可の設定の場合は、自分自身が指定されたらエラーを表示
        // TODO

        // 結果として、宛先が一つもないなら、そのまま終了
        if ( targets.size() == 0 ) {
            return true;
        }

        // メールを送信する
        manager.sendNewMail(sender.getName(), targets, args[2]);

        return true;
    }

    private boolean doWriteCommand(CommandSender sender, Command command, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".write") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        return true;
    }

    private boolean doToCommand(CommandSender sender, Command command, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".to") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        return true;
    }

    private boolean doMessageCommand(CommandSender sender, Command command, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".message") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        return true;
    }

    private boolean doAttachCommand(CommandSender sender, Command command, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".attach") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        return true;
    }

    private boolean doSendCommand(CommandSender sender, Command command, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".send") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        return true;
    }

    private boolean doReloadCommand(CommandSender sender, Command command, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".reload") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        // データをリロードする
        manager.reload();
        config.reloadConfig();
        Messages.reload(config.getLang());
        sender.sendMessage(Messages.get("InformationReload"));
        return true;
    }
}
