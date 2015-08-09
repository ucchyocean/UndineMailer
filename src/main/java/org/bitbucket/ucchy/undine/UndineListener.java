/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine;

import java.util.ArrayList;
import java.util.List;

import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bitbucket.ucchy.undine.sender.MailSenderConsole;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Undineのリスナークラス
 * @author ucchy
 */
public class UndineListener implements Listener {

    private UndineMailer parent;

    /**
     * コンストラクタ
     * @param parent プラグイン
     */
    public UndineListener(UndineMailer parent) {
        this.parent = parent;
    }

    /**
     * プレイヤーがインベントリを閉じた時に呼び出されるメソッド
     * @param event
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {

        if ( !(event.getPlayer() instanceof Player) ) return;

        Player player = (Player)event.getPlayer();

        // インベントリを同期する
        parent.getBoxManager().syncAttachBox(player);
    }

    /**
     * プレイヤーがサーバーに参加した時に呼び出されるメソッド
     * @param event
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();
        UndineConfig config = parent.getUndineConfig();

        // プレイヤーキャッシュを更新する
        parent.getPlayerCache().put(player.getName(), MailSender.getMailSender(player));

        // MailManagerのロードが完了していないなら、以降は何もしない
        if ( !parent.getMailManager().isLoaded() ) {
            return;
        }

        final MailSender sender = MailSender.getMailSender(player);

        // 未読のメールを遅れて表示する
        int delay = config.getLoginNotificationDelaySeconds();
        new BukkitRunnable() {
            public void run() {
                parent.getMailManager().displayUnreadOnJoin(sender);
            }
        }.runTaskLaterAsynchronously(parent, delay * 20);

        // 新規プレイヤーの場合は、ウェルカムメールを送る
        if ( !player.hasPlayedBefore() && config.isUseWelcomeMail() ) {
            MailSender from = MailSenderConsole.getMailSenderConsole();
            List<MailSender> to = new ArrayList<MailSender>();
            to.add(sender);
            List<String> message = new ArrayList<String>();
            for ( String msg : Messages.get("WelcomeMailBody").split("\\n") ) {
                message.add(msg);
            }
            List<ItemStack> attachments = cloneItemStackList(config.getWelcomeMailAttachments());
            final MailData mail = new MailData(to, from, message, attachments);
            int welcomeDelay = config.getWelcomeMailDelaySeconds();
            new BukkitRunnable() {
                public void run() {
                    parent.getMailManager().sendNewMail(mail);
                }
            }.runTaskLaterAsynchronously(parent, welcomeDelay * 20);
        }
    }

    /**
     * プレイヤーがインベントリ内をクリックした時に呼び出されるメソッド
     * @param event
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        Player player = (Player)event.getWhoClicked();

        // 添付アイテムボックスのインベントリでなければ、何もしない
        if ( !parent.getBoxManager().isOpeningAttachBox(player) ) return;

        int size = event.getInventory().getSize();
        boolean inside = (event.getRawSlot() < size);

        // 添付ボックス内へのアイテム配置を禁止する(取り出し専用にする)
        switch ( event.getAction() ) {
        case PLACE_ALL:
        case PLACE_ONE:
        case PLACE_SOME:
        case SWAP_WITH_CURSOR:
            if ( inside ) {
                event.setCancelled(true);
            }
            return;
        case MOVE_TO_OTHER_INVENTORY:
            if ( !inside ) {
                event.setCancelled(true);
            }
            return;
        case HOTBAR_SWAP:
        case HOTBAR_MOVE_AND_READD:
            event.setCancelled(true);
            return;
        default:
            return;
        }
    }

    /**
     * プレイヤーがインベントリ内をドラッグした時に呼び出されるメソッド
     * @param event
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {

        Player player = (Player)event.getWhoClicked();

        // 添付アイテムボックスのインベントリでなければ、何もしない
        if ( !parent.getBoxManager().isOpeningAttachBox(player) ) return;

        // 添付ボックス内へのアイテム配置を禁止する(取り出し専用にする)
        int size = event.getInventory().getSize();
        for ( int i : event.getRawSlots() ) {
            if ( i < size ) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * ItemStackのディープコピーを作成して返す
     * @param org
     * @return
     */
    private static List<ItemStack> cloneItemStackList(List<ItemStack> org) {
        List<ItemStack> list = new ArrayList<ItemStack>();
        for ( ItemStack item : org ) {
            list.add(item.clone());
        }
        return list;
    }
}
