/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine.command;

import java.util.List;

import org.bukkit.command.CommandSender;

/**
 * サブコマンド
 * @author ucchy
 */
public interface SubCommand {

    /**
     * コマンドを取得します。
     * @return コマンド
     */
    public String getCommandName();

    /**
     * パーミッションノードを取得します。
     * @return パーミッションノード
     */
    public String getPermissionNode();

    /**
     * コマンドを実行します。
     * @param sender コマンド実行者
     * @param args 実行時の引数
     */
    public void runCommand(CommandSender sender, String[] args);

    /**
     * TABキー補完を実行します。
     * @param sender コマンド実行者
     * @param args 補完時の引数
     * @return 補完候補
     */
    public List<String> tabComplete(CommandSender sender, String[] args);
}
