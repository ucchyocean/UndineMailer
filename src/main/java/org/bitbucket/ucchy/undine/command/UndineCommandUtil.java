/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine.command;

import org.bitbucket.ucchy.undine.Messages;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bitbucket.ucchy.undine.sender.MailSenderConsole;
import org.bitbucket.ucchy.undine.sender.MailSenderPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.github.ucchyocean.messaging.tellraw.ClickEventType;
import com.github.ucchyocean.messaging.tellraw.MessageComponent;
import com.github.ucchyocean.messaging.tellraw.MessageParts;

/**
 * コマンド実行関連のユーティリティクラス
 * @author ucchy
 */
public class UndineCommandUtil {

    /**
     * アイテム表記から、ItemStackを作成して返す
     * @param desc アイテム表記
     * （マテリアル名。コロンを付けた後にデータ値を指定することも可能。
     *   例：WOOL, WOOL:3）
     * @return ItemStack
     */
    protected static ItemStack getItemStackFromDescription(String desc) {
        String[] descs = desc.split(":");
        if ( descs.length <= 0 ) return null;
        Material material = Material.getMaterial(descs[0].toUpperCase());
        if ( material == null ) return null;
        ItemStack item = new ItemStack(material);
        if ( descs.length >= 2 && descs[1].matches("[0-9]{1,5}") ) {
            short durability = Short.parseShort(descs[1]);
            item.setDurability(durability);
        }
        return item;
    }

    /**
     * OKボタンとキャンセルボタンと拒否ボタンを表示して、
     * 確認をとるtellrawメッセージを表示する。
     * @param ms メッセージ送信先
     * @param okCommand OKボタンを押した時に実行するコマンド
     * @param cancelCommand キャンセルボタンを押した時に実行するコマンド
     */
    protected static void showOKCancelButton(
            MailSender ms, String okCommand, String cancelCommand) {

        MessageComponent msg = new MessageComponent();
        msg.addText("     ");
        MessageParts buttonOK = new MessageParts(
                Messages.get("ButtonOK"), ChatColor.AQUA);
        buttonOK.setClickEvent(ClickEventType.RUN_COMMAND, okCommand);
        msg.addParts(buttonOK);
        msg.addText("     ");
        MessageParts buttonCancel = new MessageParts(
                Messages.get("ButtonCancel"), ChatColor.AQUA);
        buttonCancel.setClickEvent(ClickEventType.RUN_COMMAND, cancelCommand);
        msg.addParts(buttonCancel);

        sendMessageComponent(msg, ms);
    }

    /**
     * アイテムを簡単な文字列表現にして返す
     * @param item アイテム
     * @return 文字列表現
     */
    protected static String getItemDesc(ItemStack item) {
//        return item.getDurability() == 0 ? item.getType().toString() :
//                item.getType().toString() + ":" + item.getDurability();
//        return item.serialize().toString();
        return item != null ? item.getType().toString() : "null";
    }

    /**
     * 正の小数付き数値を返す。Doubleにパース不可の場合は、-1が返される。
     * @param value 変換対象
     * @return 変換後の値。doubleでない場合は-1が返される
     */
    protected static double tryParseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch(NumberFormatException e) {
            return -1;
        }
    }

    /**
     * 指定されたメッセージコンポーネントを、指定されたMailSenderに送信する。
     * @param msg メッセージコンポーネント
     * @param sender 送信先
     */
    private static void sendMessageComponent(MessageComponent msg, MailSender sender) {
        if ( sender instanceof MailSenderPlayer && sender.isOnline() ) {
            msg.send(sender.getPlayer());
        } else if ( sender instanceof MailSenderConsole ) {
            msg.send(Bukkit.getConsoleSender());
        }
    }
}
