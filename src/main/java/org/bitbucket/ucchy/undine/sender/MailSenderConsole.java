/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.undine.sender;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

/**
 * コンソール
 * @author ucchy
 */
public class MailSenderConsole extends MailSender {

    private ConsoleCommandSender sender;

    private static HashMap<String, String> strMetadata;
    private static HashMap<String, Boolean> blnMetadata;

    /**
     * コンストラクタ
     * @param sender コンソール
     */
    public MailSenderConsole(ConsoleCommandSender sender) {
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
     * @return 常にtrue
     */
    @Override
    public boolean isValidDestination() {
        return true;
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
        return sender.getName();
    }

    /**
     * メッセージを送る
     * @param message メッセージ
     * @see org.bitbucket.ucchy.undine.sender.MailSender#sendMessage(java.lang.String)
     */
    @Override
    public void sendMessage(String message) {
        sender.sendMessage(message);
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
     * @return 常に "-" が返される。
     * @see org.bitbucket.ucchy.undine.sender.MailSender#getWorldName()
     */
    @Override
    public String getWorldName() {
        return "-";
    }

    /**
     * 発言者が今いる地点を取得する
     * @return 常にnullが返される。
     * @see org.bitbucket.ucchy.undine.sender.MailSender#getLocation()
     */
    @Override
    public Location getLocation() {
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
        return sender.hasPermission(node);
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
        if ( strMetadata == null ) {
            strMetadata = new HashMap<String, String>();
        }
        strMetadata.put(key, value);
    }

    /**
     * 文字列のメタデータを取得する
     * @param key キー
     * @return 値
     * @see org.bitbucket.ucchy.undine.sender.MailSender#getStringMetadata(java.lang.String)
     */
    @Override
    public String getStringMetadata(String key) {
        if ( strMetadata == null ) {
            return null;
        }
        return strMetadata.get(key);
    }

    /**
     * 真偽値のメタデータを設定する
     * @param key キー
     * @param value 値
     * @see org.bitbucket.ucchy.undine.sender.MailSender#setBooleanMetadata(java.lang.String, boolean)
     */
    @Override
    public void setBooleanMetadata(String key, boolean value) {
        if ( blnMetadata == null ) {
            blnMetadata = new HashMap<String, Boolean>();
        }
        blnMetadata.put(key, value);
    }

    /**
     * 真偽値のメタデータを取得する
     * @param key キー
     * @return 値
     * @see org.bitbucket.ucchy.undine.sender.MailSender#getBooleanMetadata(java.lang.String)
     */
    @Override
    public boolean getBooleanMetadata(String key) {
        if ( blnMetadata == null ) {
            return false;
        }
        return blnMetadata.get(key);
    }

    /**
     * 指定されたCommandSenderと同一かどうかを返す
     * @param sender
     * @return 同一かどうか
     * @see org.bitbucket.ucchy.undine.sender.MailSender#equals(org.bukkit.command.CommandSender)
     */
    @Override
    public boolean equals(CommandSender sender) {
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

    /**
     * MailSenderConsoleを返す
     * @return MailSenderConsole
     */
    public static MailSenderConsole getMailSenderConsole() {
        return new MailSenderConsole(Bukkit.getConsoleSender());
    }
}
