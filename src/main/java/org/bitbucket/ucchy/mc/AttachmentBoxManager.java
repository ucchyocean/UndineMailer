/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.mc;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * 添付ボックス管理クラス
 * @author ucchy
 */
public class AttachmentBoxManager {

    private MagicMail parent;

    private HashMap<Player, Inventory> editmodeBoxes;
    private HashMap<Integer, Inventory> attachmentBoxes;

    /**
     * コンストラクタ
     * @param parent プラグイン
     */
    public AttachmentBoxManager(MagicMail parent) {
        this.parent = parent;
        editmodeBoxes = new HashMap<Player, Inventory>();
        attachmentBoxes = new HashMap<Integer, Inventory>();
    }

    /**
     * 指定されたプレイヤーに、そのプレイヤーの編集中ボックスを表示する
     * @param player プレイヤー
     */
    protected void displayEditmodeBox(Player player) {

        // 既に、該当プレイヤーの編集中ボックスインベントリがある場合は、そちらを表示する
        if ( editmodeBoxes.containsKey(player) ) {
            player.openInventory(editmodeBoxes.get(player));
            return;
        }

        // 添付ボックスの作成
        int size = parent.getMailCraftConfig().getAttachBoxSize() * 9;
        String title = Messages.get("EditmodeBoxTitle");
        // TODO: タイトルが32文字を超えたときの対応を入れるべきかどうか検討する
        Inventory box = Bukkit.createInventory(player, size, title);

        editmodeBoxes.put(player, box);
        player.openInventory(box);
    }

    /**
     * 指定されたプレイヤーの編集中ボックスを取得する
     * @param player プレイヤー
     * @return 編集中ボックス
     */
    protected Inventory getEditmodeBox(Player player) {

        if ( editmodeBoxes.containsKey(player) ) {
            return editmodeBoxes.get(player);
        }
        return null;
    }

    /**
     * 該当プレイヤーの編集中ボックスをクリアする
     * @param player プレイヤー
     */
    protected void clearEditmodeBox(Player player) {

        if ( editmodeBoxes.containsKey(player) ) {
            editmodeBoxes.remove(player);
        }
    }

    /**
     * 指定されたメールの添付ボックスを開いて確認する
     * @param player 確認する人
     * @param mail メール
     */
    protected void displayAttachmentBox(Player player, MailData mail) {

        // 既に、該当メールの添付ボックスインベントリがある場合は、そちらを表示する
        if ( attachmentBoxes.containsKey(mail.getIndex()) ) {
            player.openInventory(attachmentBoxes.get(mail.getIndex()));
            return;
        }

        // 添付ボックスの作成
        int size = 6 * 9;
        String title = Messages.get("AttachmentBoxTitle").replace("%number", mail.getIndex() + "");
        // TODO: タイトルが32文字を超えたときの対応を入れるべきかどうか検討する
        Inventory box = Bukkit.createInventory(player, size, title);

        // アイテムを追加
        for ( ItemStack item : mail.getAttachments() ) {
            box.addItem(item);
        }

        attachmentBoxes.put(mail.getIndex(), box);

        // 元のメールの添付ボックスはからにする
        mail.setAttachments(new ArrayList<ItemStack>());

        // 指定されたplayerの画面に添付ボックスを表示する
        player.openInventory(box);
    }
}
