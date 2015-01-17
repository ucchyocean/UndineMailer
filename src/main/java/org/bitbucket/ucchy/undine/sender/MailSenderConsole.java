/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.undine.sender;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

/**
 * コンソール
 * @author ucchy
 */
public class MailSenderConsole extends MailSender {

    ConsoleCommandSender sender;

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
     * 指定されたCommandSenderと同一かどうかを返す
     * @param sender
     * @return 同一かどうか
     * @see org.bitbucket.ucchy.undine.sender.MailSender#equals(org.bukkit.entity.Player)
     */
    @Override
    public boolean equals(CommandSender sender) {
        return this.sender.equals(sender);
    }

    /**
     * IDを返す
     * @return 名前をそのまま返す
     * @see org.bitbucket.ucchy.undine.sender.MailSender#getID()
     */
    @Override
    public String toString() {
        return getName();
    }
}
