/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine;

import java.util.ArrayList;
import java.util.HashMap;

import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * 添付ボックス管理クラス
 * @author ucchy
 */
public class AttachmentBoxManager {

    private UndineMailer parent;

    private HashMap<Player, Inventory> editmodeBoxes;
    private HashMap<Integer, Inventory> attachmentBoxes;
    private HashMap<Player, MailData> indexCache;

    /**
     * コンストラクタ
     * @param parent プラグイン
     */
    public AttachmentBoxManager(UndineMailer parent) {
        this.parent = parent;
        editmodeBoxes = new HashMap<Player, Inventory>();
        attachmentBoxes = new HashMap<Integer, Inventory>();
        indexCache = new HashMap<Player, MailData>();
    }

    /**
     * 指定されたプレイヤーに、そのプレイヤーの編集中ボックスを表示する
     * @param player プレイヤー
     * @param インベントリ名
     */
    private void displayEditmodeBox(Player player) {

        // 既に、該当プレイヤーの編集中ボックスインベントリがある場合は、そちらを表示する
        if ( editmodeBoxes.containsKey(player) ) {
            player.openInventory(editmodeBoxes.get(player));
            return;
            //return editmodeBoxes.get(player).getName();
        }

        // 添付ボックスの作成
        int size = parent.getUndineConfig().getAttachBoxSize() * 9;
        String title = Messages.get("EditmodeBoxTitle");

        // インベントリタイトルに32文字以上は設定できないので、必要に応じて削る
        if ( title.length() > 32 ) {
            title = title.substring(0, 32);
        }

        Inventory box = Bukkit.createInventory(player, size, title);

        // アイテムの追加
        MailSender sender = MailSender.getMailSender(player);
        MailData mail = parent.getMailManager().getEditmodeMail(sender);
        for ( ItemStack item : mail.getAttachments() ) {
            box.addItem(item);
        }

        editmodeBoxes.put(player, box);
        player.openInventory(box);
        //return box.getName();
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
    public void clearEditmodeBox(Player player) {

        if ( editmodeBoxes.containsKey(player) ) {
            editmodeBoxes.get(player).clear();
            editmodeBoxes.remove(player);
        }
    }

    /**
     * 指定されたメールの添付ボックスを開いて確認する
     * @param player 確認する人
     * @param mail メール
     * @param インベントリ名
     */
    private void displayAttachmentBox(Player player, MailData mail) {

        // 既に、該当メールの添付ボックスインベントリがある場合は、そちらを表示する
        if ( attachmentBoxes.containsKey(mail.getIndex()) ) {
            player.openInventory(attachmentBoxes.get(mail.getIndex()));
            return;
            //return attachmentBoxes.get(mail.getIndex()).getName();
        }

        // 添付ボックスの作成
        int size = (int)((mail.getAttachments().size() - 1) / 9 + 1) * 9;
        String title = Messages.get("AttachmentBoxTitle", "%number", mail.getIndex());

        // インベントリタイトルに32文字以上は設定できないので、必要に応じて削る
        if ( title.length() > 32 ) {
            title = title.substring(0, 32);
        }

        Inventory box = Bukkit.createInventory(player, size, title);

        // アイテムを追加
        for ( ItemStack item : mail.getAttachments() ) {
            box.addItem(item);
        }

        attachmentBoxes.put(mail.getIndex(), box);

        // 指定されたplayerの画面に添付ボックスを表示する
        player.openInventory(box);

        //return box.getName();
    }

    /**
     * 指定されたメールの添付ボックスを、指定されたプレイヤーの画面に表示する
     * @param player 確認する人
     * @param mail メール
     */
    public void displayAttachBox(Player player, MailData mail) {

        if ( !mail.isSent() ) {
            displayEditmodeBox(player);
        } else {
            displayAttachmentBox(player, mail);
            mail.setOpenAttachments();
        }

        // メールのインデクスを記録しておく
        indexCache.put(player, mail);
    }

    /**
     * 指定したプレイヤーが、添付ボックスを開いている状態かどうかを返す
     * @param player プレイヤー
     * @return 添付ボックスを開いているかどうか
     * （編集メールのボックスを含まない）
     */
    protected boolean isOpeningAttachBox(Player player) {
        return indexCache.containsKey(player)
                && indexCache.get(player).isSent();
    }

    /**
     * 指定したプレイヤーが、編集メールの添付ボックスを開いている状態かどうかを返す
     * @param player プレイヤー
     * @return 編集メールの添付ボックスを開いているかどうか
     * （送信済みメールのボックスを含まない）
     */
    protected boolean isOpeningEditmodeBox(Player player) {
        return indexCache.containsKey(player)
                && !indexCache.get(player).isSent();
    }

    /**
     * 指定されたプレイヤーが開いていた添付ボックスを、メールと同期する
     * @param player プレイヤー
     */
    protected void syncAttachBox(Player player) {

        // 開いていたボックスのインデクスが記録されていないなら、何もしない
        if ( !indexCache.containsKey(player) ) return;

        // 同期するボックスとメールを取得する
        MailData mail = indexCache.get(player);

        // インデクスを削除する
        indexCache.remove(player);

        Inventory inv;
        if ( !mail.isSent() ) {
            inv = editmodeBoxes.get(player);
        } else {
            inv = attachmentBoxes.get(mail.getIndex());
        }

        // 一旦取り出して再度挿入することで、アイテムをスタックして整理する
        ArrayList<ItemStack> temp = new ArrayList<ItemStack>();
        for ( ItemStack item : inv.getContents() ) {
            if ( item != null && item.getType() != Material.AIR ) {
                temp.add(item);
            }
        }
        inv.clear();
        for ( ItemStack item : temp ) {
            inv.addItem(item);
        }

        // ArrayListへ再配置
        ArrayList<ItemStack> array = new ArrayList<ItemStack>();
        for ( ItemStack item : inv.getContents() ) {
            if ( item != null && item.getType() != Material.AIR ) {
                array.add(item);
            }
        }

        // 同期して保存する
        mail.setAttachments(array);
        parent.getMailManager().saveMail(mail);

        // メール詳細を開く
        if ( player.isOnline() ) {
            if ( !mail.isSent() ) {
                parent.getMailManager().displayEditmode(
                        MailSender.getMailSender(player));
            } else {
                parent.getMailManager().displayMail(
                        MailSender.getMailSender(player), mail);
            }
        }
    }

    /**
     * 開いたままになっているインベントリを強制的に全て閉じ、同期を実行する
     */
    protected void closeAllBox() {
        for ( Player player : indexCache.keySet() ) {
            player.closeInventory();
            syncAttachBox(player);
        }
    }
}

