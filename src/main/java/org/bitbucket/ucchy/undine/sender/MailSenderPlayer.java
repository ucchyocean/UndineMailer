/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.undine.sender;

import java.util.List;

import org.bitbucket.ucchy.undine.UndineMailer;
import org.bitbucket.ucchy.undine.Utility;
import org.bitbucket.ucchy.undine.bridge.PCGFPluginLibBridge;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
        String uuid = UndineMailer.getInstance().getUUID(player.getName());
        this.nameOrUuid = "$" + uuid;
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
            //offline = Bukkit.getOfflinePlayer(UUID.fromString(nameOrUuid.substring(1)));
            String name = PCGFPluginLibBridge.getNameFromUUID(nameOrUuid.substring(1));
            offline = Bukkit.getOfflinePlayer(name);
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
     * 発言者が今いる地点を取得する
     * @return 地点
     * @see org.bitbucket.ucchy.undine.sender.MailSender#getLocation()
     */
    @Override
    public Location getLocation() {
        Player player = getPlayer();
        if ( player != null ) {
            return player.getLocation();
        }
        return null;
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
        Player player = offline.getPlayer();
        if ( !offline.isOnline() || player == null ) {
            return;
        }
        player.setMetadata(key,
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
        Player player = offline.getPlayer();
        if ( !offline.isOnline() || player == null ) {
            return null;
        }
        List<MetadataValue> values = player.getMetadata(key);
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
        Player player = offline.getPlayer();
        if ( !offline.isOnline() || player == null ) {
            return;
        }
        player.setMetadata(key,
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
        Player player = offline.getPlayer();
        if ( !offline.isOnline() || player == null ) {
            return false;
        }
        List<MetadataValue> values = player.getMetadata(key);
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
            return nameOrUuid.equals("$" + UndineMailer.getInstance().getUUID(player.getName()));
        } else {
            return nameOrUuid.equals(player.getName());
        }
    }

    /**
     * このMailSenderPlayerのUUIDが、PlayerNameUuidCacheにキャッシュされているかどうかを返す
     * @return キャッシュされているかどうか
     */
    public boolean isUuidCached() {
        upgrade();
        if ( !nameOrUuid.startsWith("$") ) return false;
        String uuid = nameOrUuid.substring(1);
        return UndineMailer.getInstance().getPlayerUuids().contains(uuid);
    }

    /**
     * IDを返す
     * @return CB178以降なら "$" + UUID を返す、CB175以前ならIDを返す
     * @see org.bitbucket.ucchy.undine.sender.MailSender#toString()
     */
    @Override
    public String toString() {
        upgrade();
        return nameOrUuid;
    }

    /**
     * データのアップグレードを行う。
     * @return アップグレードを実行したかどうか
     */
    public boolean upgrade() {

        // CB1.7.5 以前なら、何もしない
        if ( !Utility.isCB178orLater() ) return false;

        // nameOrUuidが $ から始まる文字列なら、アップグレード済みなので何もしない
        if ( nameOrUuid.startsWith("$") ) return false;

        // nameOrUuidを、$ + UUID に変更する
        String uuid = UndineMailer.getInstance().getUUID(nameOrUuid);
        if ( uuid.equals("") ) return false;
        nameOrUuid = "$" + uuid;
        return true;
    }
}
