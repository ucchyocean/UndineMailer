/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.undine.sender;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

/**
 * Senderの抽象クラス
 * @author ucchy
 */
public abstract class MailSender implements Comparable<MailSender> {

    /**
     * オンラインかどうか
     * @return オンラインかどうか
     */
    public abstract boolean isOnline();

    /**
     * 宛先として有効かどうか
     * @return 接続したことがあるプレイヤーかどうか
     */
    public abstract boolean isValidDestination();

    /**
     * プレイヤー名を返す
     * @return プレイヤー名
     */
    public abstract String getName();

    /**
     * プレイヤー表示名を返す
     * @return プレイヤー表示名
     */
    public abstract String getDisplayName();

    /**
     * メッセージを送る
     * @param message メッセージ
     */
    public abstract void sendMessage(String message);

    /**
     * BukkitのPlayerを取得する
     * @return Player
     */
    public abstract Player getPlayer();

    /**
     * 発言者が今いるワールドのワールド名を取得する
     * @return ワールド名
     */
    public abstract String getWorldName();

    /**
     * 指定されたパーミッションノードの権限を持っているかどうかを取得する
     * @param node パーミッションノード
     * @return 権限を持っているかどうか
     */
    public abstract boolean hasPermission(String node);

    /**
     * 指定されたCommandSenderと同一かどうかを返す
     * @param sender
     * @return 同一かどうか
     */
    public abstract boolean equals(CommandSender sender);

    /**
     * 文字列表現を返す
     * @return 名前管理なら名前、UUID管理なら "$" + UUID を返す
     */
    @Override
    public abstract String toString();

    /**
     * 同一のオブジェクトかどうかを返す
     * @param other 他方のオブジェクト
     * @return 同一かどうか
     */
    @Override
    public boolean equals(Object other) {
        if ( !(other instanceof MailSender) ) {
            return false;
        }
        return this.toString().equals(((MailSender)other).toString());
    }

    /**
     * インスタンス同士の比較を行う。このメソッドを実装しておくことで、
     * Java8でのHashMapのキー挿入における高速化が期待できる（らしい）。
     * @param other
     * @return
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(MailSender other) {
        return this.toString().compareTo(other.toString());
    }

    /**
     * 名前またはUUIDから、MailSenderを作成して返す
     * @param nameOrUuid 名前、または、"$" + UUID
     * @return Sender
     */
    public static MailSender getMailSenderFromString(String nameOrUuid) {

        if ( nameOrUuid == null ) return null;

        // UUIDからの変換
        if ( nameOrUuid.startsWith("$") ) {
            UUID id = UUID.fromString(nameOrUuid.substring(1));
            return new MailSenderPlayer(Bukkit.getOfflinePlayer(id));
        }

        // nameからの変換
        String name = nameOrUuid;
        if ( name.equals("CONSOLE") ) {
            return new MailSenderConsole(Bukkit.getConsoleSender());
        }

        return MailSenderPlayer.fromName(name);
    }

    /**
     * CommandSenderから、MailSenderを作成して返す
     * @param sender
     * @return MailSender
     */
    public static MailSender getMailSender(CommandSender sender) {
        if ( sender == null ) {
            return null;
        } else if ( sender instanceof BlockCommandSender ) {
            return new MailSenderBlock((BlockCommandSender)sender);
        } else if ( sender instanceof ConsoleCommandSender ) {
            return new MailSenderConsole((ConsoleCommandSender)sender);
        } else if ( sender instanceof OfflinePlayer ) {
            return new MailSenderPlayer((OfflinePlayer)sender);
        }
        return null;
    }
}
