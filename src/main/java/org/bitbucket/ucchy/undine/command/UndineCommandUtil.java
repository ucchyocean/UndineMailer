/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine.command;

import java.util.ArrayList;

import org.bitbucket.ucchy.undine.Messages;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bitbucket.ucchy.undine.tellraw.ClickEventType;
import org.bitbucket.ucchy.undine.tellraw.MessageComponent;
import org.bitbucket.ucchy.undine.tellraw.MessageParts;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

/**
 * コマンド実行関連のユーティリティクラス
 * @author ucchy
 */
public class UndineCommandUtil {

    /**
     * アイテム表記から、ItemStackを作成して返す
     * @param desc アイテム表記
     * （マテリアル名、または、アイテムID。コロンを付けた後にデータ値を指定することも可能。
     *   例：WOOL, WOOL:3, 35, 35:6 ）
     * @return ItemStack
     */
    protected static ItemStack getItemStackFromDescription(String desc) {
        String[] descs = desc.split(":");
        if ( descs.length <= 0 ) return null;
        Material material = Material.getMaterial(descs[0].toUpperCase());
        if ( material == null && descs[0].matches("[0-9]{1,5}") ) {
            @SuppressWarnings("deprecation")
            Material m = Material.getMaterial(Integer.parseInt(descs[0]));
            material = m;
        }
        if ( material == null ) return null;
        ItemStack item = new ItemStack(material);
        if ( descs.length >= 2 && descs[1].matches("[0-9]{1,5}") ) {
            short durability = Short.parseShort(descs[1]);
            item.setDurability(durability);
        }
        return item;
    }

    /**
     * 宛先として有効な全てのプレイヤー名を取得する
     * @return 有効な宛先
     */
    protected static ArrayList<OfflinePlayer> getAllValidPlayers() {
        ArrayList<OfflinePlayer> players = new ArrayList<OfflinePlayer>();
        for ( OfflinePlayer player : Bukkit.getOfflinePlayers() ) {
            if ( player.hasPlayedBefore() || player.isOnline() ) {
                players.add(player);
            }
        }
        return players;
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

        msg.send(ms);
    }

    /**
     * アイテムを簡単な文字列表現にして返す
     * @param item アイテム
     * @return 文字列表現
     */
    protected static String getItemDesc(ItemStack item) {
        return item.getDurability() == 0 ? item.getType().toString() :
                item.getType().toString() + ":" + item.getDurability();
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
}
