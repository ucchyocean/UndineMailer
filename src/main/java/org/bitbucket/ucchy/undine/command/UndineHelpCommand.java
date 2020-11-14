/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine.command;

import java.util.List;

import org.bitbucket.ucchy.undine.Messages;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import com.github.ucchyocean.messaging.tellraw.ClickEventType;
import com.github.ucchyocean.messaging.tellraw.MessageComponent;
import com.github.ucchyocean.messaging.tellraw.MessageParts;

/**
 * undine help コマンド
 * @author ucchy
 */
public class UndineHelpCommand implements SubCommand {

    private static final String NAME = "help";
    private static final String NODE = "undine." + NAME;
    private static final String PERMISSION_PREFIX = "undine.";

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
     * @param label 実行時のラベル
     * @param args 実行時の引数
     * @see org.bitbucket.ucchy.undine.command.SubCommand#runCommand(org.bukkit.command.CommandSender, java.lang.String[])
     */
    @Override
    public void runCommand(CommandSender sender, String label, String[] args) {

        String parts = Messages.get("ListHorizontalParts");
        String pre = Messages.get("ListVerticalParts");

        sender.sendMessage(parts + parts + " "
                + Messages.get("HelpTitle") + " " + parts + parts);

        // umailコマンドのヘルプ
        for ( String c : new String[]{
                "inbox", "outbox", "trash", "text", "write",
                "item", "reload"} ) {

            if ( !sender.hasPermission(PERMISSION_PREFIX + c) ) {
                continue;
            }

            MessageComponent msg = new MessageComponent();
            msg.addText(pre);

            String l = "[" + Messages.get("HelpCommand_" + c) + "]";
            MessageParts button = new MessageParts(l, ChatColor.AQUA);
            if ( c.equals("text") ) {
                // undine text コマンドだけは、suggest_commandを設定する。
                button.setClickEvent(ClickEventType.SUGGEST_COMMAND,
                        UndineCommand.COMMAND + " " + c);
            } else {
                button.setClickEvent(ClickEventType.RUN_COMMAND,
                        UndineCommand.COMMAND + " " + c);
            }
            msg.addParts(button);

            msg.addText(" " + ChatColor.WHITE + Messages.get("HelpDescription_" + c));

            msg.send(sender);
        }

        // ugroupコマンドのヘルプ
        if ( sender.hasPermission(GroupCommand.PERMISSION + ".command") ) {

            MessageComponent msg = new MessageComponent();
            msg.addText(pre);

            String l = "[" + Messages.get("HelpCommand_group") + "]";
            MessageParts button = new MessageParts(l, ChatColor.AQUA);
            button.setClickEvent(ClickEventType.RUN_COMMAND, GroupCommand.COMMAND);
            msg.addParts(button);

            msg.addText(" " + ChatColor.WHITE + Messages.get("HelpDescription_group"));

            msg.send(sender);
        }

        // helpコマンドのヘルプ
        if ( sender.hasPermission(PERMISSION_PREFIX + "help") ) {
            MessageComponent msg = new MessageComponent();
            msg.addText(pre);

            String l = "[" + Messages.get("HelpCommand_help") + "]";
            MessageParts button = new MessageParts(l, ChatColor.AQUA);
            button.setClickEvent(ClickEventType.RUN_COMMAND,
                    UndineCommand.COMMAND + " help");
            msg.addParts(button);

            msg.addText(" " + ChatColor.WHITE + Messages.get("HelpDescription_help"));

            msg.send(sender);
        }

        sender.sendMessage(Messages.get("ListLastLine"));
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
