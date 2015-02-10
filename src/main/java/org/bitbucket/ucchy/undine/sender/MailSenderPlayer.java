/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.undine.sender;

import java.util.List;

import org.bitbucket.ucchy.undine.UndineMailer;
import org.bitbucket.ucchy.undine.Utility;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

/**
 * プレイヤー
 * @author ucchy
 */
public class MailSenderPlayer extends MailSender {

    private OfflinePlayer sender;

    /**
     * コンストラクタ
     * @param sender プレイヤー
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
     * 宛先として有効かどうか
     * @return 宛先として有効かどうか
     */
    @Override
    public boolean isValidDestination() {
        return sender.hasPlayedBefore() || sender.isOnline();
    }

    /**
     * プレイヤー名を返す
     * @return プレイヤー名
     * @see org.bitbucket.ucchy.undine.sender.MailSender#getName()
     */
    @Override
    public String getName() {
        return sender.getName();
    }

    /**
     * プレイヤー表示名を返す
     * @return プレイヤー表示名
     * @see org.bitbucket.ucchy.undine.sender.MailSender#getDisplayName()
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
     * @param message メッセージ
     * @see org.bitbucket.ucchy.undine.sender.MailSender#sendMessage(java.lang.String)
     */
    @Override
    public void sendMessage(String message) {
        Player player = getPlayer();
        if ( player != null ) {
            player.sendMessage(message);
        }
    }

    /**
     * BukkitのOfflinePlayerを取得する。
     * @return OfflinePlayer
     * @see org.bitbucket.ucchy.undine.sender.MailSender#getOfflinePlayer()
     */
    @Override
    public OfflinePlayer getOfflinePlayer() {
        return sender;
    }

    /**
     * BukkitのPlayerを取得する
     * @return Player
     * @see org.bitbucket.ucchy.undine.sender.MailSender#getPlayer()
     */
    @Override
    public Player getPlayer() {
        return sender.getPlayer();
    }

    /**
     * 発言者が今いるワールドのワールド名を取得する
     * @return ワールド名
     * @see org.bitbucket.ucchy.undine.sender.MailSender#getWorldName()
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
     * @see org.bitbucket.ucchy.undine.sender.MailSender#hasPermission(java.lang.String)
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
     * OPかどうかを調べる
     * @return OPかどうか
     * @see org.bitbucket.ucchy.undine.sender.MailSender#isOp()
     */
    @Override
    public boolean isOp() {
        return sender.isOp();
    }

    /**
     * 文字列のメタデータを設定する
     * @param key キー
     * @param value 値
     * @see org.bitbucket.ucchy.undine.sender.MailSender#setStringMetadata(java.lang.String, java.lang.String)
     */
    @Override
    public void setStringMetadata(String key, String value) {
        if ( !sender.isOnline() ) {
            return;
        }
        sender.getPlayer().setMetadata(key,
                new FixedMetadataValue(UndineMailer.getInstance(), value));
    }

    /**
     * 文字列のメタデータを取得する
     * @param key キー
     * @return 値
     * @see org.bitbucket.ucchy.undine.sender.MailSender#getStringMetadata(java.lang.String)
     */
    @Override
    public String getStringMetadata(String key) {
        if ( !sender.isOnline() ) {
            return null;
        }
        List<MetadataValue> values = sender.getPlayer().getMetadata(key);
        if ( values.size() == 0 ) {
            return null;
        }
        return values.get(0).asString();
    }

    /**
     * 真偽値のメタデータを設定する
     * @param key キー
     * @param value 値
     * @see org.bitbucket.ucchy.undine.sender.MailSender#setBooleanMetadata(java.lang.String, boolean)
     */
    @Override
    public void setBooleanMetadata(String key, boolean value) {
        if ( !sender.isOnline() ) {
            return;
        }
        sender.getPlayer().setMetadata(key,
                new FixedMetadataValue(UndineMailer.getInstance(), value));
    }

    /**
     * 真偽値のメタデータを取得する
     * @param key キー
     * @return 値
     * @see org.bitbucket.ucchy.undine.sender.MailSender#getBooleanMetadata(java.lang.String)
     */
    @Override
    public boolean getBooleanMetadata(String key) {
        if ( !sender.isOnline() ) {
            return false;
        }
        List<MetadataValue> values = sender.getPlayer().getMetadata(key);
        if ( values.size() == 0 ) {
            return false;
        }
        return values.get(0).asBoolean();

    }

    /**
     * 指定されたCommandSenderと同一かどうかを返す
     * @param sender
     * @return 同一かどうか
     * @see org.bitbucket.ucchy.undine.sender.MailSender#equals(org.bukkit.command.CommandSender)
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
     * @see org.bitbucket.ucchy.undine.sender.MailSender#toString()
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
