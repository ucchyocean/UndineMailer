/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.undine.sender;

import java.util.List;
import java.util.UUID;

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

    private String nameOrUuid;
    private OfflinePlayer offline;

    /**
     * コンストラクタ
     * @param nameOrUuid プレイヤー名、または、"$"+UUID
     */
    public MailSenderPlayer(String nameOrUuid) {
        this.nameOrUuid = nameOrUuid;
    }

    /**
     * コンストラクタ
     * @param player プレイヤー
     */
    public MailSenderPlayer(OfflinePlayer player) {
        if ( Utility.isCB178orLater() ) {
            this.nameOrUuid = "$" + player.getUniqueId().toString();
        } else {
            this.nameOrUuid = player.getName();
        }
    }

    /**
     * オンラインかどうか
     * @return オンラインかどうか
     */
    @Override
    public boolean isOnline() {
        if ( offline == null ) {
            offline = getOfflinePlayer();
        }
        return offline.isOnline();
    }

    /**
     * 宛先として有効かどうか
     * @return 宛先として有効かどうか
     */
    @Override
    public boolean isValidDestination() {
        if ( offline == null ) {
            offline = getOfflinePlayer();
        }
        return offline.hasPlayedBefore() || offline.isOnline();
    }

    /**
     * プレイヤー名を返す
     * @return プレイヤー名
     * @see org.bitbucket.ucchy.undine.sender.MailSender#getName()
     */
    @Override
    public String getName() {
        if ( offline == null ) {
            offline = getOfflinePlayer();
        }
        return offline.getName();
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
    @SuppressWarnings("deprecation")
    @Override
    public OfflinePlayer getOfflinePlayer() {
        if ( offline != null ) return offline;
        if ( nameOrUuid.startsWith("$") ) {
            offline = Bukkit.getOfflinePlayer(UUID.fromString(nameOrUuid.substring(1)));
        } else {
            offline = Bukkit.getOfflinePlayer(nameOrUuid);
        }
        return offline;
    }

    /**
     * BukkitのPlayerを取得する
     * @return Player
     * @see org.bitbucket.ucchy.undine.sender.MailSender#getPlayer()
     */
    @Override
    public Player getPlayer() {
        if ( offline == null ) {
            offline = getOfflinePlayer();
        }
        return offline.getPlayer();
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
        if ( offline == null ) {
            offline = getOfflinePlayer();
        }
        return offline.isOp();
    }

    /**
     * 文字列のメタデータを設定する
     * @param key キー
     * @param value 値
     * @see org.bitbucket.ucchy.undine.sender.MailSender#setStringMetadata(java.lang.String, java.lang.String)
     */
    @Override
    public void setStringMetadata(String key, String value) {
        if ( offline == null ) {
            offline = getOfflinePlayer();
        }
        if ( !offline.isOnline() ) {
            return;
        }
        offline.getPlayer().setMetadata(key,
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
        if ( offline == null ) {
            offline = getOfflinePlayer();
        }
        if ( !offline.isOnline() ) {
            return null;
        }
        List<MetadataValue> values = offline.getPlayer().getMetadata(key);
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
        if ( offline == null ) {
            offline = getOfflinePlayer();
        }
        if ( !offline.isOnline() ) {
            return;
        }
        offline.getPlayer().setMetadata(key,
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
        if ( offline == null ) {
            offline = getOfflinePlayer();
        }
        if ( !offline.isOnline() ) {
            return false;
        }
        List<MetadataValue> values = offline.getPlayer().getMetadata(key);
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
        OfflinePlayer player = (OfflinePlayer)sender;
        if ( nameOrUuid.startsWith("$") ) {
            return nameOrUuid.equals("$" + player.getUniqueId().toString());
        } else {
            return nameOrUuid.equals(player.getName());
        }
    }

    /**
     * IDを返す
     * @return CB178以降なら "$" + UUID を返す、CB175以前ならIDを返す
     * @see org.bitbucket.ucchy.undine.sender.MailSender#toString()
     */
    @Override
    public String toString() {
        if ( !nameOrUuid.startsWith("$") && Utility.isCB178orLater() ) {
            @SuppressWarnings("deprecation")
            OfflinePlayer player = Bukkit.getOfflinePlayer(nameOrUuid);
            nameOrUuid = "$" + player.getUniqueId().toString();
        }
        return nameOrUuid;
    }
}
