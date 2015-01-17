/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Undineのリスナークラス
 * @author ucchy
 */
public class UndineListener implements Listener {

    private Undine parent;

    /**
     * コンストラクタ
     * @param parent プラグイン
     */
    public UndineListener(Undine parent) {
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

        // 監視対象のメタデータが入っていないなら、無視する
        if ( !player.hasMetadata(AttachmentBoxManager.BOX_INV_META_NAME) ) return;

        // インベントリを同期する
        parent.getBoxManager().syncAttachBox(player);

        // メタデータを削除する
        player.removeMetadata(AttachmentBoxManager.BOX_INV_META_NAME, parent);
    }

    /**
     * プレイヤーがサーバーに参加した時に呼び出されるメソッド
     * @param event
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        // TODO 未読のメールを表示する
    }
}
