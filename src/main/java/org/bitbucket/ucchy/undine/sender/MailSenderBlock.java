/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.undine.sender;

import java.util.List;

import org.bitbucket.ucchy.undine.UndineMailer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

/**
 * コマンドブロック
 * @author ucchy
 */
public class MailSenderBlock extends MailSender {

    BlockCommandSender sender;

    /**
     * コンストラクタ
     * @param sender コマンドブロック
     */
    public MailSenderBlock(BlockCommandSender sender) {
        this.sender = sender;
    }

    /**
     * オンラインかどうか
     * @return 常にtrue
     * @see org.bitbucket.ucchy.undine.sender.MailSender#isOnline()
     */
    @Override
    public boolean isOnline() {
        return true;
    }

    /**
     * 宛先として有効かどうか
     * @return 常にfalse
     */
    @Override
    public boolean isValidDestination() {
        return false;
    }

    /**
     * プレイヤー名を返す
     * @return プレイヤー名
     * @see org.bitbucket.ucchy.undine.sender.MailSender#getName()
     */
    @Override
    public String getName() {
        if ( sender == null ) return "@";
        return sender.getName();
    }

    /**
     * プレイヤー表示名を返す
     * @return プレイヤー表示名
     * @see org.bitbucket.ucchy.undine.sender.MailSender#getDisplayName()
     */
    @Override
    public String getDisplayName() {
        if ( sender == null ) return "@";
        return sender.getName();
    }

    /**
     * メッセージを送る、実際は何もせずにメッセージを捨てる
     * @param message メッセージ
     * @see org.bitbucket.ucchy.undine.sender.MailSender#sendMessage(java.lang.String)
     */
    @Override
    public void sendMessage(String message) {
        // do nothing.
    }

    /**
     * BukkitのOfflinePlayerを取得する。
     * @return 常にnullが返される
     * @see org.bitbucket.ucchy.undine.sender.MailSender#getOfflinePlayer()
     */
    @Override
    public OfflinePlayer getOfflinePlayer() {
        return null;
    }

    /**
     * BukkitのPlayerを取得する
     * @return 常にnullが返される
     * @see org.bitbucket.ucchy.undine.sender.MailSender#getPlayer()
     */
    @Override
    public Player getPlayer() {
        return null;
    }

    /**
     * 発言者が今いるワールドのワールド名を取得する
     * @return コマンドブロックが配置されているワールド名が返される。
     * @see org.bitbucket.ucchy.undine.sender.MailSender#getWorldName()
     */
    @Override
    public String getWorldName() {
        if ( sender == null || sender.getBlock() == null ) return "";
        return sender.getBlock().getWorld().getName();
    }

    /**
     * 発言者が今いる地点を取得する
     * @return 地点
     * @see org.bitbucket.ucchy.undine.sender.MailSender#getLocation()
     */
    @Override
    public Location getLocation() {
        if ( sender == null || sender.getBlock() == null ) return null;
        return sender.getBlock().getLocation();
    }

    /**
     * 指定されたパーミッションノードの権限を持っているかどうかを取得する
     * @param node パーミッションノード
     * @return 常にtrue
     * @see org.bitbucket.ucchy.undine.sender.MailSender#hasPermission(java.lang.String)
     */
    @Override
    public boolean hasPermission(String node) {
        return true;
    }

    /**
     * OPかどうかを調べる
     * @return 常にtrue
     * @see org.bitbucket.ucchy.undine.sender.MailSender#isOp()
     */
    @Override
    public boolean isOp() {
        return true;
    }

    /**
     * 文字列のメタデータを設定する
     * @param key キー
     * @param value 値
     * @see org.bitbucket.ucchy.undine.sender.MailSender#setStringMetadata(java.lang.String, java.lang.String)
     */
    @Override
    public void setStringMetadata(String key, String value) {
        if ( sender == null || sender.getBlock() == null
                || sender.getBlock().getType() != Material.COMMAND ) {
            return;
        }
        sender.getBlock().setMetadata(key,
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
        if ( sender == null || sender.getBlock() == null
                || sender.getBlock().getType() != Material.COMMAND ) {
            return null;
        }
        List<MetadataValue> values = sender.getBlock().getMetadata(key);
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
        if ( sender == null || sender.getBlock() == null
                || sender.getBlock().getType() != Material.COMMAND ) {
            return;
        }
        sender.getBlock().setMetadata(key,
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
        if ( sender == null || sender.getBlock() == null
                || sender.getBlock().getType() != Material.COMMAND ) {
            return false;
        }
        List<MetadataValue> values = sender.getBlock().getMetadata(key);
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
        if ( sender == null ) return false;
        return this.sender.equals(sender);
    }

    /**
     * IDを返す
     * @return 名前をそのまま返す
     * @see org.bitbucket.ucchy.undine.sender.MailSender#toString()
     */
    @Override
    public String toString() {
        return getName();
    }
}
