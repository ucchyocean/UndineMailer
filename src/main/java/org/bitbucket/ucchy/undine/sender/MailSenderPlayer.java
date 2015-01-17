/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.undine.sender;

import org.bitbucket.ucchy.undine.Utility;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * プレイヤー
 * @author ucchy
 */
public class MailSenderPlayer extends MailSender {

    private OfflinePlayer sender;

    /**
     * コンストラクタ
     * @param player プレイヤー
     */
    public MailSenderPlayer(OfflinePlayer sender) {
        this.sender = sender;
    }

    /**
     * オンラインかどうか
     * @return オンラインかどうか
     */
    @Override
    public boolean isOnline() {
        return sender.isOnline();
    }

    /**
     * プレイヤー名を返す
     * @return プレイヤー名
     * @see com.MailSender.ucchyocean.lc.channel.ChannelPlayer#getName()
     */
    @Override
    public String getName() {
        return sender.getName();
    }

    /**
     * プレイヤー表示名を返す
     * @return プレイヤー表示名
     * @see com.MailSender.ucchyocean.lc.channel.ChannelPlayer#getDisplayName()
     */
    @Override
    public String getDisplayName() {
        Player player = getPlayer();
        if ( player != null ) {
            return player.getDisplayName();
        }
        return getName();
    }

    /**
     * メッセージを送る
     * @param message 送るメッセージ
     * @see com.MailSender.ucchyocean.lc.channel.ChannelPlayer#sendMessage(java.lang.String)
     */
    @Override
    public void sendMessage(String message) {
        Player player = getPlayer();
        if ( player != null ) {
            player.sendMessage(message);
        }
    }

    /**
     * BukkitのPlayerを取得する
     * @return Player
     * @see com.MailSender.ucchyocean.lc.channel.ChannelPlayer#getPlayer()
     */
    @Override
    public Player getPlayer() {
        return sender.getPlayer();
    }

    /**
     * 発言者が今いるワールドのワールド名を取得する
     * @return ワールド名
     * @see com.MailSender.ucchyocean.lc.channel.ChannelPlayer#getWorldName()
     */
    @Override
    public String getWorldName() {
        Player player = getPlayer();
        if ( player != null ) {
            return player.getWorld().getName();
        }
        return "-";
    }

    /**
     * 指定されたパーミッションノードの権限を持っているかどうかを取得する
     * @param node パーミッションノード
     * @return 権限を持っているかどうか
     * @see com.MailSender.ucchyocean.lc.channel.ChannelPlayer#hasPermission(java.lang.String)
     */
    @Override
    public boolean hasPermission(String node) {
        Player player = getPlayer();
        if ( player == null ) {
            return false;
        } else {
            return player.hasPermission(node);
        }
    }

    /**
     * 指定されたCommandSenderと同一かどうかを返す
     * @param sender
     * @return 同一かどうか
     * @see com.MailSender.ucchyocean.lc.channel.ChannelPlayer#equals(org.bukkit.entity.Player)
     */
    @Override
    public boolean equals(CommandSender sender) {
        if ( sender == null || !(sender instanceof OfflinePlayer) ) {
            return false;
        }
        return this.sender.getName().equals(sender.getName());
    }

    /**
     * IDを返す
     * @return CB178移行なら "$" + UUID を返す、CB175以前ならIDを返す
     * @see com.MailSender.ucchyocean.lc.channel.ChannelPlayer#getID()
     */
    @Override
    public String toString() {
        if ( Utility.isCB178orLater() ) {
            return "$" + sender.getUniqueId().toString();
        }
        return sender.getName();
    }

    /**
     * 名前からSenderを生成して返す
     * @param name 名前
     * @return Sender
     */
    public static MailSenderPlayer fromName(String name) {
        @SuppressWarnings("deprecation")
        OfflinePlayer player = Bukkit.getPlayerExact(name);
        if ( player != null ) {
            return new MailSenderPlayer(player);
        }
        @SuppressWarnings("deprecation")
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        if ( offline != null ) {
            return new MailSenderPlayer(offline);
        }
        return null;
    }
}
