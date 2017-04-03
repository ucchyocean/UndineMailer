/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.bitbucket.ucchy.undine.Messages;
import org.bitbucket.ucchy.undine.UndineMailer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import com.github.ucchyocean.messaging.tellraw.ClickEventType;
import com.github.ucchyocean.messaging.tellraw.MessageComponent;
import com.github.ucchyocean.messaging.tellraw.MessageParts;

/**
 * listコマンド
 * @author ucchy
 */
public class ListCommand implements TabExecutor {

    public static final String COMMAND_INDEX = "/uindex";
    protected static final String COMMAND_LIST = "/ulist";
    private static final String PERMISSION = "undine.list";
    private static final int PAGE_SIZE = 10;

    private UndineMailer parent;

    /**
     * コンストラクタ
     * @param parent
     */
    public ListCommand(UndineMailer parent) {
        this.parent = parent;
    }

    /**
     * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION) ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        // コマンドが利用不可になっていたら、エラーを表示して終了
        if ( !parent.getUndineConfig().isEnablePlayerList() ) {
            sender.sendMessage(Messages.get("ErrorInvalidCommand"));
            return true;
        }

        // パラメータが足りない場合はエラーを表示して終了
        if ( !label.equalsIgnoreCase("uindex") && args.length < 1 ) {
            sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "Prefix"));
            return true;
        }

        // 以下、プレイヤーリスト表示処理
        ArrayList<String> names = new ArrayList<String>(parent.getPlayerCache().keySet());
        Collections.sort(names, new Comparator<String>() {
            public int compare(String o1, String o2) {
                return o1.compareToIgnoreCase(o2);
            }
        });

        String parts = Messages.get("ListHorizontalParts");
        String pre = Messages.get("ListVerticalParts");

        // 空行を挿入する
        for ( int i=0; i<parent.getUndineConfig().getUiEmptyLines(); i++ ) {
            sender.sendMessage("");
        }


        if ( label.equalsIgnoreCase("uindex") ) {
            // インデクスの表示

            String next = "";
            for ( int i=0; i<args.length; i++ ) {
                next += " " + args[i];
            }

            ArrayList<String> indexes = new ArrayList<String>();
            for ( String name : names ) {
                String p = name.substring(0, 1).toUpperCase();
                if ( !indexes.contains(p) ) indexes.add(p);
            }

            String title = Messages.get("PlayerListIndexTitle");
            sender.sendMessage(parts + parts + " " + title + " " + parts + parts);

            int linenum = (int)((indexes.size() - 1) / 10) + 1;
            for ( int line=0; line<linenum; line++ ) {

                MessageComponent msg = new MessageComponent();
                msg.addText(pre);

                for ( int i=0; i<10; i++ ) {
                    int index = line * 10 + i;
                    if ( index >= indexes.size() ) break;
                    String p = indexes.get(index);
                    MessageParts button = new MessageParts("[" + p + "]",  ChatColor.AQUA);
                    button.setClickEvent(ClickEventType.RUN_COMMAND,
                            COMMAND_LIST + " " + p + " 1" + next);
                    msg.addParts(button);
                    msg.addText(" ");
                }

                msg.send(sender);
            }

            String returnCommand = (next.startsWith(" " + UndineCommand.COMMAND))
                    ? UndineCommand.COMMAND + " write"
                    : next.trim().replace("add", "detail");

            MessageComponent msg = new MessageComponent();
            msg.addText(parts + parts + " ");
            MessageParts button = new MessageParts(Messages.get("Return"), ChatColor.AQUA);
            button.setClickEvent(ClickEventType.RUN_COMMAND, returnCommand);
            msg.addParts(button);
            msg.addText(" " + parts + parts);
            msg.send(sender);

            return true;
        }


        // プレイヤーリストの表示

        String prefix = args[0];
        String next = "";
        int page = 1;
        if ( args.length >= 2 && args[1].matches("[0-9]{1,5}") ) {
            page = Integer.parseInt(args[1]);

            for ( int i=2; i<args.length; i++ ) {
                next += " " + args[i];
            }
        }

        ArrayList<String> list = new ArrayList<String>();
        for ( String n : names ) {
            if ( n.toUpperCase().startsWith(prefix) ) list.add(n);
        }
        int max = (int)((list.size() - 1) / PAGE_SIZE) + 1;

        String title = Messages.get("PlayerListTitle", "%pre", prefix);
        sender.sendMessage(parts + parts + " " + title + " " + parts + parts);

        for ( int i=0; i<PAGE_SIZE; i++ ) {

            int index = (page - 1) * PAGE_SIZE + i;
            if ( index < 0 || list.size() <= index ) {
                continue;
            }

            String name = list.get(index);
            MessageComponent msg = new MessageComponent();
            msg.addText(pre);
            MessageParts button = new MessageParts("[" + name + "]", ChatColor.AQUA);
            if ( next.length() > 0 ) {
                button.setClickEvent(ClickEventType.RUN_COMMAND, next.trim() + " " + name);
            }
            msg.addParts(button);
            msg.send(sender);
        }

        sendPager(sender, COMMAND_LIST + " " + prefix, page, max, next);

        return true;
    }

    /**
     * @see org.bukkit.command.TabCompleter#onTabComplete(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public List<String> onTabComplete(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
        return null;
    }

    /**
     * ページャーを対象プレイヤーに表示する
     * @param sender 表示対象
     * @param commandPre 最後に実行するコマンド
     * @param page 現在のページ
     * @param max 最終ページ
     * @param next 最後に実行するコマンド
     */
    private void sendPager(CommandSender sender, String commandPre, int page, int max, String next) {

        String returnLabel = Messages.get("Return");
        String firstLabel = Messages.get("FirstPage");
        String prevLabel = Messages.get("PrevPage");
        String nextLabel = Messages.get("NextPage");
        String lastLabel = Messages.get("LastPage");
        String returnToolTip = Messages.get("ReturnToolTip");
        String firstToolTip = Messages.get("FirstPageToolTip");
        String prevToolTip = Messages.get("PrevPageToolTip");
        String nextToolTip = Messages.get("NextPageToolTip");
        String lastToolTip = Messages.get("LastPageToolTip");
        String parts = Messages.get("ListHorizontalParts");

        MessageComponent msg = new MessageComponent();

        msg.addText(parts + " ");

        MessageParts returnButton = new MessageParts(returnLabel, ChatColor.AQUA);
        returnButton.setClickEvent(ClickEventType.RUN_COMMAND, COMMAND_INDEX + next);
        returnButton.setHoverText(returnToolTip);
        msg.addParts(returnButton);

        msg.addText(" ");

        if ( page > 1 ) {
            MessageParts firstButton = new MessageParts(
                    firstLabel, ChatColor.AQUA);
            firstButton.setClickEvent(ClickEventType.RUN_COMMAND, commandPre + " 1" + next);
            firstButton.setHoverText(firstToolTip);
            msg.addParts(firstButton);

            msg.addText(" ");

            MessageParts prevButton = new MessageParts(
                    prevLabel, ChatColor.AQUA);
            prevButton.setClickEvent(ClickEventType.RUN_COMMAND, commandPre + " " + (page - 1) + next);
            prevButton.setHoverText(prevToolTip);
            msg.addParts(prevButton);

        } else {
            msg.addText(firstLabel + " " + prevLabel, ChatColor.WHITE);
        }

        msg.addText(" (" + page + "/" + max + ") ");

        if ( page < max ) {
            MessageParts nextButton = new MessageParts(
                    nextLabel, ChatColor.AQUA);
            nextButton.setClickEvent(ClickEventType.RUN_COMMAND, commandPre + " " + (page + 1) + next);
            nextButton.setHoverText(nextToolTip);
            msg.addParts(nextButton);

            msg.addText(" ");

            MessageParts lastButton = new MessageParts(
                    lastLabel, ChatColor.AQUA);
            lastButton.setClickEvent(ClickEventType.RUN_COMMAND, commandPre + " " + max + next);
            lastButton.setHoverText(lastToolTip);
            msg.addParts(lastButton);

        } else {
            msg.addText(nextLabel + " " + lastLabel, ChatColor.WHITE);
        }

        msg.addText(" " + parts);

        msg.send(sender);
    }
}
